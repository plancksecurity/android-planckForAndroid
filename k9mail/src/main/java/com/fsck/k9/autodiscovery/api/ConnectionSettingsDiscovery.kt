package com.fsck.k9.autodiscovery.api

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings

interface ConnectionSettingsDiscovery {
    fun discover(email: String, oAuthProviderType: OAuthProviderType?): DiscoveryResults?
}

data class DiscoveryResults(val incoming: List<DiscoveredServerSettings>, val outgoing: List<DiscoveredServerSettings>)

data class DiscoveredServerSettings(
    val protocol: ServerSettings.Type,
    val host: String,
    val port: Int,
    val security: ConnectionSecurity,
    val authType: AuthType?,
    val username: String?
)
