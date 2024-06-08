package security.planck.ui

import android.content.Context
import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import foundation.pEp.jniadapter.PassphraseType
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.planck.ui.passphrase.old.PassphraseActivity
import security.planck.ui.passphrase.old.PassphraseRequirementType
import timber.log.Timber

object PassphraseProvider {
    @Volatile
    @JvmStatic
    var passphrase = ""
    @Volatile
    var running = false
        private set

    fun getPassphraseRequiredCallback(context: Context): PassphraseRequiredCallback {
        return PassphraseRequiredCallback { passphraseType ->
            var result = ""
            Log.e("pEpEngine-passphrase", "base 0")

            runBlocking {

                Log.e("pEpEngine-passphrase", "base 1")

                result = passphraseFromUser(context, passphraseType)
                if (passphraseType != PassphraseType.pEpPassphraseForNewKeysRequired
                    && result.isNotBlank()) {
                    MessagingController.getInstance().tryToDecryptMessagesThatCouldNotDecryptBefore()
                }
                Log.e("pEpEngine-passphrase", "base 2")

            }
            Log.e("pEpEngine-passphrase", "base 3")

            result
        }
    }

    suspend fun passphraseFromUser(context: Context, passphraseType: PassphraseType): String {
        prepareProvider()
        launchUI(context, passphraseType)
        wait()
        Log.e("pEpEngine-passphrase", " Callback END UI")

        Log.e("pEpEngine-passphrase", "Prerereturn   1")
        return passphrase

    }

    private fun prepareProvider() {
        passphrase = ""
        running = (K9.app as K9).isRunningInForeground
    }

    private suspend fun launchUI(context: Context, passphraseType: PassphraseType) = withContext(Dispatchers.Main) {
        val type = when (passphraseType) {
            PassphraseType.pEpPassphraseRequired -> PassphraseRequirementType.MISSING_PASSPHRASE
            PassphraseType.pEpWrongPassphrase -> PassphraseRequirementType.WRONG_PASSPHRASE
            PassphraseType.pEpPassphraseForNewKeysRequired -> PassphraseRequirementType.NEW_KEYS_PASSPHRASE
        }

        PassphraseActivity.notifyRequest(context, type)
    }

    private suspend fun wait() = withContext(Dispatchers.IO) {
        while (passphrase == "" && running) {
            Timber.e("pEpEngine, delay")
            delay(1000)
        }
        Timber.e("pEpEngine, return")


    }

    fun stop() {
        running = false;
    }


}

