package security.pEp.auth

interface OAuthTokenRevokedListener {
    fun onTokenRevoked(accountUuid: String)
}
