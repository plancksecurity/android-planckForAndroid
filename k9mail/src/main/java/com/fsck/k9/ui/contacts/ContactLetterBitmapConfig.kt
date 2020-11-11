package com.fsck.k9.ui.contacts

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import com.fsck.k9.K9
import com.fsck.k9.R
import javax.inject.Inject
import javax.inject.Named

class ContactLetterBitmapConfig  @Inject constructor(@Named("AppContext") context: Context) {
    val hasDefaultBackgroundColor: Boolean = !K9.isColorizeMissingContactPictures()
    private val appThemeResourceId =
            if (K9.getK9LegacyTheme() == K9.Theme.LIGHT)
                R.style.Theme_K9_Dialog_Light
            else
                R.style.Theme_K9_Dialog_Dark
    val useDarkTheme = K9.getK9LegacyTheme() != K9.Theme.LIGHT

    val defaultBackgroundColor: Int

    init {
        defaultBackgroundColor = if (hasDefaultBackgroundColor) {
            val outValue = TypedValue()
            val themedContext = ContextThemeWrapper(context, appThemeResourceId)
            themedContext.theme.resolveAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor, outValue, true)
            outValue.data
        } else {
            0
        }
    }
}
