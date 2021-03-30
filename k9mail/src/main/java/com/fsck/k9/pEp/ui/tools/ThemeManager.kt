package com.fsck.k9.pEp.ui.tools

import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import com.fsck.k9.K9
import com.fsck.k9.R

object ThemeManager {
    @JvmStatic
    var appTheme = AppTheme.FOLLOW_SYSTEM

    @JvmStatic
    fun isDarkTheme(): Boolean = legacyTheme == Theme.DARK

    @JvmStatic
    val legacyTheme: Theme
        get() = when (appTheme) {
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
    var k9MessageViewTheme: Theme = Theme.USE_GLOBAL

    @JvmStatic
    var k9ComposerTheme: Theme = Theme.USE_GLOBAL

    @JvmStatic
    fun getComposerTheme(): Theme = resolveTheme(k9ComposerTheme)

    @JvmStatic
    fun getMessageViewTheme() = resolveTheme(k9MessageViewTheme)

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
        if (!useFixedMessageViewTheme && k9MessageViewTheme == Theme.USE_GLOBAL) {
            k9MessageViewTheme = legacyTheme
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

    @JvmStatic
    fun getToolbarColor(context: Context, toolbarType: ToolbarType): Int {
        val attr = when(toolbarType) {
            ToolbarType.DEFAULT -> R.attr.toolbarDefaultColor
            ToolbarType.MESSAGEVIEW -> R.attr.messageViewToolbarColor
        }
        return getColorFromAttributeResource(context, attr)
    }

    @JvmStatic
    @ColorInt
    fun getStatusBarColor(context: Context, toolbarType: ToolbarType): Int {
        val attr = when(toolbarType) {
            ToolbarType.DEFAULT -> R.attr.toolbarDefaultColor
            ToolbarType.MESSAGEVIEW -> R.attr.messageViewToolbarColor
        }
        return getColorFromAttributeResource(context, attr)
    }

    @JvmStatic
    fun getAttributeResource(context: Context, @AttrRes resource: Int): Int {
        val a: TypedArray = context.theme.obtainStyledAttributes(ThemeManager.appThemeResourceId, intArrayOf(resource))
        return a.getResourceId(0, 0)
    }

    @JvmStatic
    @ColorInt
    fun getColorFromAttributeResource(context: Context, @AttrRes resource: Int): Int {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(resource, typedValue, true)
        return typedValue.data
    }

    @JvmStatic
    val filePickerThemeResourceId: Int
        get() = if(legacyTheme == Theme.LIGHT) R.style.FilePickerTheme_Light else R.style.FilePickerTheme_Dark

    fun setCurrentTheme(value: String?) {
        appTheme = stringToAppTheme(value)
        updateAppTheme()
    }

    fun themeToString(theme: Theme) = when (theme) {
        Theme.LIGHT -> "light"
        Theme.DARK -> "dark"
        Theme.USE_GLOBAL -> "global"
    }

    fun stringToTheme(theme: String?) = when (theme) {
        "light" -> Theme.LIGHT
        "dark" -> Theme.DARK
        "global" -> Theme.USE_GLOBAL
        else -> throw AssertionError()
    }

    private fun stringToAppTheme(theme: String?) = when (theme) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "follow_system" -> AppTheme.FOLLOW_SYSTEM
        else -> throw AssertionError()
    }

    fun appThemeToString(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> "dark"
        AppTheme.LIGHT -> "light"
        AppTheme.FOLLOW_SYSTEM -> "follow_system"
    }

    enum class ToolbarType {
        DEFAULT, MESSAGEVIEW
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