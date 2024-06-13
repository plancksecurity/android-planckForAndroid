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
    lateinit var email: String
    fun init(view: PassphraseInputView, type: PassphraseRequirementType, email: String) {
        this.view = view
        this.type = type
        this.email = email
        view.init()
        view.initAffirmativeListeners()

        when (type) {
            PassphraseRequirementType.MISSING_PASSPHRASE -> {
                view.showPasswordRequest(email)
                view.enableNonSyncDismiss()
            }
            PassphraseRequirementType.WRONG_PASSPHRASE -> {
                view.showRetryPasswordRequest(email)
                view.enableNonSyncDismiss()

            }
            PassphraseRequirementType.SYNC_PASSPHRASE -> {
                view.enableSyncDismiss()
                view.showSyncPasswordRequest(email)
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                view.enableNonSyncDismiss()
                view.showNewKeysPassphrase()
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
                        planck.configPassphrase(email, passphrase)
                    }
                    controller.tryToDecryptMessagesThatCouldNotDecryptBefore()
                    finish()
                }
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                PassphraseProvider.passphrase = passphrase
                K9.setPlanckNewKeysPassphrase(passphrase)
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