package security.planck.passphrase.extensions

private const val ACCEPTED_SYMBOLS = """@\$!%*+\-_#?&\[\]\{\}\(\)\.:;,<>~"'\\/"""
private const val PASSPHRASE_REGEX =
    """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[$ACCEPTED_SYMBOLS])[A-Za-z\d$ACCEPTED_SYMBOLS]{12,}$"""

fun String.isValidPassphrase(): Boolean {
    return length >= 3
    //return matches(PASSPHRASE_REGEX.toRegex())
}