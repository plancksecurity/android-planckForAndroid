package com.fsck.k9.pEp


import android.content.Context
import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.pEp.PEpProvider.*
import com.fsck.k9.pEp.infrastructure.exceptions.AppCannotDecryptException
import com.fsck.k9.pEp.infrastructure.exceptions.AppDidntEncryptMessageException
import com.fsck.k9.pEp.infrastructure.exceptions.AuthFailurePassphraseNeeded
import com.fsck.k9.pEp.infrastructure.exceptions.AuthFailureWrongPassphrase
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor
import com.fsck.k9.pEp.ui.HandshakeData
import com.fsck.k9.pEp.ui.blacklist.KeyListItem
import foundation.pEp.jniadapter.*
import foundation.pEp.jniadapter.Engine.decrypt_message_Return
import foundation.pEp.jniadapter.Sync.*
import kotlinx.coroutines.*
import security.pEp.ui.PassphraseProvider.getPassphraseRequiredCallback
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class PEpProviderImplKotlin @Inject constructor(
        private val threadExecutor: ThreadExecutor,
        private val postExecutionThread: PostExecutionThread,
        private val context: Context) : PEpProvider {

    private lateinit var engine: Engine

    private val sendMessageSet = false
    private val showHandshakeSet = false

    init {
        createEngineInstanceIfNeeded()
    }

    private fun createEngineInstanceIfNeeded() {
        if (!this::engine.isInitialized) {
            try {
                createEngineSession()
            } catch (e: pEpException) {
                Timber.e(e, "%s %s", TAG, "createIfNeeded " + Thread.currentThread().id)
            }
        } else {
            Timber.d(TAG, "createIfNeeded " + Thread.currentThread().id)
        }
    }

    @Synchronized
    override fun getRating(message: com.fsck.k9.mail.Message): Rating {
        val from = message.from[0] // FIXME: From is an array?!
        val to = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO))
        val cc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC))
        val bcc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC))
        return getRating(from, to, cc, bcc)
    }

    override fun getRating(message: com.fsck.k9.mail.Message, callback: ResultCallback<Rating>) {
        val from = message.from[0] // FIXME: From is an array?!
        val to = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO))
        val cc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC))
        val bcc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC))
        getRating(from, to, cc, bcc, callback)
    }

    private fun getRating(message: Message): Rating {
        try {
            createEngineInstanceIfNeeded()
            return engine.outgoing_message_rating(message)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "during getRating:")
        }
        return Rating.pEpRatingUndefined
    }

    @Throws(pEpException::class)
    private fun createEngineSession() {
        engine = Engine()
        initEngineConfig(engine)
    }

    private fun initEngineConfig(engine: Engine) {
        engine.config_passive_mode(K9.getPEpPassiveMode())
        configKeyServerLockup(K9.getPEpUseKeyserver())
        engine.config_unencrypted_subject(!K9.ispEpSubjectProtection())
        engine.setMessageToSendCallback(MessagingController.getInstance(context))
        engine.setNotifyHandshakeCallback((context.applicationContext as K9).notifyHandshakeCallback)
        engine.setPassphraseRequiredCallback(getPassphraseRequiredCallback(context))
    }

    @get:Throws(pEpException::class)
    private val newEngineSession: Engine
        get() {
            val engine = Engine()
            initEngineConfig(engine)
            return engine
        }

    private fun configKeyServerLockup(pEpUseKeyserver: Boolean) {
        if (pEpUseKeyserver) startKeyserverLookup() else stopKeyserverLookup()
    }

    //Don't instantiate a new engine
    @Synchronized
    override fun getRating(from: Address?, toAddresses: List<Address>, ccAddresses: List<Address>,
                           bccAddresses: List<Address>): Rating {
        if (bccAddresses.isNotEmpty()) {
            return Rating.pEpRatingUnencrypted
        }
        val recipientsSize = toAddresses.size + ccAddresses.size + bccAddresses.size
        if (from == null || recipientsSize == 0)
            return Rating.pEpRatingUndefined

        var testee: Message? = null
        try {
            createEngineInstanceIfNeeded()

            testee = Message()
            val idFrom = PEpUtils.createIdentity(from, context)
            idFrom.user_id = PEP_OWN_USER_ID
            idFrom.me = true
            testee.from = idFrom
            testee.to = PEpUtils.createIdentities(toAddresses, context)
            testee.cc = PEpUtils.createIdentities(ccAddresses, context)
            testee.bcc = PEpUtils.createIdentities(bccAddresses, context)
            testee.shortmsg = "hello, world" // FIXME: do I need them?
            testee.longmsg = "Lorem ipsum"
            testee.dir = Message.Direction.Outgoing
            val result = engine.outgoing_message_rating(testee) // stupid way to be able to patch the value in debugger
            Timber.i(TAG, "getRating " + result.name)
            return result
        } catch (e: Throwable) {
            Timber.e(e, "%s %s", TAG, "during color test:")
        } finally {
            testee?.close()
        }
        return Rating.pEpRatingUndefined
    }

    override fun getRating(from: Address, toAddresses: List<Address>, ccAddresses: List<Address>,
                           bccAddresses: List<Address>, callback: ResultCallback<Rating>) {
        getRating(null, from, toAddresses, ccAddresses, bccAddresses, callback)
    }

    private fun isUnencryptedForSome(toAddresses: List<Address>, ccAddresses: List<Address>,
                                     bccAddresses: List<Address>): Boolean {
        toAddresses.forEach { toAddress ->
            if (getRating(toAddress).value > Rating.pEpRatingUnencrypted.value) return true
        }
        ccAddresses.forEach { ccAddress ->
            if (getRating(ccAddress).value > Rating.pEpRatingUnencrypted.value) return true
        }
        bccAddresses.forEach { bccAddress ->
            if (getRating(bccAddress).value > Rating.pEpRatingUnencrypted.value) return true
        }
        return false
    }

    private fun processKeyImportSyncMessages(decReturn: decrypt_message_Return?, decryptedMimeMessage: MimeMessage): DecryptResult? {
        val flags: Int
        val lastValidDate = Date(System.currentTimeMillis() - TIMEOUT)

        when {
            decryptedMimeMessage.headerNames.contains(MimeHeader.HEADER_PEP_KEY_IMPORT)
                    || decryptedMimeMessage.headerNames.contains(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY) -> {
                if (lastValidDate.after(decryptedMimeMessage.sentDate)) {
                    flags = DecryptFlags.pEpDecryptFlagConsumed.value
                    return DecryptResult(decryptedMimeMessage, decReturn!!.rating, flags)
                }
            }
            PEpUtils.isAutoConsumeMessage(decryptedMimeMessage) -> {
                flags = when {
                    lastValidDate.after(decryptedMimeMessage.sentDate) -> DecryptFlags.pEpDecryptFlagConsumed.value
                    else -> DecryptFlags.pEpDecryptFlagIgnored.value
                }
                return DecryptResult(decryptedMimeMessage, decReturn!!.rating, flags)
            }
        }
        return null
    }

    private fun extractpEpImportHeaderFromReplyTo(decMsg: MimeMessage) {
        val replyTo = Vector<Address>()
        for (address in decMsg.replyTo) {
            if (address.hostname.contains("peptunnel")) {
                decMsg.addHeader(MimeHeader.HEADER_PEP_KEY_IMPORT, address.personal)
                decMsg.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME, "true")
            } else if (address.address.contains(MimeHeader.HEADER_PEP_AUTOCONSUME.toUpperCase(Locale.ROOT))) {
                decMsg.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME, "true")
            } else {
                replyTo.add(address)
            }
        }
        decMsg.replyTo = replyTo.toTypedArray()
    }

    override fun decryptMessage(source: MimeMessage, callback: ResultCallback<DecryptResult>) {
        threadExecutor.execute {
            Timber.d(TAG, "decryptMessage() enter")
            var srcMsg: Message? = null
            var decReturn: decrypt_message_Return? = null
            var engine: Engine? = null
            try {
                engine = newEngineSession

                srcMsg = PEpMessageBuilder(source).createMessage(context)
                srcMsg.dir = Message.Direction.Incoming

                Timber.d(TAG, "decryptMessage() before decrypt")
                decReturn = engine.decrypt_message(srcMsg, Vector(), 0)
                Timber.d(TAG, "decryptMessage() after decrypt")

                if (decReturn.rating == Rating.pEpRatingCannotDecrypt
                        || decReturn.rating == Rating.pEpRatingHaveNoKey) {
                    notifyError(AppCannotDecryptException(KEY_MIOSSING_ERORR_MESSAGE), callback)
                    return@execute
                }

                val message = decReturn.dst
                val decMsg = getMimeMessage(source, message)

                notifyLoaded(DecryptResult(decMsg, decReturn.rating, decReturn.flags), callback)

            } catch (t: Throwable) {
                Timber.e(t, "%s %s", TAG, "while decrypting message:")
                notifyError(AppCannotDecryptException("Could not decrypt", t), callback)
            } finally {
                srcMsg?.close()
                if (decReturn != null && decReturn.dst !== srcMsg) decReturn.dst.close()
                engine?.close()
                Timber.d(TAG, "decryptMessage() exit")
            }
        }
    }

    private fun isUsablePrivateKey(result: decrypt_message_Return): Boolean {
        // TODO: 13/06/16 Check if it is necessary to check own id
        return (result.rating.value >= Rating.pEpRatingTrusted.value && result.flags == 0x01)
    }

    @Throws(MessagingException::class, pEpException::class)
    private fun getUnencryptedCopies(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> {
        val messages: MutableList<MimeMessage> = ArrayList()
        messages.add(getUnencryptedBCCCopy(source))
        messages.add(getEncryptedCopy(source, getUnencryptedCopyWithoutBCC(source), extraKeys))
        return messages
    }

    private fun getUnencryptedCopyWithoutBCC(source: MimeMessage): Message {
        val message = stripEncryptedRecipients(source)
        message.bcc = null
        return message
    }

    @Throws(MessagingException::class)
    private fun getUnencryptedBCCCopy(source: MimeMessage): MimeMessage {
        val message = stripEncryptedRecipients(source)
        message.to = null
        message.cc = null
        val result = getMimeMessage(source, message)
        message.close()
        return result
    }


    private fun stripUnencryptedRecipients(source: MimeMessage): Message {
        return stripRecipients(source, false)
    }

    private fun stripEncryptedRecipients(source: MimeMessage): Message {
        return stripRecipients(source, true)
    }

    private fun handleEncryptedBCC(source: MimeMessage,
                                   pEpMessage: Message,
                                   outgoingMessageList: MutableList<Message>) {
        pEpMessage.bcc.forEach { identity ->
            val message = PEpMessageBuilder(source).createMessage(context)
            message.to = null
            message.cc = null
            val oneBCCList: Vector<Identity> = Vector<Identity>()
            oneBCCList.add(identity)
            message.bcc = oneBCCList
            outgoingMessageList.add(message)
        }

        pEpMessage.bcc = null
        if (pEpMessage.to == null && pEpMessage.cc == null && pEpMessage.bcc == null) {
            outgoingMessageList.removeAt(ENCRYPTED_MESSAGE_POSITION)
        }
    }


    private fun stripRecipients(src: MimeMessage, encrypted: Boolean): Message {
        val message = PEpMessageBuilder(src).createMessage(context)
        message.to = removeRecipients(message.to, encrypted)
        message.cc = removeRecipients(message.cc, encrypted)
        message.bcc = removeRecipients(message.bcc, encrypted)
        return message
    }

    private fun removeRecipients(recipientList: Vector<Identity>?, deletingEncrypted: Boolean): Vector<Identity>? {
        if (recipientList != null) {
            val iterator = recipientList.iterator()
            while (iterator.hasNext()) {
                val identity = iterator.next()
                if (deletingEncrypted && isEncrypted(identity)
                        || !deletingEncrypted && !isEncrypted(identity)) {
                    iterator.remove()
                }
            }
        }
        return recipientList
    }

    private fun isEncrypted(identity: Identity): Boolean {
        return getRating(identity).value > Rating.pEpRatingUnencrypted.value
    }

    private fun convertExtraKeys(extraKeys: Array<String>?): Vector<String>? {
        return if (extraKeys.isNullOrEmpty())
            null
        else {
            val rv = Vector<String>()
            Collections.addAll(rv, *extraKeys)
            rv
        }
    }

    @Synchronized
    override fun getRating(address: Address): Rating {
        val identity = PEpUtils.createIdentity(address, context)
        return getRating(identity)
    }

    @Synchronized
    override fun getRating(identity: Identity): Rating {
        createEngineInstanceIfNeeded()
        return try {
            engine.identity_rating(identity)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getRating: ")
            Rating.pEpRatingUndefined
        }
    }

    override fun getRating(identity: Identity, callback: ResultCallback<Rating>) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = Engine()
                val rating = engine.identity_rating(identity)
                notifyLoaded(rating, callback)
            } catch (e: Exception) {
                notifyError(e, callback)
            } finally {
                engine?.close()
            }
        }
    }

    override fun getRating(address: Address, callback: ResultCallback<Rating>) {
        val id = PEpUtils.createIdentity(address, context)
        getRating(id, callback)
    }

    @Synchronized
    override fun trustwords(id: Identity, language: String): String {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun trustwords(myself: Identity, partner: Identity, lang: String, isShort: Boolean): String? {
        return try {
            engine.get_trustwords(myself, partner, lang, !isShort)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "trustwords: ")
            null
        }
    }

    override fun obtainTrustwords(self: Identity, other: Identity, lang: String,
                                  areKeysyncTrustwords: Boolean,
                                  callback: ResultCallback<HandshakeData>) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = newEngineSession
                val myself: Identity
                val another: Identity
                if (!areKeysyncTrustwords) {
                    self.user_id = PEP_OWN_USER_ID
                    self.me = true
                    myself = engine.myself(self)
                    another = engine.updateIdentity(other)
                } else {
                    myself = self
                    another = other
                }
                val longTrustwords = engine.get_trustwords(myself, another, lang, true)
                val shortTrustwords = engine.get_trustwords(myself, another, lang, false)
                notifyLoaded(HandshakeData(longTrustwords, shortTrustwords, myself, another), callback)
            } catch (e: Exception) {
                notifyError(e, callback)
            } finally {
                engine?.close()
            }
        }
    }

    @Synchronized
    override fun close() {
        if (this::engine.isInitialized) {
            //engine.stopSync();
            engine.close()
        }
    }

    @Synchronized
    override fun updateIdentity(id: Identity): Identity {
        createEngineInstanceIfNeeded()
        return engine.updateIdentity(id)
    }

    @Synchronized
    override fun trustPersonaKey(id: Identity) {
        createEngineInstanceIfNeeded()
        Timber.i(TAG, "Calling trust personal key")
        engine.trustPersonalKey(id)
    }

    @Synchronized
    override fun trustOwnKey(id: Identity) {
        createEngineInstanceIfNeeded()
        Timber.i(TAG, "Calling trust own key")
        engine.trustOwnKey(id)
    }

    @Synchronized
    override fun keyMistrusted(id: Identity) {
        createEngineInstanceIfNeeded()
        engine.keyMistrusted(id)
    }

    @Synchronized
    override fun resetTrust(id: Identity) {
        createEngineInstanceIfNeeded()
        engine.keyResetTrust(id)
    }

    override fun loadMessageRatingAfterResetTrust(mimeMessage: MimeMessage?,
                                                  isIncoming: Boolean,
                                                  id: Identity,
                                                  resultCallback: ResultCallback<Rating>) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = newEngineSession
                engine.keyResetTrust(id)
                val pEpMessage = PEpMessageBuilder(mimeMessage).createMessage(context)
                val rating: Rating
                if (isIncoming) {
                    pEpMessage.dir = Message.Direction.Incoming
                    rating = engine.re_evaluate_message_rating(pEpMessage)
                } else {
                    pEpMessage.dir = Message.Direction.Outgoing
                    rating = engine.outgoing_message_rating(pEpMessage)
                }
                notifyLoaded(rating, resultCallback)
            } catch (e: pEpException) {
                notifyError(e, resultCallback)
            } finally {
                engine?.close()
            }
        }
    }

    @Synchronized
    override fun getLog(): String {
        return engine.getCrashdumpLog(100)
    }

    @Synchronized
    override fun printLog() {
        log.split("\n".toRegex()).toTypedArray()
                .forEach { logLine -> Timber.i(TAG, logLine) }
    }

    @Synchronized
    override fun myself(myId: Identity): Identity {
        createEngineInstanceIfNeeded()
        myId.user_id = PEP_OWN_USER_ID
        myId.me = true
        return engine.myself(myId)
    }

    @Synchronized
    override fun setOwnIdentity(id: Identity, fpr: String): Identity? {
        createEngineInstanceIfNeeded()
        return try {
            val sanitizedFpr = PEpUtils.sanitizeFpr(fpr)
            engine.setOwnKey(id, sanitizedFpr)
        } catch (e: Exception) {
            //TODO: Make pEpException a runtime one, and filter here
            null
        }
    }

    @Synchronized
    override fun setPassiveModeEnabled(enable: Boolean) {
        createEngineInstanceIfNeeded()
        engine.config_passive_mode(enable)
    }

    @Synchronized
    override fun startKeyserverLookup() {
        createEngineInstanceIfNeeded()
        engine.startKeyserverLookup()
    }

    @Synchronized
    override fun stopKeyserverLookup() {
        createEngineInstanceIfNeeded()
        engine.stopKeyserverLookup()
    }

    @Deprecated("private key detection is not supported anymore, alternatives are pEp sync and import from FS")
    @Synchronized
    override fun getOwnKeyDetails(message: Message): KeyDetail? {
        try {
            val id = engine.own_message_private_key_details(message)
            return KeyDetail(id.fpr, Address(id.address, id.username))
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "getOwnKeyDetails: ")
        }
        return null
    }

    @Synchronized
    override fun setSubjectProtection(isProtected: Boolean) {
        createEngineInstanceIfNeeded()
        engine.config_unencrypted_subject(!isProtected)
    }

    @Synchronized
    override fun configPassphrase(passphrase: String) {
        createEngineInstanceIfNeeded()
        engine.config_passphrase(passphrase)
    }

    @Synchronized
    override fun getBlacklistInfo(): List<KeyListItem>? {
        try {
            val identities: MutableList<KeyListItem> = ArrayList()
            val keys = engine.OpenPGP_list_keyinfo("")
            keys?.forEach { key ->
                identities.add(KeyListItem(key.first, key.second, engine.blacklist_is_listed(key.first)))
            }

            return identities
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo")
        }
        return null
    }

    @Synchronized
    override fun getMasterKeysInfo(): List<KeyListItem>? {
        try {
            val identities: MutableList<KeyListItem> = ArrayList()
            val keys = engine.OpenPGP_list_keyinfo("")
            keys?.forEach { key -> identities.add(KeyListItem(key.first, key.second)) }
            return identities
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo")
        }
        return null
    }

    @Synchronized
    override fun addToBlacklist(fpr: String) {
        engine.blacklist_add(fpr)
    }

    @Synchronized
    override fun deleteFromBlacklist(fpr: String) {
        engine.blacklist_delete(fpr)
    }

    @Synchronized
    override fun setSyncSendMessageCallback(callback: MessageToSendCallback) {
        engine.setMessageToSendCallback(callback)
    }

    private fun areCallbackSet(): Boolean {
        return sendMessageSet && showHandshakeSet
    }

    @Synchronized
    override fun setSyncHandshakeCallback(activity: NotifyHandshakeCallback) {
        engine.setNotifyHandshakeCallback(activity)
    }

    @Synchronized
    override fun startSync() {
        threadExecutor.execute {
            try {
                Timber.i("%s %s", TAG, "Trying to start sync thread Engine.startSync()")
                engine.startSync()
            } catch (exception: pEpException) {
                Timber.e("%s %s", TAG, "Could not Engine.startSync()", exception)
            }
        }
    }

    override fun stopSync() {
        Timber.d("%s %s", TAG, "stopSync")
        createEngineInstanceIfNeeded()
        engine.stopSync()
    }

    override fun loadOwnIdentities(callback: ResultCallback<List<Identity>>) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = newEngineSession
                val identitiesVector: List<Identity> = engine.own_identities_retrieve()
                notifyLoaded(identitiesVector, callback)
            } catch (error: pEpException) {
                notifyError(error, callback)
            } finally {
                engine?.close()
            }
        }
    }

    override fun setIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = newEngineSession
                engine.set_identity_flags(identity, flags)
                notifyCompleted(completedCallback)
            } catch (e: pEpException) {
                notifyError(e, completedCallback)
            } finally {
                engine?.close()
            }
        }
    }

    override fun unsetIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        threadExecutor.execute {
            var engine: Engine? = null
            try {
                engine = newEngineSession
                engine.unset_identity_flags(identity, flags)
                notifyCompleted(completedCallback)
            } catch (e: pEpException) {
                notifyError(e, completedCallback)
            } finally {
                engine?.close()
            }
        }
    }

    override fun setIdentityFlag(identity: Identity, sync: Boolean) = try {
        when {
            sync -> engine.enable_identity_for_sync(identity)
            else -> engine.disable_identity_for_sync(identity)
        }
    } catch (e: pEpException) {
        Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
    }

    override fun unsetIdentityFlag(identity: Identity, flags: Int) = try {
        engine.unset_identity_flags(identity, flags)
    } catch (e: pEpException) {
        Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
    }

    override fun setFastPollingCallback(needsFastPollCallback: NeedsFastPollCallback) {
        engine.setNeedsFastPollCallback(needsFastPollCallback)
    }

    private fun <RESULT> notifyLoaded(privacyState: RESULT, callback: ResultCallback<RESULT>) {
        postExecutionThread.post { callback.onLoaded(privacyState) }
    }

    private fun notifyCompleted(completedCallback: CompletedCallback) {
        postExecutionThread.post { completedCallback.onComplete() }
    }

    private fun notifyError(throwable: Throwable, callback: Callback) {
        postExecutionThread.post { callback.onError(throwable) }
    }

    override fun incomingMessageRating(message: MimeMessage): Rating {
        val pEpMessage = PEpMessageBuilder(message).createMessage(context)
        return try {
            engine.re_evaluate_message_rating(pEpMessage)
        } catch (e: pEpException) {
            Timber.e(e)
            Rating.pEpRatingUndefined
        }
    }

    override fun loadOutgoingMessageRatingAfterResetTrust(
            identity: Identity, from: Address, toAddresses: List<Address>, ccAddresses: List<Address>,
            bccAddresses: List<Address>, callback: ResultCallback<Rating>) {
        getRating(identity, from, toAddresses, ccAddresses, bccAddresses, callback)
    }

    private fun getRating(identity: Identity?, from: Address?, toAddresses: List<Address>,
                          ccAddresses: List<Address>, bccAddresses: List<Address>,
                          callback: ResultCallback<Rating>) {
        Timber.i("Counter of PEpProviderImpl +1")
        EspressoTestingIdlingResource.increment()
        threadExecutor.execute {
            if (bccAddresses.isNotEmpty()) {
                notifyLoaded(Rating.pEpRatingUnencrypted, callback)
                return@execute
            }
            var testee: Message? = null
            var engine: Engine? = null
            try {
                engine = newEngineSession
                if (identity != null) {
                    engine.keyResetTrust(identity)
                }
                val recipientsSize = toAddresses.size + ccAddresses.size + bccAddresses.size
                if (from == null || recipientsSize == 0) notifyLoaded(Rating.pEpRatingUndefined, callback)

                testee = Message()

                val idFrom = PEpUtils.createIdentity(from, context)
                idFrom.user_id = PEP_OWN_USER_ID
                idFrom.me = true
                testee.from = idFrom
                testee.to = PEpUtils.createIdentities(toAddresses, context)
                testee.cc = PEpUtils.createIdentities(ccAddresses, context)
                testee.bcc = PEpUtils.createIdentities(bccAddresses, context)
                testee.shortmsg = "hello, world" // FIXME: do I need them?
                testee.longmsg = "Lorem ipsum"
                testee.dir = Message.Direction.Outgoing

                val result = engine.outgoing_message_rating(testee) // stupid way to be able to patch the value in debugger
                Timber.i(TAG, "getRating " + result.name)

                notifyLoaded(result, callback)
            } catch (e: Throwable) {
                Timber.e(e, "%s %s", TAG, "during color test:")
                notifyError(e, callback)
            } finally {
                Timber.i("Counter of PEpProviderImpl  -1")
                EspressoTestingIdlingResource.decrement()
                testee?.close()
                engine?.close()
            }
        }
    }

    override fun obtainLanguages(): Map<String, PEpLanguage>? {
        return try {
            val languages: MutableMap<String, PEpLanguage> = HashMap()
            val languageList = engine._languagelist
            val languageCharacters = languageList.split("\n".toRegex()).toTypedArray()
            for (languageCharacter in languageCharacters) {
                val split = languageCharacter.split(",".toRegex()).toTypedArray()
                val pEpLanguage =
                        PEpLanguage(getElementAtPosition(split[0]),
                                getElementAtPosition(split[1]),
                                getElementAtPosition(split[2]))
                languages[getElementAtPosition(split[0])] = pEpLanguage
            }
            languages
        } catch (e: pEpException) {
            Timber.e(e)
            null
        }
    }

    override fun generatePrivateKeyMessage(message: MimeMessage, fpr: String): com.fsck.k9.mail.Message? {
        return try {
            createEngineInstanceIfNeeded()
            val containerMsg = PEpMessageBuilder(message).createMessage(context)
            containerMsg.dir = Message.Direction.Outgoing
            getMimeMessage(engine.encrypt_message_and_add_priv_key(containerMsg, fpr))
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "generatePrivateKeyMessage: ")
            null
        }
    }

    override fun importKey(key: ByteArray) {
        createEngineInstanceIfNeeded()
        engine.importKey(key)
    }

    override fun keyResetIdentity(ident: Identity, fpr: String) {
        createEngineInstanceIfNeeded()
        val identity = updateIdentity(ident)
        try {
            engine.key_reset_identity(identity, fpr)
        } catch (e: pEpPassphraseRequired) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        }
    }

    override fun keyResetUser(userId: String, fpr: String) {
        createEngineInstanceIfNeeded()
        try {
            engine.key_reset_user(userId, fpr)
        } catch (e: pEpPassphraseRequired) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        }
    }

    override fun keyResetAllOwnKeys() {
        createEngineInstanceIfNeeded()
        try {
            engine.key_reset_all_own_keys()
        } catch (e: pEpPassphraseRequired) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        }
    }

    override fun leaveDeviceGroup() {
        createEngineInstanceIfNeeded()
        engine.leave_device_group()
    }

    override fun isSyncRunning(): Boolean {
        createEngineInstanceIfNeeded()
        return engine.isSyncRunning
    }

    private fun getElementAtPosition(chain: String): String {
        return chain.substring(1, chain.length - 1)
    }

    @Throws(pEpException::class)
    override fun encryptMessage(result: Message): Message = runBlocking {
        return@runBlocking encryptMessageSuspend(result)
    }

    @Throws(pEpException::class)
    private suspend fun encryptMessageSuspend(result: Message): Message = withContext(Dispatchers.Default) {
        createEngineInstanceIfNeeded()
        return@withContext engine.encrypt_message(result, null, result.encFormat)
    }

    override fun encryptMessage(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> = runBlocking {
        encryptMessageSuspend(source, extraKeys)
    }

    private suspend fun encryptMessageSuspend(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> = withContext(Dispatchers.IO) {
        // TODO: 06/12/16 add unencrypted for some
        Timber.d(TAG, "encryptMessage() enter")
        val resultMessages: MutableList<MimeMessage> = ArrayList()
        val message = PEpMessageBuilder(source).createMessage(context)
        return@withContext try {
            createEngineInstanceIfNeeded()
            if (source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY).isNotEmpty()) {
                val key = source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY)[0]
                var replyTo = message.replyTo
                if (replyTo == null) {
                    replyTo = Vector()
                }
                replyTo.add(PEpUtils.createIdentity(Address(key + PEP_SIGNALING_BYPASS_DOMAIN, key), context))
                message.replyTo = replyTo
            }
            resultMessages.add(getEncryptedCopy(source, message, extraKeys))
            resultMessages
        } catch (e: pEpPassphraseRequired) {
            Timber.e(e, "%s %s", TAG, "while encrypting message:")
            throw AuthFailurePassphraseNeeded()
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "while encrypting message:")
            throw AuthFailureWrongPassphrase()
        } catch (e: AppDidntEncryptMessageException) {
            throw e
        } catch (t: Throwable) {
            Timber.e(t, "%s %s", TAG, "while encrypting message:")
            throw RuntimeException("Could not encrypt", t)
        } finally {
            Timber.d(TAG, "encryptMessage() exit")
        }
    }

    override fun encryptMessageToSelf(source: MimeMessage?, keys: Array<String>): MimeMessage? = runBlocking {
        encryptMessageToSelfSuspend(source, keys)
    }

    private suspend fun encryptMessageToSelfSuspend(source: MimeMessage?, keys: Array<String>): MimeMessage? = withContext(Dispatchers.Default) {
        if (source == null) {
            return@withContext null
        }
        createEngineInstanceIfNeeded()
        var message: Message? = null
        return@withContext try {
            message = PEpMessageBuilder(source).createMessage(context)
            message.dir = Message.Direction.Outgoing
            Timber.d(TAG, "encryptMessage() before encrypt to self")
            val from = message.from
            from.user_id = PEP_OWN_USER_ID
            from.me = true
            message.from = from
            var currentEnc = engine.encrypt_message_for_self(message.from, message, convertExtraKeys(keys))
            if (currentEnc == null) currentEnc = message
            Timber.d(TAG, "encryptMessage() after encrypt to self")
            getMimeMessage(source, currentEnc)
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "encryptMessageToSelf: ")
            source
        } finally {
            message?.close()
        }
    }

    @Throws(pEpException::class, MessagingException::class)
    private suspend fun encryptMessages(source: MimeMessage, extraKeys: Array<String>,
                                        messagesToEncrypt: List<Message>): List<MimeMessage> = withContext(Dispatchers.IO) {
        val messages: MutableList<MimeMessage> = ArrayList()
        messagesToEncrypt.forEach { message -> messages.add(getEncryptedCopySuspend(source, message, extraKeys)) }
        return@withContext messages
    }


    @Throws(pEpException::class, MessagingException::class, AppDidntEncryptMessageException::class)
    private fun getEncryptedCopy(source: MimeMessage,
                                 message: Message,
                                 extraKeys: Array<String>): MimeMessage = runBlocking {
        getEncryptedCopySuspend(source, message, extraKeys)
    }

    @Throws(pEpException::class, MessagingException::class, AppDidntEncryptMessageException::class)
    private suspend fun getEncryptedCopySuspend(source: MimeMessage,
                                                message: Message,
                                                extraKeys: Array<String>): MimeMessage = withContext(Dispatchers.Default) {
        message.dir = Message.Direction.Outgoing
        Timber.d(TAG, "encryptMessage() before encrypt")
        val from = message.from
        from.user_id = PEP_OWN_USER_ID
        from.me = true
        message.from = from
        var currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys), message.encFormat)
        source.setFlag(Flag.X_PEP_WASNT_ENCRYPTED, source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED) && currentEnc == null)
        if (currentEnc == null) {
            if (source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED)) {
                throw AppDidntEncryptMessageException(source)
            }
            currentEnc = message
        }
        Timber.d(TAG, "encryptMessage() after encrypt")
        return@withContext getMimeMessage(source, currentEnc)
    }

    @Throws(pEpException::class, MessagingException::class)
    private fun getEncryptedCopies(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> = runBlocking {
        val messagesToEncrypt: MutableList<Message> = ArrayList()
        val toEncryptMessage = stripUnencryptedRecipients(source)
        messagesToEncrypt.add(toEncryptMessage)

        if (toEncryptMessage.bcc != null) {
            handleEncryptedBCC(source, toEncryptMessage, messagesToEncrypt)
        }
        val result: List<MimeMessage> = ArrayList(encryptMessages(source, extraKeys, messagesToEncrypt))
        messagesToEncrypt.forEach { message -> message.close() }

        return@runBlocking result
    }

    //FIXME: Implement sync use lists.

    override fun acceptSync() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeAccepted)
        }
    }

    override fun rejectSync() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeRejected)
        }
    }

    override fun cancelSync() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeCancel)
        }
    }

    private suspend fun deliverHandshakeResult(syncResult: SyncHandshakeResult) = withContext(Dispatchers.IO) {
        engine.deliverHandshakeResult(syncResult, Vector())
    }

    override fun canEncrypt(address: String): Boolean = runBlocking {
        canEncryptSuspend(address)
    }

    private suspend fun canEncryptSuspend(address: String): Boolean = withContext(Dispatchers.IO) {
        createEngineInstanceIfNeeded()

        val msg = Message()
        val id = myself(PEpUtils.createIdentity(Address(address), context))
        msg.from = id

        val to = Vector<Identity>()
        to.add(id)
        msg.to = to

        msg.shortmsg = "hello, world"
        msg.longmsg = "this is a test"
        msg.dir = Message.Direction.Outgoing

        try {
            engine.encrypt_message(msg, null, Message.EncFormat.PEP)
        } catch (e: pEpException) {
            Timber.e(e)
            return@withContext false
        }
        return@withContext true
    }

    override fun decryptMessage(source: MimeMessage): DecryptResult = runBlocking {
        Timber.d(TAG, "decryptMessage() enter")
        decryptMessageSuspend(source)
    }

    private suspend fun decryptMessageSuspend(source: MimeMessage): DecryptResult = withContext(Dispatchers.IO) {
        var srcMsg: Message? = null
        var decReturn: decrypt_message_Return? = null
        try {
            createEngineInstanceIfNeeded()

            srcMsg = PEpMessageBuilder(source).createMessage(context)
            srcMsg.dir = Message.Direction.Incoming

            Timber.d("%s %s", TAG, "pEpdecryptMessage() before decrypt")
            decReturn = engine.decrypt_message(srcMsg, Vector(), 0)
            Timber.d("%s %s", TAG, "pEpdecryptMessage() *after* decrypt")

            Timber.d(TAG, "pEpdecryptMessage() after decrypt Subject" + decReturn.dst.shortmsg)
            val message = decReturn.dst
            val decMsg = getMimeMessage(source, message)

            if (PEpUtils.isAutoConsumeMessage(decMsg)) {
                Timber.e("%s %s", TAG, "Called decrypt on auto-consume message")
                if (K9.DEBUG) {
                    //Not using Timber on purpose.
                    Log.e(TAG, message.attachments[0].toString())
                }
            } else {
                Timber.e("%s %s", TAG, "Called decrypt on non auto-consume message")
                Timber.e("%s %s", TAG, "Subject: " + decMsg.subject + "Message-id: " + decMsg.messageId)
            }
            val neverUnprotected = (decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE).isNotEmpty()
                    && decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE)[0] == PEP_ALWAYS_SECURE_TRUE)
            decMsg.setFlag(Flag.X_PEP_NEVER_UNSECURE, neverUnprotected)

            extractpEpImportHeaderFromReplyTo(decMsg)
            // TODO: 2020-02-20 Seem like this flags currently are not used on the engine,
            //  this needs to be reviewed and probably removed
            val flaggedResult = processKeyImportSyncMessages(decReturn, decMsg)
            flaggedResult ?: DecryptResult(decMsg, decReturn.rating, -1)

        } catch (t: Throwable) {
            Timber.e(t, "%s %s", TAG, source.subject +
                    "\n${source.from[0]}" +
                    "\n${source.sentDate}" +
                    "\n${source.messageId}")
            throw AppCannotDecryptException("Could not decrypt", t)
        } finally {
            srcMsg?.close()
            if (decReturn != null && decReturn.dst !== srcMsg) decReturn.dst.close()
            Timber.d(TAG, "decryptMessage() exit")
        }
    }

    companion object {
        private const val TAG = "pEpEngine-provider"
        private const val PEP_SIGNALING_BYPASS_DOMAIN = "@peptunnel.com"

        @Throws(MessagingException::class)
        private fun getMimeMessage(source: MimeMessage?, message: Message): MimeMessage {
            val builder = MimeMessageBuilder(message).newInstance()
            var text = message.longmsgFormatted
            val messageFormat: SimpleMessageFormat
            if (!message.longmsgFormatted.isNullOrEmpty()) {
                messageFormat = SimpleMessageFormat.HTML
            } else {
                messageFormat = SimpleMessageFormat.TEXT
                text = message.longmsg
            }
            var sent = message.sent
            if (sent == null) sent = Date()
            var replyTo: Array<Address?>? = arrayOfNulls(0)
            if (source != null) {
                replyTo = source.replyTo
            }
            builder.setSubject(message.shortmsg)
                    .setSentDate(sent)
                    .setHideTimeZone(K9.hideTimeZone())
                    .setTo(PEpUtils.createAddressesList(message.to))
                    .setCc(PEpUtils.createAddressesList(message.cc))
                    .setBcc(PEpUtils.createAddressesList(message.bcc))
                    .setInReplyTo(PEpUtils.clobberVector(message.inReplyTo))
                    .setReferences(PEpUtils.clobberVector(message.references))
                    .setIdentity(message.from, replyTo)
                    .setMessageFormat(messageFormat) //.setMessageFormat(message.getEncFormat())
                    .setText(text)
                    .setAttachments(message.attachments, message.encFormat)
            //.setSignature(message.get)
            //.setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
            //.setIdentityChanged(message.get)
            //.setSignatureChanged(mSignatureChanged)
            //.setCursorPosition(mMessageContentView.getSelectionStart())
            //TODO rethink message reference
            //.setMessageReference(source.getReferences());
            //.setDraft(isDraft)
            //.setIsPgpInlineEnabled(cryptoStatus.isPgpInlineModeEnabled())
            //.setForcedUnencrypted(recipientPresenter.isForceUnencrypted());
            val mimeMessage = builder.parseMessage(message)
            val isRequestedFromPEpMessage = source == null
            if (!isRequestedFromPEpMessage) {
                val alwaysSecureHeader = source!!.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE)
                if (alwaysSecureHeader.isNotEmpty()) {
                    mimeMessage.addHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE, alwaysSecureHeader[0])
                }
                mimeMessage.setFlags(source.flags, true)
            }
            return mimeMessage
        }

        @JvmStatic
        fun getMimeMessage(message: Message): com.fsck.k9.mail.Message? {
            try {
                return getMimeMessage(null, message)
            } catch (e: MessagingException) {
                Timber.e(e, "%s %s", TAG, "getMimeMessage: ")
            }
            return null
        }
    }

}