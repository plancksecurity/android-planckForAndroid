package com.fsck.k9.oauth

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.mail.AuthType

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
