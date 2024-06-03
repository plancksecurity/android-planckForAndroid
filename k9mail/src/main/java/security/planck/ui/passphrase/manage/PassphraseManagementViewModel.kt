package security.planck.ui.passphrase.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import security.planck.passphrase.PassphraseRepository
import security.planck.ui.passphrase.PassphraseDialogMode
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val passphraseRepository: PassphraseRepository,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<PassphraseMgmtState> =
        MutableLiveData(PassphraseMgmtState.Idle)
    val state: LiveData<PassphraseMgmtState> = stateLiveData
    lateinit var mode: PassphraseDialogMode


    fun start(
        //mode: PassphraseDialogMode,
    ) {
        //this.mode = mode
        //when (mode) {
        //    PassphraseDialogMode.MANAGE -> loadAccountsForManagement()
        //    //PassphraseDialogMode.UNLOCK -> loadAccountsForUnlocking()
        //}
    }

    private fun loadAccountsForManagement() {
        //viewModelScope.launch {
        //    stateLiveData.value = PassphraseMgmtState.Loading
        //    getAccountsUsingOrNotPassphrase().onFailure {
        //        stateLiveData.value = PassphraseMgmtState.CoreError(it)
        //    }.onSuccess {
        //        stateLiveData.value = PassphraseMgmtState.ManagingAccounts(it)
        //    }
        //}
    }

    private suspend fun getAccountsUsingOrNotPassphrase(): Result<List<AccountUsesPassphrase>> {
        val accountsUsePassphrase = preferences.availableAccounts.map { account ->
            planckProvider.hasPassphrase(account.email).fold(
                onFailure = {
                    return Result.failure(it)
                },
                onSuccess = {
                    AccountUsesPassphrase(account, it)
                }
            )
        }
        return Result.success(accountsUsePassphrase)
    }

}
