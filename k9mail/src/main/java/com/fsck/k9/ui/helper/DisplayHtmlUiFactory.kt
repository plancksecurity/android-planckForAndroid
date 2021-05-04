package com.fsck.k9.ui.helper

import com.fsck.k9.message.html.DisplayHtml
import javax.inject.Inject

class DisplayHtmlUiFactory @Inject constructor(private val htmlSettingsProvider: HtmlSettingsProvider) {
    fun createForMessageView(): DisplayHtml {
        return DisplayHtml(htmlSettingsProvider.createForMessageView())
    }

    fun createForMessageCompose(): DisplayHtml {
        return DisplayHtml(htmlSettingsProvider.createForMessageCompose())
    }
}
