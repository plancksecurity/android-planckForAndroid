package security.pEp.ui.passphrase

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PassphrasePresenter @Inject constructor(@Named("AppContext") private val context: Context) {
    lateinit var view: PassphraseInputView

    fun init(view: PassphraseInputView, type: PassphraseRequirementType) {
        this.view = view
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
        }
    }

    fun cancel() {
        view.finish();
    }

    fun deliverPassphrase(passphrase: String) {
        val provider = PEpProviderFactory.createAndSetupProvider(context)
        provider.configPassphrase(passphrase)
        provider.close()
        view.finish()

    }

    fun validateInput(passphrase: String) {
        view.enableActionConfirmation(passphrase.isNotEmpty())
    }

    fun cancelSync() {
        val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ioScope.launch {
            val provider = PEpProviderFactory.createAndSetupProvider(context)
            try {
                (context.applicationContext as K9).shutdownSync(provider)
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
    SYNC_PASSPHRASE
}