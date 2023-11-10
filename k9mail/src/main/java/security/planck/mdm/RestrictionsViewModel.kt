package security.planck.mdm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Preferences
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import security.planck.provisioning.AccountProvisioningSettings
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.findNextAccountToInstall
import javax.inject.Inject

/**
 * [ViewModel] dedicated for restrictions affairs.
 */
@HiltViewModel
class RestrictionsViewModel @Inject constructor(
    configurationManager: ConfigurationManager,
    private val preferences: Preferences,
    private val provisioningSettings: ProvisioningSettings,
) : ViewModel() {
    private val restrictionsUpdatedLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))

    /**
     * @property restrictionsUpdated
     *
     * [LiveData] that delivers restrictions updates.
     */
    val restrictionsUpdated: LiveData<Event<Boolean>> = restrictionsUpdatedLiveData

    private val accountRemovedLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val accountRemoved: LiveData<Event<Boolean>> = accountRemovedLiveData

    private val wrongAccountSettingsLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val wrongAccountSettings: LiveData<Event<Boolean>> = wrongAccountSettingsLiveData

    private val nextAccountToInstallLiveData =
        object : MutableLiveData<AccountProvisioningSettings?>(null) {
            override fun onActive() {
                super.onActive()
                value = findNextAccountToInstall()
            }
        }
    val nextAccountToInstall: LiveData<AccountProvisioningSettings?> = nextAccountToInstallLiveData

    init {
        configurationManager.restrictionsUpdatedFlow
            .onEach {
                restrictionsUpdatedLiveData.value = Event(it > 0)
                nextAccountToInstallLiveData.value = findNextAccountToInstall()
            }.launchIn(viewModelScope)

        configurationManager.accountRemovedFlow.onEach {
            accountRemovedLiveData.value = Event(it)
        }.launchIn(viewModelScope)

        configurationManager.wrongAccountSettingsFlow.onEach {
            wrongAccountSettingsLiveData.value = Event(it)
        }.launchIn(viewModelScope)
    }

    private fun findNextAccountToInstall() =
        provisioningSettings.findNextAccountToInstall(preferences)
}