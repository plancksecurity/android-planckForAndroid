package com.fsck.k9.pEp

import androidx.annotation.ColorInt

object PEpColorUtils {
    @JvmStatic
    @ColorInt
    fun makeColorTransparent(@ColorInt color: Int): Int {
        return color and 0x00FFFFFF
    }

    @JvmStatic
    @ColorInt
    fun makeColorOpaque(@ColorInt color: Int): Int {
        return color or -0x1000000
    }
}