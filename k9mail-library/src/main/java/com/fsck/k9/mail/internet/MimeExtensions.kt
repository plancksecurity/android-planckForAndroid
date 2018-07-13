package com.fsck.k9.mail.internet


// RFC 2045: tspecials :=  "(" / ")" / "<" / ">" / "@" / "," / ";" / ":" / "\" / <"> / "/" / "[" / "]" / "?" / "="
private val TSPECIALS = charArrayOf('(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=')

// RFC 5234: HTAB = %x09
internal const val HTAB = '\t'

// RFC 5234: SP = %x20
internal const val SPACE = ' '

internal const val CR = '\r'
internal const val LF = '\n'
internal const val DQUOTE = '"'
internal const val SEMICOLON = ';'
internal const val EQUALS_SIGN = '='
internal const val ASTERISK = '*'
internal const val SINGLE_QUOTE = '\''


internal fun Char.isTSpecial() = this in TSPECIALS

// RFC 2045: token := 1*<any (US-ASCII) CHAR except SPACE, CTLs, or tspecials>
// RFC 5234: CTL = %x00-1F / %x7F
internal fun Char.isTokenChar() = isVChar() && !isTSpecial()

// RFC 5234: VCHAR = %x21-7E
internal fun Char.isVChar() = toInt() in 33..126

// RFC 5234: WSP =  SP / HTAB
internal fun Char.isWsp() = this == SPACE || this == HTAB

// RFC 2231: attribute-char := <any (US-ASCII) CHAR except SPACE, CTLs, "*", "'", "%", or tspecials>
internal fun Char.isAttributeChar() = isVChar() && this != '*' && this != '\'' && this != '%' && !isTSpecial()
