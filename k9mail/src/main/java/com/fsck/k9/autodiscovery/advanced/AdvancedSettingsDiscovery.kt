package com.fsck.k9.autodiscovery.advanced

import androidx.annotation.WorkerThread
import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.autodiscovery.thunderbird.ThunderbirdDiscovery
import com.fsck.k9.oauth.OAuthConfigurationProvider

class AdvancedSettingsDiscovery(
    private val providersXmlDiscovery: ProvidersXmlDiscovery,
    private val thunderbirdDiscovery: ThunderbirdDiscovery,
    oAuthConfigurationProvider: OAuthConfigurationProvider,
    private val advanced: Boolean = true,
) : ConnectionSettingsDiscovery(oAuthConfigurationProvider) {

    @WorkerThread
    override fun discover(email: String): DiscoveryResults? {
        return providersXmlDiscovery.discover(email) ?: let {
            if (advanced) thunderbirdDiscovery.discover(email)
            else null
        }
    }
}