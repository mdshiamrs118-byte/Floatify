package com.floatify.app

/** A mini-app that can be opened inside a floating window. */
data class FloatApp(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
)

object FloatApps {
    val all = listOf(
        FloatApp("files", "File Manager", "Browse files in a floating window", "📁"),
        FloatApp("calc", "Calculator", "Quick calculations over any app", "🧮"),
        FloatApp("browser", "Browser", "Mini web browser on top", "🌐"),
        FloatApp("games", "Games", "Casual games while you multitask", "🎮"),
        FloatApp("notes", "Notes", "Jot things down instantly", "📝"),
        FloatApp("music", "Music", "Floating music controls", "🎵"),
        FloatApp("timer", "Timer", "Stopwatch and countdown", "⏱️"),
        FloatApp("gallery", "Gallery", "Peek at your photos", "🖼️"),
    )

    fun find(id: String) = all.firstOrNull { it.id == id }
}
