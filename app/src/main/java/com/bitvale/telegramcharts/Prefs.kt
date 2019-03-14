package com.bitvale.telegramcharts

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 12-Mar-19
 */
class Prefs(context: Context) {
    val PREFS_FILENAME = "com.bitvale.telegramcharts.prefs"
    val THEME = "theme"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    var theme: Int
        get() = prefs.getInt(THEME, R.style.AppThemeLight)
        set(value) = prefs.edit().putInt(THEME, value).apply()
}