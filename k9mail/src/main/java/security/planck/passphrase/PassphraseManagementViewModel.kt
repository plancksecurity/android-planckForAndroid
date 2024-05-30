package security.planck.passphrase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import timber.log.Timber
import javax.inject.Inject

private const val ACCEPTED_SYMBOLS = """@\$!%*+\-_#?&\[\]\{\}\(\)\.:;,<>~"'\\/"""
private const val PASSPHRASE_REGEX =
    """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[$ACCEPTED_SYMBOLS])[A-Za-z\d$ACCEPTED_SYMBOLS]{12,}$"""

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
    lateinit var mode: PassphraseDialogMode

    val passwordStates = mutableStateListOf<TextFieldState>()

    fun start(
        mode: PassphraseDialogMode,
        accountsWithErrors: List<String>?,
    ) {
        this.mode = mode
        when (mode) {
            PassphraseDialogMode.MANAGE -> loadAccountsForManagement()
            PassphraseDialogMode.UNLOCK -> loadAccountsForUnlocking(accountsWithErrors = accountsWithErrors.orEmpty())
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
            }.onSuccess { accountsWithPassphrase ->
                initializePasswordStatesIfNeeded(accountsWithPassphrase, accountsWithErrors)
                stateLiveData.value =
                    PassphraseMgmtState.UnlockingPassphrases(accountsWithPassphrase.map { it.email })
            }
        }
    }

    private fun initializePasswordStatesIfNeeded(
        accountsUsingPassphrase: List<Account>,
        accountsWithErrors: List<String>
    ) {
        if (passwordStates.isEmpty()) {
            passwordStates.addAll(accountsUsingPassphrase.map {
                TextFieldState(email = it.email, isError = accountsWithErrors.contains(it.email))
            })
        }
    }

    private fun updateWithUnlockErrors(accountsWithErrors: List<String>) {
        for (index in passwordStates.indices) {
            val state = passwordStates[index]
            if (accountsWithErrors.contains(state.email)) {
                passwordStates[index].errorState = true
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
                if (passphraseRepository.shouldRetryImmediately) {
                    loadAccountsForUnlocking() // we should notify of an error...? // should we really retry here...?
                    //stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases(passwordStates.map { state -> state.email })
                } else {
                    stateLiveData.value = PassphraseMgmtState.Finish
                }
            }.onSuccess { list ->
                Timber.e("EFA-601 RESULT: $list")
                // if not too many errors, we can retry directly showing the error to the user. No delay in place yet.
                if (!list.isNullOrEmpty() && passphraseRepository.shouldRetryImmediately) {
                    updateWithUnlockErrors(list)
                    loadAccountsForUnlocking(accountsWithErrors = list)
                    //stateLiveData.value = PassphraseMgmtState.UnlockingPassphrases(passwordStates.map { it.email })
                } else {
                    stateLiveData.value = PassphraseMgmtState.Finish
                }
            }
        }
    }

    fun validateInput(state: TextFieldState) {
        state.errorState = !state.textState.isValidPassphrase()
    }

    private fun String.isValidPassphrase(): Boolean {
        return length > 3
        //return matches(PASSPHRASE_REGEX.toRegex())
    }

}

data class TextFieldState(
    val email: String,
    private val text: String = "",
    private val isError: Boolean = false,
) {
    var textState by mutableStateOf(text)
    var errorState by mutableStateOf(isError)
}