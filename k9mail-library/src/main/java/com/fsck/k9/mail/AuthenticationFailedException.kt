package com.fsck.k9.mail

const val LOGIN_REQUIRED = "Login required"
private const val INVALID_GRANT = "invalid_grant"

class AuthenticationFailedException @JvmOverloads constructor(
    message: String,
    throwable: Throwable? = null,
    val messageFromServer: String? = null
) : MessagingException(message, throwable) {
    val isMessageFromServerAvailable = messageFromServer != null
    val isOAuthTokenRevoked = messageFromServer == INVALID_GRANT || message == LOGIN_REQUIRED
    // TODO: 11/8/22 these are only for Gmail, check other providers
}
