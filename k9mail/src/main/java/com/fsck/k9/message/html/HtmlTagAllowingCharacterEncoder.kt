package com.fsck.k9.message.html

open class HtmlTagAllowingCharacterEncoder(html: StringBuilder) : HtmlCharacterEncoder(html) {
    override fun appendHtmlEncoded(ch: Char) {
        when (ch) {
            '&' -> html.append("&amp;")
            '\r' -> Unit
            '\n' -> html.append(HTML_NEWLINE)
            else -> html.append(ch)
        }
    }
}
