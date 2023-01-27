package com.fsck.k9.activity.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.K9
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.advanced.AdvancedSettingsDiscovery
import com.fsck.k9.oauth.OAuthConfigurationProvider
import com.fsck.k9.oauth.discover
import com.fsck.k9.pEp.infrastructure.livedata.Event
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.fragments.toServerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.pEp.provisioning.ProvisioningSettings
import security.pEp.provisioning.SimpleMailSettings

class AccountSetupBasicsViewModel(
    private val mailSettingsDiscovery: AdvancedSettingsDiscovery,
    private val oAuthConfigurationProvider: OAuthConfigurationProvider,
    private val provisioningSettings: ProvisioningSettings = (K9.app as K9).component.provisioningSettings()
) : ViewModel() {

    private val _connectionSettings =
        MutableLiveData<Pair<Event<ConnectionSettings?>, Boolean>>(Pair(Event(null), false))
    val connectionSettings: LiveData<Pair<Event<ConnectionSettings?>, Boolean>> =
        _connectionSettings

    fun discoverMailSettingsAsync(email: String, oAuthProviderType: OAuthProviderType? = null) {
        viewModelScope.launch {
            discoverMailSettings(email, oAuthProviderType)
                .also { _connectionSettings.value = Pair(Event(it), true) }
        }
    }

    private suspend fun discoverMailSettings(
        email: String,
        oAuthProviderType: OAuthProviderType? = null
    ): ConnectionSettings? = withContext(Dispatchers.IO) {
        provisioningSettings.provisionedMailSettings?.let { mailSettings ->
            mailSettingsDiscovery.setProvisionedSettings(
                incomingUriTemplate = getServerUriTemplate(mailSettings.incoming, CheckDirection.INCOMING),
                incomingUsername = mailSettings.incoming.userName!!,
                outgoingUriTemplate = getServerUriTemplate(mailSettings.outgoing, CheckDirection.OUTGOING),
                outgoingUsername = mailSettings.outgoing.userName!!
            )
        }
        val discoveryResults = mailSettingsDiscovery.discover(
            email,
            oAuthConfigurationProvider,
            oAuthProviderType
        )
        if (discoveryResults == null || discoveryResults.incoming.isEmpty() || discoveryResults.outgoing.isEmpty()) {
            return@withContext null
        }

        val incomingServerSettings =
            discoveryResults.incoming.first().toServerSettings() ?: return@withContext null
        val outgoingServerSettings =
            discoveryResults.outgoing.first().toServerSettings() ?: return@withContext null

        return@withContext ConnectionSettings(incomingServerSettings, outgoingServerSettings)
    }

    private fun getServerUriTemplate(
        settings: SimpleMailSettings,
        direction: CheckDirection
    ): String {
        val protocol = when (direction) {
            CheckDirection.INCOMING -> "imap"
            CheckDirection.OUTGOING -> "smtp"
        }
        return (protocol +
                "+" +
                settings.getConnectionSecurityString() +
                "+" +
                "://" +
                settings.server +
                ":" +
                settings.port)
    }
}