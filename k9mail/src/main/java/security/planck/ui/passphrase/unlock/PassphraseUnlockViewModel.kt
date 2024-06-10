package security.planck.ui.passphrase.unlock

import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.launch
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.passphrase.PassphraseRepository
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val passphraseRepository: PassphraseRepository,
    passphraseFormatValidator: PassphraseFormatValidator,
) : PassphraseViewModel(passphraseFormatValidator) {
    private val passwordStates get() = textFieldStates.filterIsInstance<AccountTextFieldState>()

    fun start() {
        loadAccountsForUnlocking()
    }

    private fun loadAccountsForUnlocking() {
        viewModelScope.launch {
            passphraseRepository.getAccountsWithPassPhrase().onFailure {
                stateLiveData.value = PassphraseState.CoreError(it)
            }.onSuccess { accountsWithPassphrase ->
                textFieldStates.clear()
                textFieldStates.addAll(accountsWithPassphrase
                    .map { AccountTextFieldState(it.email) })
                stateLiveData.value =
                    PassphraseUnlockState.UnlockingPassphrases(passwordStates = passwordStates)
            }
        }
    }

    fun unlockKeysWithPassphrase() {
        passphraseOperation(
            onSuccess = { passphraseRepository.unlockPassphrase() }
        ) {
            stateLiveData.value = PassphraseState.Processing
            val keysWithPassphrase =
                passwordStates.map { state -> Pair(state.email, state.text) }
            planckProvider.unlockKeysWithPassphrase(ArrayList(keysWithPassphrase))
        }
    }

    override fun calculateNewOverallStatus(): PassphraseVerificationStatus {
        var success = 0
        textFieldStates.forEach { state ->
            if (state.errorStatus.isError) {
                return PassphraseVerificationStatus.WRONG_FORMAT
            } else {
                if (state.errorStatus == TextFieldStateContract.ErrorStatus.SUCCESS) {
                    success++
                }
            }
        }
        return if (success == textFieldStates.size) PassphraseVerificationStatus.SUCCESS
        else PassphraseVerificationStatus.NONE
    }

    override fun updateState(errorType: PassphraseVerificationStatus?) {
        stateLiveData.value = PassphraseUnlockState.UnlockingPassphrases(
            passwordStates,
            status = errorType ?: getCurrentStatusOrDefault()
        )
    }
}
