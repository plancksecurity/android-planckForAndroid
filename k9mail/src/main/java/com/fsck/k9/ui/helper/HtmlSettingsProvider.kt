package com.fsck.k9.ui.helper

import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings
import com.fsck.k9.pEp.ui.tools.Theme
import com.fsck.k9.pEp.ui.tools.ThemeManager

class HtmlSettingsProvider {
    fun createForMessageView() = HtmlSettings(
        useDarkMode = ThemeManager.getMessageViewTheme() == Theme.DARK,
        useFixedWidthFont = K9.messageViewFixedWidthFont()
    )

    fun createForMessageCompose() = HtmlSettings(
        useDarkMode = ThemeManager.getComposerTheme() == Theme.DARK,
        useFixedWidthFont = false
    )
}
