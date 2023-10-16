package security.planck.ui.passphrase

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.planck.ui.PassphraseProvider
import timber.log.Timber
import javax.inject.Inject

class PassphrasePresenter @Inject constructor(
    private val planck: PlanckProvider,
    private val preferences: Preferences,
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
                view.enableNonSyncDismiss()
                view.showNewKeysPassphrase()
            }
        }
    }

    fun cancel() {
        finish()
    }

    fun deliverPassphrase(passphrase: String) {
        val scope = CoroutineScope(PlanckDispatcher + SupervisorJob())

        when (type) {
            PassphraseRequirementType.SYNC_PASSPHRASE -> {
                scope.launch {
                    planck.configPassphrase(passphrase)
                }
                finish()
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                scope.launch {
                    K9.setPlanckNewKeysPassphrase(passphrase)
                    val editor = preferences.storage.edit()
                    K9.save(editor)
                    editor.commit()
                    planck.configPassphraseForNewKeys(true, passphrase)
                    finish(true)
                }
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
        view.enableActionConfirmation(passphrase.isNotEmpty())
    }

    fun cancelSync() {
        val scope = CoroutineScope(PlanckDispatcher + SupervisorJob())
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