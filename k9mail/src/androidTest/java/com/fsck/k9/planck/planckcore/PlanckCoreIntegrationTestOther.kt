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
import foundation.pEp.jniadapter.exceptions.pEpIllegalValue
import junit.framework.TestCase.assertEquals
import org.junit.AfterClass
import org.junit.Assert.assertThrows
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import security.planck.file.PlanckSystemFileLocator
import timber.log.Timber
import java.util.Locale
import java.util.Vector

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PlanckCoreIntegrationTestOther {
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
    fun `step2 encrypt message to myself`() {
        planckCore = Engine()
        configureCore()


        var myIdentity = createIdentity("coretest01@test.ch", "coretest01")
        myIdentity.setAsMyself()
        assertEquals(true, myIdentity.me)
        assertEquals(PlanckProvider.PLANCK_OWN_USER_ID, myIdentity.user_id)
        myIdentity = myself(myIdentity)

        val message = Message()
        message.dir = Message.Direction.Outgoing
        message.from = myIdentity
        message.to = Vector(listOf(myIdentity))

        Timber.e("$TAG message before encryption: ${message.print()}")


        val encryptedMessage = planckCore.encrypt_message(message, null, Message.EncFormat.PEP)
        planckCore.close()


        Timber.e("$TAG encrypted message: ${encryptedMessage.print()}")
        assertEquals(2, encryptedMessage.attachments.size) // version and encrypted message
        assertEquals("p≡p", encryptedMessage.shortmsg)
        assertEquals("this message was encrypted with p≡p https://pEp-project.org", encryptedMessage.longmsg)
        assertEquals(
            "['X-pEp-Version' : '3.3', 'X-EncStatus' : 'trusted_and_anonymized']",
            encryptedMessage.optFields.toString()
        )
        assertEquals(Message.EncFormat.PGPMIME, encryptedMessage.encFormat)
    }

    @Test
    fun `step3 encrypting a message fails if direction is not outgoing`() {
        planckCore = Engine()
        configureCore()


        var myIdentity = createIdentity("coretest01@test.ch", "coretest01")
        myIdentity.setAsMyself()
        assertEquals(true, myIdentity.me)
        assertEquals(PlanckProvider.PLANCK_OWN_USER_ID, myIdentity.user_id)
        myIdentity = myself(myIdentity)

        val message = Message()
        message.dir = Message.Direction.Incoming
        message.from = myIdentity
        message.to = Vector(listOf(myIdentity))

        Timber.e("$TAG message before encryption: ${message.print()}")

        assertThrows(pEpIllegalValue::class.java) {
            planckCore.encrypt_message(message, null, Message.EncFormat.PEP)
        }
        planckCore.close()
    }

    @Test
    @Ignore
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

    private fun Message.print(): String {
        return "from: $from" +
                "\nto: $to" +
                "\nid: ${this.id}" +
                "\ncc: ${this.cc}" +
                "\nbcc: ${this.bcc}" +
                "\nattachments: ${this.attachments?.joinToString("\n=====================================\n")}" +
                "\nsent date: ${this.sent}" +
                "\nreferences: ${this.references}" +
                "\nrecvBy: ${this.recvBy}" +
                "\nrecv: ${this.recv}" +
                "\noptfields: ${this.optFields}" +
                "\ninReplyTo: ${this.inReplyTo}" +
                "\ncomments: ${this.comments}" +
                "\ndirection: ${this.dir}" +
                "\nreplyTo: ${this.replyTo}" +
                "\nencFormat: ${this.encFormat}" +
                "\nshortmsg: ${this.shortmsg}" +
                "\nlongmsg: ${this.longmsg}"
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
        private const val TAG = "CORE TEST"
        private const val OWN_ADDRESS = "ownaddress@own.ch"
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
 * - threading test
 * - myself?
 * - decrypt message
 * - encrypt message to myself
 * - encrypt message to partner
 * - get incoming message rating
 * - get outgoing message rating
 * - trust partner
 * - mistrust partner
 * - reset partner's key
 * - reset own key
 * - reset own keys
 * - perform sync? --> For this I may need 2 devices running the test or similar. (Should be possible on jvm with 2 mains). I need 2 separate databases.
 * - reset own keys while in sync group?
 */