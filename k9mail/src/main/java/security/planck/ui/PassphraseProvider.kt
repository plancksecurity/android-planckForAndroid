package security.planck.ui

import android.content.Context
import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import foundation.pEp.jniadapter.PassphraseEntry
import foundation.pEp.jniadapter.PassphraseType
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.ui.passphrase.models.PlanckPassphraseEntry
import security.planck.ui.passphrase.old.PassphraseActivity
import security.planck.ui.passphrase.old.PassphraseRequirementType
import timber.log.Timber

object PassphraseProvider {
    const val PASSPHRASE_FOR_NEW_KEYS_ENTRY = "PASSPHRASE_FOR_NEW_KEYS_ENTRY"

    @Volatile
    @JvmStatic
    var passphraseEntry = PlanckPassphraseEntry()
    @Volatile
    var running = false
        private set
    @JvmStatic
    var createdAccountEmail = ""

    fun getPassphraseRequiredCallback(context: Context): PassphraseRequiredCallback {
        return PassphraseRequiredCallback { passphraseType, email ->
            var result: PlanckPassphraseEntry
            Log.e("pEpEngine-passphrase", "base 0")

            runBlocking {

                Log.e("pEpEngine-passphrase", "base 1")

                result = passphraseFromUser(context, passphraseType, email)
                if (passphraseType != PassphraseType.pEpPassphraseForNewKeysRequired
                    && result.isNotBlank()) {
                    MessagingController.getInstance().tryToDecryptMessagesThatCouldNotDecryptBefore()
                }
                Log.e("pEpEngine-passphrase", "base 2")

            }
            Log.e("pEpEngine-passphrase", "base 3")

            result.toPassphraseEntry()
        }
    }

    suspend fun passphraseFromUser(context: Context, passphraseType: PassphraseType, email: String): PlanckPassphraseEntry {
        prepareProvider()
        launchUI(context, passphraseType, email)
        wait()
        Log.e("pEpEngine-passphrase", " Callback END UI")

        Log.e("pEpEngine-passphrase", "Prerereturn   1")
        return passphraseEntry

    }

    private fun prepareProvider() {
        passphraseEntry = PlanckPassphraseEntry()
        running = (K9.app as K9).isRunningInForeground
    }

    private suspend fun launchUI(context: Context, passphraseType: PassphraseType, email: String) = withContext(Dispatchers.Main) {
        val type = when (passphraseType) {
            PassphraseType.pEpPassphraseRequired -> PassphraseRequirementType.MISSING_PASSPHRASE
            PassphraseType.pEpWrongPassphrase -> PassphraseRequirementType.WRONG_PASSPHRASE
            PassphraseType.pEpPassphraseForNewKeysRequired -> PassphraseRequirementType.NEW_KEYS_PASSPHRASE
        }

        PassphraseActivity.notifyRequest(context, type, email)
    }

    private suspend fun wait() = withContext(Dispatchers.IO) {
        while (passphraseEntry.isBlank() && running) {
            Timber.e("pEpEngine, delay")
            delay(1000)
        }
        Timber.e("pEpEngine, return")


    }

    fun stop() {
        running = false
    }


}

