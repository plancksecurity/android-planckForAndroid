package com.fsck.k9.planck.planckcore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import foundation.pEp.jniadapter.CipherSuite
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Message
import foundation.pEp.jniadapter.Sync
import foundation.pEp.jniadapter.Sync.PassphraseRequiredCallback
import foundation.pEp.jniadapter.SyncHandshakeSignal
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import security.planck.file.PlanckSystemFileLocator
import timber.log.Timber
import java.util.Locale

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
        runBlocking { delay(3000) }

        planckCore.stopSync()
        planckCore.close()
        clearEngineDatabases()
    }

    @Test
    fun startSync() {
        //clearEngineDatabases()
        //runBlocking { delay(3000) }
        planckCore = Engine()
        configureCore()
        //planckCore.disable_all_sync_channels()



        var myIdentity = createIdentity("coretest01@test.ch", "coretest01")
        myIdentity = myself(myIdentity)
        planckCore.enable_identity_for_sync(myIdentity)
        //PlanckUtils.updateSyncFlag(account, pEp, myIdentity)
        planckCore.startSync()
        Timber.e("$TAG Sync started: sync running: ${planckCore.isSyncRunning}")
        runBlocking { delay(5000) }

        planckCore.stopSync()
        Timber.e("$TAG Sync stopped: sync running: ${planckCore.isSyncRunning}")
        planckCore.close()
        assertEquals(1, messagesToSendList.size)
        assertEquals(1, notifiedHandshakeList.size)
        val msgToSend = messagesToSendList.first()
        assertEquals(3, msgToSend.attachments.size) // The sync attachment, a pgp signature and the public key (duplicated)
        assertEquals(SyncHandshakeSignal.SyncNotifySole, notifiedHandshakeList.first().signal)
        //clearEngineDatabases()
    }

    private fun myself(identity: Identity): Identity {
        var myIdentity = identity
        Timber.e("$TAG myIdentity before myself: $myIdentity")
        myIdentity.user_id = PlanckProvider.PLANCK_OWN_USER_ID
        myIdentity.me = true
        myIdentity = planckCore.myself(myIdentity)
        Timber.e("$TAG myIdentity after myself: $myIdentity")
        return myIdentity
    }

    private fun createIdentity(address: String, userName: String = ""): Identity {
        val id = Identity()
        id.address = address.lowercase(Locale.getDefault())
        id.username = address.lowercase(Locale.getDefault())
        if (userName.isNotBlank()) {
            id.username = userName
        }
        return id
        //if (PlanckUtils.isMyself(context, adr)) {
        //    id.user_id = PlanckProvider.PLANCK_OWN_USER_ID
        //    id.me = true
        //    return id
        //}
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

    private data class NotifiedHandshake(
        val myself: Identity,
        val partner: Identity,
        val signal: SyncHandshakeSignal,
    )

    private val messagesToSendList = mutableListOf<Message>()
    private val notifiedHandshakeList = mutableListOf<NotifiedHandshake>()

    private val testMessageToSend = Sync.MessageToSendCallback { msgToSend ->
        Timber.e("$TAG testMessageToSend got message: $msgToSend")
        messagesToSendList.add(msgToSend)

        Timber.e("$TAG message to send from: ${msgToSend.from}")
        Timber.e("$TAG message to send to: ${msgToSend.to}")
        Timber.e("$TAG message to send shortmsg: ${msgToSend.shortmsg}")
        Timber.e("$TAG message to send longmsg: ${msgToSend.longmsg}")
        Timber.e("$TAG message to send attachments: ${msgToSend.attachments.joinToString("\n=====================================\n")}")
        Timber.e("$TAG message to send attachments size: ${msgToSend.attachments.size}")
        Timber.e("$TAG message to send encformat: ${msgToSend.encFormat}")
        Timber.e("$TAG message to send replyto: ${msgToSend.replyTo}")
        Timber.e("$TAG message to send cc: ${msgToSend.cc}")
        Timber.e("$TAG message to send bcc: ${msgToSend.bcc}")
        Timber.e("$TAG message to send comments: ${msgToSend.comments}")
        Timber.e("$TAG message to send inreplyto: ${msgToSend.inReplyTo}")
        Timber.e("$TAG message to send id: ${msgToSend.id}")
        Timber.e("$TAG message to send direction: ${msgToSend.dir}")
        Timber.e("$TAG message to send keywords: ${msgToSend.keywords}")
        Timber.e("$TAG message to send optfields: ${msgToSend.optFields}")
        Timber.e("$TAG message to send recv: ${msgToSend.recv}")
        Timber.e("$TAG message to send recBy: ${msgToSend.recvBy}")
        Timber.e("$TAG message to send references: ${msgToSend.references}")
        Timber.e("$TAG message to send sent date: ${msgToSend.sent}")
    }

    private val testNotifyHandshake = Sync.NotifyHandshakeCallback { myself, partner, signal ->
        Timber.e("$TAG testNotifyHandshake got notification: $myself, $partner, $signal")
        notifiedHandshakeList.add(NotifiedHandshake(myself, partner, signal))
        Timber.e("$TAG notify handshake myself: $myself")
        Timber.e("$TAG notify handshake partner: $partner")
        Timber.e("$TAG notify handshake signal: $signal")
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