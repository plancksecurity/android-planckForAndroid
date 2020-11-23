package com.fsck.k9.pEp.ui.tools

import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.fsck.k9.K9
import com.fsck.k9.R

object ThemeManager {
    @JvmStatic
    var appTheme = AppTheme.FOLLOW_SYSTEM

    @JvmStatic
    val legacyTheme: Theme
    get() = when(appTheme) {
        AppTheme.DARK -> Theme.DARK
        AppTheme.LIGHT -> Theme.LIGHT
        AppTheme.FOLLOW_SYSTEM -> if (Build.VERSION.SDK_INT < 28) Theme.LIGHT else systemTheme
    }

    private val systemTheme: Theme
    get() = when (K9.app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_NO -> Theme.LIGHT
        Configuration.UI_MODE_NIGHT_YES -> Theme.DARK
        else -> Theme.LIGHT
    }

    @JvmStatic
    val appThemeResourceId: Int
    get() = R.style.Theme_K9_DayNight

    @JvmStatic
    val composerThemeResourceId: Int
    get() = appThemeResourceId

    @JvmStatic
    var composerTheme: Theme = resolveTheme(Theme.USE_GLOBAL)
    @JvmStatic
    var messageViewTheme: Theme = resolveTheme(Theme.USE_GLOBAL)

    private fun resolveTheme(theme: Theme): Theme {
        return when (theme) {
            Theme.LIGHT, Theme.DARK -> theme
            else -> legacyTheme
        }
    }

    @JvmStatic
    var useFixedMessageViewTheme: Boolean = true
    private set

    @JvmStatic
    fun setUseFixedMessageViewTheme(boolean: Boolean) {
        useFixedMessageViewTheme = boolean
        if(!useFixedMessageViewTheme && messageViewTheme == Theme.USE_GLOBAL) {
            messageViewTheme = legacyTheme
        }
    }

    @JvmStatic
    fun updateAppTheme() {
        val defaultNightMode = when (appTheme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.FOLLOW_SYSTEM -> {
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

/**
 * Possible values for the different theme settings.
 *
 *
 * **Important:**
 * Do not change the order of the items! The ordinal value (position) is used when saving the
 * settings.
 */
enum class Theme {
    LIGHT, DARK, USE_GLOBAL
}

enum class AppTheme {
    LIGHT, DARK, FOLLOW_SYSTEM
}