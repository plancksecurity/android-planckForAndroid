package security.pEp.ui.passphrase

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.pEp.ui.PassphraseProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PassphrasePresenter @Inject constructor(
        @Named("AppContext") private val context: Context,
        @Named("NewInstance") private val pEp: PEpProvider
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
        PassphraseProvider.stop()
        view.finish()
    }

    fun deliverPassphrase(passphrase: String) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        when (type) {
            PassphraseRequirementType.SYNC_PASSPHRASE -> {
                scope.launch {
                    pEp.configPassphrase(passphrase)
                }
                view.finish()
            }
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE -> {
                scope.launch {
                    K9.setpEpNewKeysPassphrase(passphrase)
                    val editor = Preferences.getPreferences(context).storage.edit()
                    K9.save(editor)
                    editor.commit()
                    pEp.configPassphraseForNewKeys(true, passphrase)
                    view.finish(true)
                }
            }
            else -> {
                PassphraseProvider.passphrase = passphrase
                view.finish()
            }
        }
    }

    fun validateInput(passphrase: String) {
        view.enableActionConfirmation(passphrase.isNotEmpty())
    }

    fun cancelSync() {
        val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ioScope.launch {
            val provider = PEpProviderFactory.createProvider(context)
            try {
               provider.stopSync()
            } catch (e: Exception) {
                Timber.e(e, "pEpEngine")
            } finally {
                provider.close()
            }

        }
        view.finish()
    }

}

enum class PassphraseRequirementType {
    MISSING_PASSPHRASE,
    WRONG_PASSPHRASE,
    SYNC_PASSPHRASE,
    NEW_KEYS_PASSPHRASE
}