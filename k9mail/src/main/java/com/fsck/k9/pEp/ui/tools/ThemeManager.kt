package com.fsck.k9.pEp.ui.tools

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.fsck.k9.K9

object ThemeManager {
    @JvmStatic
    fun updateAppTheme() {
        val defaultNightMode = when (K9.getK9AppTheme()) {
            K9.AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            K9.AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            K9.AppTheme.FOLLOW_SYSTEM -> {
                if (Build.VERSION.SDK_INT < 28) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode)
    }
}