package com.fsck.k9.pEp

import android.content.res.Resources
import java.util.Locale

object LangUtils {
    @JvmStatic
    fun getDefaultLocale(): Locale {
        return Resources.getSystem().configuration.locales[0]
    }
}