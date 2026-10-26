package com.floatify.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Floating file manager body, built with plain Views (the service has no Compose host).
 *
 * Features: browse folders, sort, open a file with another app, share, copy path,
 * delete, and a graceful hand-off to the "All files access" settings screen when
 * Android's scoped storage hides a directory.
 */
object FileManagerView {

    private const val AUTHORITY = "com.floatify.app.fileprovider"

    private enum class Sort { NAME, SIZE, DATE }

    fun create(context: Context): View {
        val d = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val startDir = resolveStartDir(context)
        var current: File = startDir
        var sort = Sort.NAME
        var rows: List<File> = emptyList()

        val body = FrameLayout(context)

        // ---------- main column ----------
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val pathText = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFF444444.toInt())
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }

        fun toolButton(label: String, onClick: () -> Unit) = TextView(context).apply {
            text = label
            textSize = 15f
            setTextColor(0xFF3F51B5.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { onClick() }
        }

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        toolbar.addView(toolButton("🏠") { navigate(startDir) })
        toolbar.addView(toolButton("↑") { navigate(current.parentFile ?: startDir) })
        toolbar.addView(pathText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toolbar.addView(toolButton("↕") {
            sort = when (sort) {
                Sort.NAME -> Sort.SIZE
                Sort.SIZE -> Sort.DATE
                Sort.DATE -> Sort.NAME
            }
            refresh()
        })
        toolbar.addView(toolButton("⟳") { refresh() })
        content.addView(toolbar)

        val list = ListView(context).apply {
            divider = null
            setBackgroundColor(Color.WHITE)
        }
        content.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(230)
        ))

        val footer = TextView(context).apply {
            textSize = 10f
            setTextColor(0xFF888888.toInt())
            setPadding(dp(10), dp(5), dp(10), dp(6))
            maxLines = 2
        }
        content.addView(footer)
        body.addView(content)

        // ---------- overlay panel (used instead of dialogs, which a service cannot show) ----------
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 14 * d
                setStroke(dp(1), 0xFFCCCCCC.toInt())
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
        }
        val panelTitle = TextView(context).apply {
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF111111.toInt())
        }
        val panelMessage = TextView(context).apply {
            textSize = 12f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(6), 0, dp(8))
        }
        val panelButtons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        panel.addView(panelTitle)
        panel.addView(panelMessage)
        panel.addView(panelButtons)
        body.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        fun panelButton(label: String, primary: Boolean, onClick: () -> Unit) = TextView(context).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(if (primary) Color.WHITE else 0xFF3F51B5.toInt())
            background = GradientDrawable().apply {
                cornerRadius = 10 * d
                setColor(if (primary) 0xFF3F51B5.toInt() else 0xFFEEEEEE.toInt())
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(6), 0, 0, 0) }
            setOnClickListener { onClick() }
            panelButtons.addView(this, lp)
        }

        fun showPanel(title: String, message: String) {
            panelTitle.text = title
            panelMessage.text = message
            panelButtons.removeAllViews()
            panel.visibility = View.VISIBLE
        }

        fun hidePanel() {
            panel.visibility = View.GONE
        }

        // ---------- data ----------
        fun readableChildren(dir: File): List<File>? = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            null
        }

        fun refresh() {
            val listing = readableChildren(current)
            if (listing == null) {
                rows = emptyList()
                list.adapter = null
                pathText.text = current.absolutePath
                footer.text = "Android blocked access to this folder."
                showPanel(
                    "Permission needed",
                    "Floatify can't read ${current.name} on this device. Grant \"All files access\" to browse your whole storage."
                )
                panelButton("Open settings", true) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .setData(Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                panelButton("Close", false) { hidePanel() }
                return
            }

            val dirs = listing.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
            val files = listing.filter { !it.isDirectory }.sortedBy { it.name.lowercase() }
            rows = when (sort) {
                Sort.NAME -> dirs + files
                Sort.SIZE -> (dirs + files).sortedByDescending { it.length() }
                Sort.DATE -> (dirs + files).sortedByDescending { it.lastModified() }
            }

            pathText.text = current.absolutePath
            val total = files.sumOf { it.length() }
            footer.text = "${rows.size} items • ${formatSize(total)} • sorted by ${sort.name.lowercase()}"
            hidePanel()

            list.adapter = object : BaseAdapter() {
                override fun getCount(): Int = rows.size
                override fun getItem(position: Int): Any = rows[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val item = rows[position]
                    val row = convertView as? LinearLayout ?: LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        addView(TextView(context).apply {
                            tag = "icon"
                            textSize = 16f
                        })
                        addView(TextView(context).apply {
                            tag = "name"
                            textSize = 14f
                            setTextColor(0xFF222222.toInt())
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                        addView(TextView(context).apply {
                            tag = "meta"
                            textSize = 10f
                            setTextColor(0xFF999999.toInt())
                            gravity = Gravity.END
                            maxLines = 2
                        })
                    }

                    val name = row.findViewWithTag<TextView>("name")
                    val meta = row.findViewWithTag<TextView>("meta")
                    val icon = row.findViewWithTag<TextView>("icon")
                    name.text = item.name
                    icon.text = if (item.isDirectory) "📁" else "📄"
                    meta.text = if (item.isDirectory) {
                        countChildren(item)
                    } else {
                        "${formatSize(item.length())}\n${formatDate(item.lastModified())}"
                    }
                    return row
                }
            }
        }

        fun countChildren(dir: File): String {
            val n = try { dir.list()?.size } catch (e: SecurityException) { null } ?: return "—"
            return "$n items"
        }

        fun navigate(dir: File) {
            if (!dir.exists() || !dir.isDirectory) {
                showPanel("Can't open", "${dir.absolutePath} is not available.")
                panelButton("Close", true) { hidePanel() }
                return
            }
            current = dir
            refresh()
        }

        // ---------- actions ----------
        fun openFile(file: File) {
            val uri = runCatching { FileProvider.getUriForFile(context, AUTHORITY, file) }.getOrNull()
            if (uri == null) {
                showPanel("Can't open", "Floatify can't share ${file.name} — it sits outside the allowed folders.")
                panelButton("Copy path", true) { copyPath(file) }
                panelButton("Close", false) { hidePanel() }
                return
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeOf(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                showPanel("No app for this file", "Nothing on this device can open ${file.name}.")
                panelButton("Copy path", true) { copyPath(file) }
                panelButton("Close", false) { hidePanel() }
            }
        }

        fun copyPath(file: File) {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cb?.setPrimaryClip(ClipData.newPlainText("path", file.absolutePath))
            showPanel("Copied", file.absolutePath)
            panelButton("Close", true) { hidePanel() }
        }

        fun shareFile(file: File) {
            val uri = runCatching { FileProvider.getUriForFile(context, AUTHORITY, file) }.getOrNull()
            val intent = if (uri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeOf(file)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, file.absolutePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            runCatching {
                context.startActivity(
                    Intent.createChooser(intent, "Share ${file.name}")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                showPanel("Can't share", "No app accepted the share request.")
                panelButton("Close", true) { hidePanel() }
            }
        }

        fun confirmDelete(file: File) {
            showPanel(
                "Delete ${file.name}?",
                if (file.isDirectory) {
                    "The folder and everything inside it will be permanently removed."
                } else {
                    "This file will be permanently removed."
                }
            )
            panelButton("Cancel", false) { hidePanel() }
            panelButton("Delete", true) {
                val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
                showPanel(
                    if (ok) "Deleted" else "Couldn't delete",
                    if (ok) "${file.name} is gone." else "Android refused to delete ${file.name}."
                )
                panelButton("Close", true) { hidePanel(); refresh() }
            }
        }

        fun filePanel(file: File) {
            showPanel(
                file.name,
                "${if (file.isDirectory) "Folder" else "File"}\n" +
                    "${file.absolutePath}\n\n" +
                    "Size: ${formatSize(file.length())}\n" +
                    "Modified: ${formatDate(file.lastModified())}"
            )
            panelButton("Copy path", false) { copyPath(file) }
            panelButton("Share", false) { shareFile(file) }
            panelButton("Delete", true) { confirmDelete(file) }
        }

        list.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val file = rows.getOrNull(position) ?: return@setOnItemClickListener
            if (file.isDirectory) navigate(file) else openFile(file)
        }
        list.setOnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val file = rows.getOrNull(position)
            if (file != null) filePanel(file)
            true
        }

        refresh()
        return body
    }

    // ---------- helpers ----------
    private fun resolveStartDir(context: Context): File {
        val external = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (external != null && external.isDirectory) return external
        return context.filesDir
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var i = 0
        while (value >= 1024 && i < units.size - 1) {
            value /= 1024
            i++
        }
        return if (i == 0) "${value.toInt()} ${units[i]}" else "%.1f %s".format(Locale.US, value, units[i])
    }

    private fun formatDate(millis: Long): String =
        if (millis <= 0L) "unknown" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

    private fun mimeOf(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".webp") || name.endsWith(".gif") -> "image/*"
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") -> "video/*"
            name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") -> "audio/*"
            name.endsWith(".pdf") -> "application/pdf"
            name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log") -> "text/plain"
            name.endsWith(".apk") -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }
}
