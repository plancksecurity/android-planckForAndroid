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
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PassphraseManagementViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
    private val passphraseRepository: PassphraseRepository,
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

    fun loadAccountsForUnlocking(accountsWithErrors: List<String> = emptyList()) {
        viewModelScope.launch {
            stateLiveData.value = PassphraseMgmtState.Loading
            getAccountsWithPassPhrase().onFailure {
                stateLiveData.value = PassphraseMgmtState.CoreError(it)
            }.onSuccess {
                stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases(it, accountsWithErrors)
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

    fun unlockKeysWithPassphrase(emails: List<String>, passphrases: List<String>) {
        Timber.e("EFA-601 UNLOCKING KEYS WITH PASSPHRASE: $emails : $passphrases")
        viewModelScope.launch {
            passphraseRepository.unlockKeysWithPassphrase(emails, passphrases).onFailure {
                Timber.e("EFA-601 RESULT ERROR: ${it.stackTraceToString()}")
                if (passphraseRepository.unlockErrors.failedAttempts < PassphraseRepository.RETRY_WITH_DELAY_AFTER) {
                    loadAccountsForUnlocking() // we should notify of an error...? // should we really retry here...?
                }
            }.onSuccess { list ->
                Timber.e("EFA-601 RESULT: $list")
                // if not too many errors, we can retry directly showing the error to the user. No delay in place yet.
                if (!list.isNullOrEmpty() && passphraseRepository.unlockErrors.failedAttempts < PassphraseRepository.RETRY_WITH_DELAY_AFTER) {
                    loadAccountsForUnlocking(accountsWithErrors = list)
                }
            }
        }
    }

}