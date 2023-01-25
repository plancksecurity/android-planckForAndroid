package com.fsck.k9.autodiscovery.advanced

import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.autodiscovery.thunderbird.ThunderbirdDiscovery
import com.fsck.k9.oauth.OAuthConfigurationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AdvancedSettingsDiscovery(
    private val providersXmlDiscovery: ProvidersXmlDiscovery,
    private val thunderbirdDiscovery: ThunderbirdDiscovery,
    oAuthConfigurationProvider: OAuthConfigurationProvider,
    private val advanced: Boolean = true,
): ConnectionSettingsDiscovery(oAuthConfigurationProvider) {
    override fun discover(email: String): DiscoveryResults? {
        return providersXmlDiscovery.discover(email) ?:let {
            if (advanced) runBlocking(Dispatchers.IO) { thunderbirdDiscovery.discover(email) }
            else null
        }
    }
}