package com.fsck.k9.ui.helper

import com.fsck.k9.K9
import com.fsck.k9.message.html.HtmlSettings
import javax.inject.Inject

class HtmlSettingsProvider @Inject constructor() {
    fun createForMessageView() = HtmlSettings(
        useFixedWidthFont = K9.messageViewFixedWidthFont()
    )

    fun createForMessageCompose() = HtmlSettings(
        useFixedWidthFont = false
    )
}
