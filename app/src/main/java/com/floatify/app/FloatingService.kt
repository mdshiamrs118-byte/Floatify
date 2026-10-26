package com.floatify.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service that draws placeholder floating windows over other apps.
 * Real mini-apps (file manager, calculator, ...) will replace the placeholder body later.
 */
class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private val windows = mutableMapOf<String, View>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val channel = NotificationChannel(CHANNEL_ID, "Floating windows", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_OPEN -> intent.getStringExtra(EXTRA_APP_ID)?.let { openWindow(it) }
            ACTION_CLOSE_ALL -> closeAll()
        }
        if (windows.isEmpty()) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closeAll()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val closeIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingService::class.java).setAction(ACTION_CLOSE_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Floatify is running")
            .setContentText("Floating windows are active")
            .setOngoing(true)
            .addAction(0, "Close all", closeIntent)
            .build()
    }

    private fun openWindow(appId: String) {
        if (windows.containsKey(appId)) return
        val app = FloatApps.find(appId) ?: return
        if (!Settings.canDrawOverlays(this)) return

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 16 * d
                setStroke(dp(1), 0xFFCCCCCC.toInt())
            }
            clipToOutline = true
            elevation = 8 * d
            alpha = Prefs.getFloat(this@FloatingService, Prefs.KEY_OPACITY, 0.95f)
        }

        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF3F51B5.toInt())
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        titleBar.addView(TextView(this).apply {
            text = "${app.emoji}  ${app.name}"
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            textSize = 15f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleBar.addView(TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(12), 0, dp(8), 0)
            setOnClickListener { closeWindow(appId) }
        })
        root.addView(titleBar)

        root.addView(
            when (app.id) {
                "calc" -> CalculatorView.create(this)
                "files" -> FileManagerView.create(this)
                else -> TextView(this).apply {
                    text = "Placeholder window for ${app.name}.\n\nThe real ${app.name} will live here. Drag the title bar to move this window."
                    setTextColor(0xFF333333.toInt())
                    textSize = 14f
                    setPadding(dp(16), dp(16), dp(16), dp(24))
                }
            }
        )

        val widthDp = Prefs.getFloat(this, Prefs.KEY_WIDTH, 280f).toInt()
        val lp = WindowManager.LayoutParams(
            dp(widthDp),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24 + windows.size * 24)
            y = dp(120 + windows.size * 24)
        }

        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        titleBar.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - touchX).toInt()
                    lp.y = startY + (e.rawY - touchY).toInt()
                    wm.updateViewLayout(root, lp)
                    true
                }
                else -> false
            }
        }

        wm.addView(root, lp)
        windows[appId] = root
    }

    private fun closeWindow(appId: String) {
        windows.remove(appId)?.let { runCatching { wm.removeView(it) } }
        if (windows.isEmpty()) stopSelf()
    }

    private fun closeAll() {
        windows.values.forEach { runCatching { wm.removeView(it) } }
        windows.clear()
    }

    companion object {
        private const val CHANNEL_ID = "floatify_service"
        private const val NOTIF_ID = 1
        const val ACTION_OPEN = "com.floatify.app.OPEN"
        const val ACTION_CLOSE_ALL = "com.floatify.app.CLOSE_ALL"
        const val EXTRA_APP_ID = "app_id"

        fun open(c: Context, appId: String) {
            c.startForegroundService(
                Intent(c, FloatingService::class.java).setAction(ACTION_OPEN).putExtra(EXTRA_APP_ID, appId)
            )
        }

        fun closeAll(c: Context) {
            c.startForegroundService(Intent(c, FloatingService::class.java).setAction(ACTION_CLOSE_ALL))
        }
    }
}
