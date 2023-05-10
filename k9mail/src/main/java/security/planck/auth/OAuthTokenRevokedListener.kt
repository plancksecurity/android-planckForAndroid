package security.planck.auth

interface OAuthTokenRevokedListener {
    fun onTokenRevoked(accountUuid: String)
}
