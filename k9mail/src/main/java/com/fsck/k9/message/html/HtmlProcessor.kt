package com.fsck.k9.message.html

import org.jsoup.nodes.Document

class HtmlProcessor(private val htmlSanitizer: HtmlSanitizer, private val displayHtml: DisplayHtml) {
    fun processForDisplay(html: String?): String {
        val document = htmlSanitizer.sanitize(html)
        addCustomHeadContents(document)
        return toCompactString(document)
    }

    private fun addCustomHeadContents(document: Document) {
        document.head().append("<meta name=\"viewport\" content=\"width=device-width\"/>" +
                displayHtml.cssStyleTheme() +
                displayHtml.cssStylePre() +
                displayHtml.cssStyleSignature())
    }

    companion object {
        @JvmStatic
        fun toCompactString(document: Document): String {
            document.outputSettings()
                    .prettyPrint(false)
                    .indentAmount(0)
            return document.html()
        }
    }
}