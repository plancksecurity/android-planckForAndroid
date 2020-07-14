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

    fun passphraseFromUser(context: Context): String {
        Log.e("pEpEngine-passphrase", "passphraseFromUser 1")
        passphrase = ""
        Handler(Looper.getMainLooper()).post {
            PassphraseActivity.launch(context, PassphraseRequirementType.MISSING_PASSPHRASE)
        }
        //runBlocking { wait() }
        Log.e("pEpEngine-passphrase", "Prerereturn 1")
        return passphrase
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

    /*
    fun getPassphraseRequiredCallback(): PassphraseRequiredCallback {
        return PassphraseRequiredCallback {
            /* val handler: Handler = Handler()
            Log.e("pEpEngine-passphrase", "start")
            passphrase
            Thread {
                Thread.sleep(10000)
                Log.e("pEpEngine-passphrase", "finish 2")
                handler.post {
                    Log.e("pEpEngine-passphrase", "finish thread UI")
                }
            }.start()
//            Handler(Lo).postDelayed({
//                Log.e("pEpEngine-passphrase", "finish 2")
//            }, 10000)
            Log.e("pEpEngine-passphrase", "finish 1")
            //Log.e("pEpEngine", "Passphrase requiered")
            //val passphrase = passphraseFromUser()
            //Log.e("pEpEngine", "Passphrase obtained ::$passphrase")
            //passphrase
            // new Handler(Looper.getMainLooper()).post(() ->  PassphraseActivity.launch(K9.this, PassphraseRequirementType.MISSING_PASSPHRASE));
            Log.e("pEpEngine-passphrase", "return")
*/
          //  return@PassphraseRequiredCallback "pEpdichauf";
            return@PassphraseRequiredCallback passphraseFromUser();
        }


    }*/

    @JvmStatic
    fun getPassphraseRequiredCallback(context: Context): PassphraseRequiredCallback {
        return PassphraseRequiredCallback {
            Log.e("pEpEngine-passphrase", "Calling callback")
            passphraseFromUser(context)
        }
    }
}

