package com.example.reeltracker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stores per-day counts. Keys look like "reel_2026-10-11" and "short_2026-10-11".
 */
object Repository {

    private const val PREFS_NAME = "reel_tracker"
    private const val KEY_DEBUG_IDS = "debug_ids"

    enum class Type(val key: String) { REEL("reel"), SHORT("short") }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun key(type: Type) = "${type.key}_${today()}"

    fun getCount(context: Context, type: Type): Int =
        prefs(context).getInt(key(type), 0)

    fun increment(context: Context, type: Type) {
        val newValue = getCount(context, type) + 1
        prefs(context).edit().putInt(key(type), newValue).apply()
    }

    fun saveDebugIds(context: Context, text: String) {
        prefs(context).edit().putString(KEY_DEBUG_IDS, text).apply()
    }

    fun getDebugIds(context: Context): String =
        prefs(context).getString(KEY_DEBUG_IDS, "") ?: ""
}
