package security.planck.passphrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<PassphraseMgmtState> =
        MutableLiveData(PassphraseMgmtState.Idle)
    val state: LiveData<PassphraseMgmtState> = stateLiveData

    fun start(mode: PassphraseDialogMode) {
        when (mode) {
            PassphraseDialogMode.MANAGE -> loadAccountsForManagement()
            PassphraseDialogMode.UNLOCK -> loadAccountsForUnlocking()
        }
    }

    fun loadAccountsForManagement() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseMgmtState.Loading
            getAccountsUsingOrNotPassphrase().onFailure {
                stateLiveData.value = PassphraseMgmtState.CoreError(it)
            }.onSuccess {
                stateLiveData.value = PassphraseMgmtState.ManagingAccounts(it)
            }
        }
    }

    fun loadAccountsForUnlocking() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseMgmtState.Loading
            getAccountsWithPassPhrase().onFailure {
                stateLiveData.value = PassphraseMgmtState.CoreError(it)
            }.onSuccess {
                stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases(it)
            }
        }
    }

    private suspend fun getAccountsWithPassPhrase(): Result<List<Account>> {
        val accounts = preferences.availableAccounts.filter { account ->
            planckProvider.hasPassphrase(account.email).fold(
                onFailure = {
                    return Result.failure(it)
                },
                onSuccess = {
                    true
                }
            )
        }
        return Result.success(accounts)
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