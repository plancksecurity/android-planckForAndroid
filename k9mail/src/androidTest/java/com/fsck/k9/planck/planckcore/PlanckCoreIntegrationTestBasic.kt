package com.fsck.k9.planck.planckcore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import foundation.pEp.jniadapter.CipherSuite
import foundation.pEp.jniadapter.CommType
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Message
import foundation.pEp.jniadapter.Sync
import foundation.pEp.jniadapter.SyncHandshakeSignal
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.OrderWith
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import security.planck.file.PlanckSystemFileLocator
import timber.log.Timber
import java.util.Locale

private const val TAG = "CORE TEST"

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PlanckCoreIntegrationTestBasic {
    private lateinit var planckCore: Engine

    @Test
    fun `step1 myself on my own identity retrieves fpr from core`() {
        planckCore = Engine()
        configureCore()


        var myIdentity = createIdentity("coretest01@test.ch", "coretest01")
        myIdentity.setAsMyself()
        assertEquals(true, myIdentity.me)
        assertEquals(PlanckProvider.PLANCK_OWN_USER_ID, myIdentity.user_id)
        myIdentity = myself(myIdentity)
        planckCore.close()


        assertEquals(PlanckProvider.PLANCK_OWN_USER_ID, myIdentity.user_id)
        assertEquals(true, myIdentity.me)
        assertEquals(CommType.PEP_ct_pEp, myIdentity.comm_type)
        assertEquals(40, myIdentity.fpr.length)
    }

    @Test
    fun `step2 myself on another identity retrieves fpr from core so it seems being me or not has no effect`() {
        planckCore = Engine()
        configureCore()


        var myIdentity = createIdentity("coretest01@test.ch", "coretest01")
        assertEquals(false, myIdentity.me)
        assertEquals(null, myIdentity.user_id)
        myIdentity = myself(myIdentity)
        planckCore.close()


        assertEquals(PlanckProvider.PLANCK_OWN_USER_ID, myIdentity.user_id)
        assertEquals(true, myIdentity.me)
        assertEquals(CommType.PEP_ct_pEp, myIdentity.comm_type)
        assertEquals(40, myIdentity.fpr.length)
    }

    private fun myself(identity: Identity): Identity {
        var myIdentity = identity
        Timber.e("$TAG myIdentity before myself: ${myIdentity.print()}")
        myIdentity = planckCore.myself(myIdentity)
        Timber.e("$TAG myIdentity after myself: ${myIdentity.print()}")
        return myIdentity
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

    private fun Identity.setAsMyself() {
        user_id = PlanckProvider.PLANCK_OWN_USER_ID
        me = true
    }

    private fun Identity.print(): String {
        return "address: $address" +
                "\nuser_id: $user_id" +
                "\nusername: $username" +
                "\nme: $me" +
                "\ncommtype: $comm_type" +
                "\nflags: $flags" +
                "\nfpr: $fpr"
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

    private val testPassphraseRequired = Sync.PassphraseRequiredCallback {
        Timber.e("$TAG testPassphraseRequired got passphrase required type: $it")
        ""
    }

    companion object {
        private val app = ApplicationProvider.getApplicationContext<K9>()

        @AfterClass
        @JvmStatic
        fun tearDown() {
            clearEngineDatabases()
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
}

/**
 * TEST CASES:
 * - myself
 * -
 */