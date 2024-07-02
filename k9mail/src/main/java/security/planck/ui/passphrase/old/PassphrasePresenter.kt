package security.planck.ui.passphrase.old

import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.ui.PassphraseProvider
import timber.log.Timber
import javax.inject.Inject

private const val ACCEPTED_SYMBOLS = """@\$!%*+\-_#?&\[\]\{\}\(\)\.:;,<>~"'\\/"""
private const val PASSPHRASE_REGEX = """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[$ACCEPTED_SYMBOLS])[A-Za-z\d$ACCEPTED_SYMBOLS]{12,}$"""

class PassphrasePresenter @Inject constructor(
    private val planck: PlanckProvider,
    private val controller: MessagingController,
    private val dispatcherProvider: DispatcherProvider,
) {
    lateinit var view: PassphraseInputView
    lateinit var type: PassphraseRequirementType
    fun init(view: PassphraseInputView, type: PassphraseRequirementType) {
        this.view = view
        this.type = type
        view.init()
        view.initAffirmativeListeners()

        when (type) {
            PassphraseRequirementType.MISSING_PASSPHRASE -> {
                view.showPasswordRequest()
                view.enableNonSyncDismiss()
            }
            PassphraseRequirementType.WRONG_PASSPHRASE -> {
                view.showRetryPasswordRequest()
                view.enableNonSyncDismiss()

            }
            PassphraseRequirementType.SYNC_PASSPHRASE -> {
                view.enableSyncDismiss()
                view.showSyncPasswordRequest()
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                val setupAccount = PassphraseProvider.newAccount
                // this means the user is setting an account
                view.enableNonSyncDismiss()
                if (setupAccount.isNotBlank()) {
                    view.showNewKeysPassphraseForAcountCreation(setupAccount)
                } else { // this should be key reset or so
                    view.showNewKeysPassphrase()
                }
            }
        }
    }

    fun cancel() {
        finish()
    }

    fun deliverPassphrase(passphrase: String) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        when (type) {
            PassphraseRequirementType.SYNC_PASSPHRASE -> {
                scope.launch {
                    withContext(dispatcherProvider.planckDispatcher()) {
                        planck.configPassphrase(passphrase)
                    }
                    controller.tryToDecryptMessagesThatCouldNotDecryptBefore()
                    finish()
                }
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                PassphraseProvider.passphrase = passphrase
                finish(true)
            }
            else -> {
                PassphraseProvider.passphrase = passphrase
                finish()
            }
        }
    }

    private fun finish(passphraseAdded: Boolean = false) {
        PassphraseProvider.stop()
        view.finish(passphraseAdded)
    }

    fun validateInput(passphrase: String) {
        val isValidPassPhrase = passphrase.isValidPassphrase()
        view.enableActionConfirmation(isValidPassPhrase)
        if (isValidPassPhrase) {
            view.hidePassphraseError()
        } else {
            view.showPassphraseError()
        }
    }

    private fun String.isValidPassphrase(): Boolean {
        return matches(PASSPHRASE_REGEX.toRegex())
    }

    fun cancelSync() {
        val scope = CoroutineScope(dispatcherProvider.planckDispatcher() + SupervisorJob())
        scope.launch {
            try {
               planck.stopSync()
            } catch (e: Exception) {
                Timber.e(e, "pEpEngine")
            }

        }
        finish()
    }

}

enum class PassphraseRequirementType {
    MISSING_PASSPHRASE,
    WRONG_PASSPHRASE,
    SYNC_PASSPHRASE,
    NEW_KEYS_PASSPHRASE
}