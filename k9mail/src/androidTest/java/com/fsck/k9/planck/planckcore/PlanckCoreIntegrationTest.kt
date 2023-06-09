package com.fsck.k9.planck.planckcore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.K9
import foundation.pEp.jniadapter.CipherSuite
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.Sync
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import security.planck.file.PlanckSystemFileLocator
import timber.log.Timber

private const val TAG = "CORE TEST"

@RunWith(AndroidJUnit4::class)
class PlanckCoreIntegrationTest {
    private lateinit var planckCore: Engine
    private val app = ApplicationProvider.getApplicationContext<K9>()

    @Test
    fun core_startup() {
        planckCore = Engine()
        configureCore()
        planckCore.startSync()
        //runBlocking(Dispatchers.IO) { delay(5000) }
        var done = false
        CoroutineScope(Dispatchers.IO).launch {
            delay(10000)
            done = true
        }
        while(!done) {
            runBlocking(Dispatchers.IO) { delay(100) }
        }
        planckCore.stopSync()
        planckCore.close()
        clearEngineDatabases()

    }

    private fun configureCore() {
        planckCore.config_passive_mode(false)
        planckCore.config_unencrypted_subject(false)
        planckCore.config_passphrase_for_new_keys(
            false,
            ""
        )
        planckCore.setMessageToSendCallback(testMessageToSend)
        planckCore.setNotifyHandshakeCallback(testNotifyHandshake)
        planckCore.setPassphraseRequiredCallback(testPassphraseRequired)
        planckCore.config_enable_echo_protocol(false)
        planckCore.config_cipher_suite(CipherSuite.pEpCipherSuiteRsa4k)

        //engine.config_media_keys(K9.getMediaKeys()?.map { it.toPair() }?.let { ArrayList(it) })
    }

    private val testMessageToSend = Sync.MessageToSendCallback {
        Timber.e("$TAG testMessageToSend got message: $it")
    }

    private val testNotifyHandshake = Sync.NotifyHandshakeCallback { myself, partner, signal ->
        Timber.e("$TAG testNotifyHandshake got notification: $myself, $partner, $signal")
    }

    private val testPassphraseRequired = PassphraseRequiredCallback {
        Timber.e("$TAG testPassphraseRequired got passphrase required type: $it")
        ""
    }

    private fun clearEngineDatabases() {
        val couldDelete = with(PlanckSystemFileLocator(app)) {
            pEpFolder.deleteRecursively() &&
            trustwordsFolder.deleteRecursively() &&
                    keyStoreFolder.deleteRecursively()
        }
        Timber.e("$TAG could delete databases: $couldDelete")
    }
}