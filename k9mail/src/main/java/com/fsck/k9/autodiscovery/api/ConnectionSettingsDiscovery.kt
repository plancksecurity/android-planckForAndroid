package com.fsck.k9.autodiscovery.api

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.oauth.OAuthConfigurationProvider

abstract class ConnectionSettingsDiscovery(
    private val oAuthConfigurationProvider: OAuthConfigurationProvider
) {
    open fun discover(email: String, oAuthProviderType: OAuthProviderType?): DiscoveryResults? {
        return discover(email)?.addOAuthIfPossible(oAuthProviderType)
    }

    abstract fun discover(email: String): DiscoveryResults?

    private fun DiscoveryResults.addOAuthIfPossible(oAuthProviderType: OAuthProviderType?): DiscoveryResults {
        return DiscoveryResults(
            this.incoming.map { it.addOAuthIfPossible(oAuthProviderType) },
            this.outgoing.map { it.addOAuthIfPossible(oAuthProviderType) }
        )

    }
    private fun DiscoveredServerSettings.addOAuthIfPossible(oAuthProviderType: OAuthProviderType?): DiscoveredServerSettings {
        val authType = if (oAuthProviderType != null || oAuthConfigurationProvider.getConfiguration(host) != null) {
            AuthType.XOAUTH2
        } else {
            AuthType.PLAIN
        }
        return this.copy(authType = authType)
    }
}

data class DiscoveryResults(val incoming: List<DiscoveredServerSettings>, val outgoing: List<DiscoveredServerSettings>)

data class DiscoveredServerSettings(
    val protocol: String,
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
    val authType: AuthType?,
    val username: String?
)
