package com.fsck.k9.pEp

import android.content.res.Resources
import android.os.Build
import java.util.*

object LangUtils {
    @JvmStatic
    fun getDefaultLocale(): Locale {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                Resources.getSystem().configuration.locales[0]
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.M -> {
                Resources.getSystem().configuration.locale
            }
            else -> {
                Locale.getDefault()
            }
        }
    }
}