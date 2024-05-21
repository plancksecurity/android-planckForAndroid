package com.fsck.k9.message.html

class HtmlCharacterEncoder(private val html: StringBuilder) {
    fun appendHtmlEncoded(ch: Char) {
        when (ch) {
            '&' -> html.append("&amp;")
            '<' -> html.append("&lt;")
            '>' -> html.append("&gt;")
            '\r' -> Unit
            '\n' -> html.append(HTML_NEWLINE)
            else -> html.append(ch)
        }
    }

    companion object {
        internal const val HTML_NEWLINE = "<br>"
    }
}
