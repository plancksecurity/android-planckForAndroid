package com.fsck.k9.autodiscovery.api

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.oauth.OAuthConfigurationProvider

interface ConnectionSettingsDiscovery {
    fun discover(email: String): DiscoveryResults?
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

fun ConnectionSettingsDiscovery.discover(
    email: String,
    oAuthConfigurationProvider: OAuthConfigurationProvider,
    oAuthProviderType: OAuthProviderType? = null
): DiscoveryResults? {
    return discover(email)?.addOAuthIfPossible(oAuthConfigurationProvider, oAuthProviderType)
}

private fun DiscoveryResults.addOAuthIfPossible(
    oAuthConfigurationProvider: OAuthConfigurationProvider,
    oAuthProviderType: OAuthProviderType?,
): DiscoveryResults {
    return DiscoveryResults(
        this.incoming.map { it.addOAuthIfPossible(oAuthConfigurationProvider, oAuthProviderType) },
        this.outgoing.map { it.addOAuthIfPossible(oAuthConfigurationProvider, oAuthProviderType) }
    )
}

private fun DiscoveredServerSettings.addOAuthIfPossible(
    oAuthConfigurationProvider: OAuthConfigurationProvider,
    oAuthProviderType: OAuthProviderType?,
): DiscoveredServerSettings {
    val authType = if (oAuthProviderType != null || oAuthConfigurationProvider.isGoogle(host)) {
        AuthType.XOAUTH2
    } else {
        AuthType.PLAIN
    }
    return this.copy(authType = authType)
}
