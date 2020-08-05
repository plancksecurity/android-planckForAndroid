package security.pEp.ui

import android.content.Context
import android.util.Log
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
        return PassphraseRequiredCallback {
            var result = ""
            Log.e("pEpEngine-passphrase", "base 0")

            runBlocking {

                Log.e("pEpEngine-passphrase", "base 1")

                result = passphraseFromUser(context)
                Log.e("pEpEngine-passphrase", "base 2")

            }
            Log.e("pEpEngine-passphrase", "base 3")

            result
        }
    }

    suspend fun passphraseFromUser(context: Context): String {
        prepareProvider()
        launchUI(context)
        wait()
        Log.e("pEpEngine-passphrase", " Callback END UI")

        Log.e("pEpEngine-passphrase", "Prerereturn   1")
        return passphrase

    }

    private fun prepareProvider() {
        passphrase = ""
        running = true
    }

    private suspend fun launchUI(context: Context) = withContext(Dispatchers.Main) {
        PassphraseActivity.notifyRequest(context, PassphraseRequirementType.MISSING_PASSPHRASE)

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

