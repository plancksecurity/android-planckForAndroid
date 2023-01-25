package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.oauth.OAuthConfigurationProvider

class ThunderbirdDiscovery(
    private val urlProvider: ThunderbirdAutoconfigUrlProvider,
    private val fetcher: ThunderbirdAutoconfigFetcher,
    private val parser: ThunderbirdAutoconfigParser,
    oAuthConfigurationProvider: OAuthConfigurationProvider
) : ConnectionSettingsDiscovery(oAuthConfigurationProvider) {

    override fun discover(email: String): DiscoveryResults? {
        val autoconfigUrls = urlProvider.getAutoconfigUrls(email)

        return autoconfigUrls
            .asSequence()
            .mapNotNull { autoconfigUrl ->
                fetcher.fetchAutoconfigFile(autoconfigUrl)?.use { inputStream ->
                    parser.parseSettings(inputStream, email)
                }
            }.map { discoveryResults ->
                DiscoveryResults(
                    discoveryResults.incoming.filter { it.protocol == ServerSettings.Type.IMAP.toString() },
                    discoveryResults.outgoing.filter { it.protocol == ServerSettings.Type.SMTP.toString() },
                )
            }
            .firstOrNull { result ->
                result.incoming.isNotEmpty() || result.outgoing.isNotEmpty()
            }
    }

    override fun toString(): String = "Thunderbird autoconfig"
}
