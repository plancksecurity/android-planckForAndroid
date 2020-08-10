package security.pEp.ui

import android.content.Context
import android.util.Log
import foundation.pEp.jniadapter.PassphraseType
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import kotlinx.coroutines.*
import security.pEp.ui.passphrase.PassphraseActivity
import security.pEp.ui.passphrase.PassphraseRequirementType
import timber.log.Timber

object PassphraseProvider {
    @Volatile
    @JvmStatic
    var passphrase = ""
    @Volatile
    var running = false

    fun getPassphraseRequiredCallback(context: Context): PassphraseRequiredCallback {
        return PassphraseRequiredCallback {passphraseType ->
            var result = ""
            Log.e("pEpEngine-passphrase", "base 0")

            runBlocking {

                Log.e("pEpEngine-passphrase", "base 1")

                result = passphraseFromUser(context, passphraseType)
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
        running = true
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

