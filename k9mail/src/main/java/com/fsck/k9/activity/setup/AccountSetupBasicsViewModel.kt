package com.fsck.k9.activity.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.advanced.AdvancedSettingsDiscovery
import com.fsck.k9.pEp.infrastructure.livedata.Event
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.fragments.toServerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountSetupBasicsViewModel(
    private val mailSettingsDiscovery: AdvancedSettingsDiscovery
): ViewModel() {

    private val _connectionSettings = MutableLiveData<Pair<Event<ConnectionSettings?>, Boolean>>(Pair(Event(null), false))
    val connectionSettings: LiveData<Pair<Event<ConnectionSettings?>, Boolean>> = _connectionSettings

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
        val discoveryResults = mailSettingsDiscovery.discover(email, oAuthProviderType)
        if (discoveryResults == null || discoveryResults.incoming.isEmpty() || discoveryResults.outgoing.isEmpty()) {
            return@withContext null
        }

        val incomingServerSettings = discoveryResults.incoming.first().toServerSettings() ?: return@withContext null
        val outgoingServerSettings = discoveryResults.outgoing.first().toServerSettings() ?: return@withContext null

        return@withContext ConnectionSettings(incomingServerSettings, outgoingServerSettings)
    }
}