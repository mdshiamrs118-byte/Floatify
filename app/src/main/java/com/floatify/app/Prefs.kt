package com.floatify.app

import android.content.Context

/** Tiny SharedPreferences wrapper for the placeholder settings. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("floatify_prefs", Context.MODE_PRIVATE)

    const val KEY_OPACITY = "window_opacity"
    const val KEY_WIDTH = "window_width_dp"

    fun getBool(c: Context, key: String, def: Boolean) = sp(c).getBoolean(key, def)
    fun setBool(c: Context, key: String, v: Boolean) = sp(c).edit().putBoolean(key, v).apply()
    fun getFloat(c: Context, key: String, def: Float) = sp(c).getFloat(key, def)
    fun setFloat(c: Context, key: String, v: Float) = sp(c).edit().putFloat(key, v).apply()
    fun clear(c: Context) = sp(c).edit().clear().apply()
}
