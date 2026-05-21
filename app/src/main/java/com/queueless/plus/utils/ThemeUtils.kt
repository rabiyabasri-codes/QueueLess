package com.queueless.plus.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    fun applyTheme(activity: AppCompatActivity, session: SessionManager) {
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
