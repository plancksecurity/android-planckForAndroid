package security.pEp.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import kotlinx.coroutines.*
import security.pEp.ui.passphrase.PassphraseActivity
import security.pEp.ui.passphrase.PassphraseRequirementType
import timber.log.Timber

object PassphraseProvider {
    @Volatile
    @JvmStatic
    var passphrase = ""
    @JvmStatic
    fun getPassphraseRequiredCallback(context: Context): PassphraseRequiredCallback {
        return PassphraseRequiredCallback {
            Log.e("pEpEngine-passphrase", "Calling callback")
            var pass = ""
            pass = passphraseFromUser(context)

            pass
        }
    }

    fun passphraseFromUser(context: Context): String = runBlocking {
        Log.e("pEpEngine-passphrase", "passphraseFromUser 1")
        passphrase = ""
        bla(context)
        wait()
        Log.e("pEpEngine-passphrase", "Prerereturn 1")
        passphrase
    }

    private suspend fun bla(context: Context) = withContext(Dispatchers.Main) {
        PassphraseActivity.launch(context, PassphraseRequirementType.MISSING_PASSPHRASE)

    }

    private suspend fun wait() = withContext(Dispatchers.IO) {
        var seconds = 0
        while (passphrase == "" && seconds < 30) {
            Timber.e("pEpEngine, delay")
            delay(1000)
            seconds += 1
        }
        Timber.e("pEpEngine, return")
    }



}

