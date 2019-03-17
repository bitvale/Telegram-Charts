package com.bitvale.telegramcharts;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
class Prefs {

    private static final String PREFS_FILENAME = "com.bitvale.telegramcharts.prefs";
    private static final String THEME = "theme";

    private SharedPreferences prefs;

    Prefs(Context context) {
        prefs  = context.getSharedPreferences(PREFS_FILENAME, 0);
    }

    int getTheme() {
        return prefs.getInt(THEME, R.style.AppThemeLight);
    }

    void setTheme(int theme) {
        prefs.edit().putInt(THEME, theme).apply();
    }
}
