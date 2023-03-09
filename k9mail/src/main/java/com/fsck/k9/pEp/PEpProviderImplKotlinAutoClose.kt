package com.fsck.k9.pEp


import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
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
import com.fsck.k9.pEp.infrastructure.extensions.mapError
import com.fsck.k9.pEp.infrastructure.threading.EngineThreadLocalAutoClose
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread
import com.fsck.k9.pEp.ui.HandshakeData
import com.fsck.k9.pEp.ui.blacklist.KeyListItem
import foundation.pEp.jniadapter.*
import foundation.pEp.jniadapter.Sync.*
import foundation.pEp.jniadapter.exceptions.*
import kotlinx.coroutines.*
import security.pEp.echo.EchoMessageReceivedListener
import security.pEp.provisioning.ProvisioningFailedException
import timber.log.Timber
import java.util.*
import kotlin.coroutines.CoroutineContext

class PEpProviderImplKotlinAutoClose constructor(
    private val postExecutionThread: PostExecutionThread,
    private val context: Context,
    private val engine: EngineThreadLocalAutoClose,
) : PEpProvider {

    private val sendMessageSet = false
    private val showHandshakeSet = false
    private var echoMessageReceivedListener: EchoMessageReceivedListener? = null

    override fun setEchoMessageReceivedListener(listener: EchoMessageReceivedListener?) {
        echoMessageReceivedListener = listener
    }

    @Deprecated("unencrypted for some is not supported anymore")
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

    private fun processKeyImportSyncMessages(source: Message, decReturn: decrypt_message_Return, decryptedMimeMessage: MimeMessage): DecryptResult? {
        val flags: Int
        val lastValidDate = Date(System.currentTimeMillis() - TIMEOUT)

        when {
            decryptedMimeMessage.headerNames.contains(MimeHeader.HEADER_PEP_KEY_IMPORT)
                    || decryptedMimeMessage.headerNames.contains(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY) -> {
                if (lastValidDate.after(decryptedMimeMessage.sentDate)) {
                    flags = DecryptFlags.pEpDecryptFlagConsumed.value
                    return DecryptResult(decryptedMimeMessage,
                            decReturn.rating,
                            flags,
                            source.isEncrypted())
                }
            }
            PEpUtils.isAutoConsumeMessage(decryptedMimeMessage) -> {
                flags = when {
                    lastValidDate.after(decryptedMimeMessage.sentDate) -> DecryptFlags.pEpDecryptFlagConsumed.value
                    else -> DecryptFlags.pEpDecryptFlagIgnored.value
                }
                return DecryptResult(decryptedMimeMessage,
                        decReturn.rating,
                        flags,
                        source.isEncrypted())
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
            } else if (address.address.contains(MimeHeader.HEADER_PEP_AUTOCONSUME.uppercase(Locale.ROOT))) {
                decMsg.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME, "true")
            } else {
                replyTo.add(address)
            }
        }
        decMsg.replyTo = replyTo.toTypedArray()
    }

    @Deprecated("not needed with KeySync")
    private fun isUsablePrivateKey(result: decrypt_message_Return): Boolean {
        // TODO: 13/06/16 Check if it is necessary to check own id
        return (result.rating.value >= Rating.pEpRatingTrusted.value && result.flags == 0x01)
    }

    @Deprecated("unencrypted for some is not supported anymore")
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

    private fun handleEncryptedBCC(source: MimeMessage, pEpMessage: Message,
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

    override fun setPassiveModeEnabled(enable: Boolean) {
        engine.use { engine -> engine.get().config_passive_mode(enable) }
    }

    override fun setSubjectProtection(isProtected: Boolean) {
        engine.use { engine -> engine.get().config_unencrypted_subject(!isProtected) }
    }

    override fun configPassphrase(passphrase: String) {
        engine.use { engine ->
            engine.get().config_passphrase(passphrase)
        }
    }

    override fun configPassphraseForNewKeys(enable: Boolean, passphrase: String?) {
        engine.use { engine ->
            engine.get().config_passphrase_for_new_keys(enable, passphrase)
        }
    }

    override fun setSyncSendMessageCallback(callback: MessageToSendCallback) {
        engine.use { engine ->
            engine.get().setMessageToSendCallback(callback)
        }
    }

    override fun setSyncHandshakeCallback(activity: NotifyHandshakeCallback) {
        engine.use { engine ->
            engine.get().setNotifyHandshakeCallback(activity)
        }
    }

    @WorkerThread
    override fun disableSyncForAllIdentites() {
        engine.use { engine ->
            engine.get().disable_all_sync_channels()
        }
    }
    @WorkerThread
    override fun updateSyncAccountsConfig() {
        disableSyncForAllIdentites()
        for (account in Preferences.getPreferences(context).accounts) {
            var id = PEpUtils.createIdentity(
                Address(account.email, account.name), context
            )
            id = myself(id)
            setIdentityFlag(id, account.isPepSyncEnabled)
        }
    }

    override fun setFastPollingCallback(needsFastPollCallback: NeedsFastPollCallback) {
        engine.use { engine ->
            engine.get().setNeedsFastPollCallback(needsFastPollCallback)
        }
    }

    private fun areCallbackSet(): Boolean {
        return sendMessageSet && showHandshakeSet
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

    @WorkerThread
    override fun obtainLanguages(): Map<String, PEpLanguage>? {
        return try {
            val supportedLocales = listOf("en", "de")
            val pEpRawLanguages = engine.use { engine.get()._languagelist }

            parseRawLanguages(pEpRawLanguages, supportedLocales)
        } catch (e: pEpException) {
            Timber.e(e)
            emptyMap()
        }
    }

    private fun parseRawLanguages(
        pEpRawLanguages: String,
        supportedLocales: List<String>
    ): MutableMap<String, PEpLanguage> {
        val rawLanguages = pEpRawLanguages
            .split("\n")
            .filter { it.isNotBlank() }
            .toTypedArray()
        val availableLanguages: MutableMap<String, PEpLanguage> = HashMap()

        for (rawLanguage in rawLanguages) {
            val languageInfo = rawLanguage.split(",").toTypedArray()
            val locale = getElementAtPosition(languageInfo[0])
            if (locale in supportedLocales) {
                val language = getElementAtPosition(languageInfo[1])
                val title = getElementAtPosition(languageInfo[2])
                val pEpLanguage = PEpLanguage(locale, language, title)
                availableLanguages[locale] = pEpLanguage
            }
        }
        return availableLanguages
    }

    @Deprecated("not needed with KeySync")
    override fun generatePrivateKeyMessage(message: MimeMessage, fpr: String): com.fsck.k9.mail.Message? {
        return try {
            val containerMsg = PEpMessageBuilder(message).createMessage(context)
            containerMsg.dir = Message.Direction.Outgoing
            val pEpMessage = engine.use { engine.get().encrypt_message_and_add_priv_key(containerMsg, fpr) }
            getMimeMessage(pEpMessage)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "generatePrivateKeyMessage: ")
            null
        }
    }

    override fun isSyncRunning(): Boolean {
        return engine.use { engine ->
            engine.get().isSyncRunning
        }
    }

    private fun getElementAtPosition(chain: String): String {
        return chain.substring(1, chain.length - 1)
    }

    // ************************************************************************************
    // ************************************************************************************
    // ************************************************************************************
    //
    //                               Moved to coroutines
    //
    // ************************************************************************************
    // ************************************************************************************
    // ************************************************************************************
    @WorkerThread //Only in controller
    @Throws(pEpException::class)
    override fun encryptMessage(result: Message): Message {
        return engine.use { engine ->
            engine.get().encrypt_message(result, null, result.encFormat)
        }
    }

    @WorkerThread
    @Throws(RuntimeException::class)
    override fun encryptMessage(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> {
        // TODO: 06/12/16 add unencrypted for some
        Timber.d("%s %s", TAG, "encryptMessage() enter")
        val resultMessages: MutableList<MimeMessage> = ArrayList()
        val message = PEpMessageBuilder(source).createMessage(context)
        return try {
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
            Timber.d("%s %s", TAG, "encryptMessage() exit")
        }
    }

    @WorkerThread //Only in controller
    override fun encryptMessageToSelf(source: MimeMessage?, keys: Array<String>): MimeMessage? {
        if (source == null) {
            return null
        }
        var message: Message? = null
        return try {
            message = PEpMessageBuilder(source).createMessage(context)
            message.dir = Message.Direction.Outgoing
            Timber.d("%s %s", TAG, "encryptMessage() before encrypt to self")
            val from = message.from
            from.user_id = PEP_OWN_USER_ID
            from.me = true
            message.from = from
            var currentEnc = engine.use { engine ->
                engine.get().encrypt_message_for_self(message.from, message, convertExtraKeys(keys))
            }
            if (currentEnc == null) currentEnc = message
            Timber.d("%s %s", TAG, "encryptMessage() after encrypt to self")
            getMimeMessage(source, currentEnc)
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "encryptMessageToSelf: ")
            source
        } finally {
            message?.close()
        }
    }

    @Throws(pEpException::class, MessagingException::class)
    private fun encryptMessages(source: MimeMessage, extraKeys: Array<String>,
                                        messagesToEncrypt: List<Message>): List<MimeMessage> {
        val messages: MutableList<MimeMessage> = ArrayList()
        messagesToEncrypt.forEach { message -> messages.add(getEncryptedCopy(source, message, extraKeys)) }
        return messages
    }


    @Throws(pEpException::class, MessagingException::class, AppDidntEncryptMessageException::class)
    private fun getEncryptedCopy(source: MimeMessage,
                                 message: Message,
                                 extraKeys: Array<String>): MimeMessage {
        message.dir = Message.Direction.Outgoing
        Timber.d("%s %s", TAG, "encryptMessage() before encrypt")
        val from = message.from
        from.user_id = PEP_OWN_USER_ID
        from.me = true
        message.from = from
        val desiredEncFormat = if (source.isSet(Flag.X_PEP_DISABLED)) Message.EncFormat.None else Message.EncFormat.PEP
        var currentEnc = engine.use { engine ->
            engine.get().encrypt_message(message, convertExtraKeys(extraKeys), desiredEncFormat)
        }
        source.setFlag(Flag.X_PEP_WASNT_ENCRYPTED, source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED) && currentEnc == null)
        if (currentEnc == null) {
            if (source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED)) {
                throw AppDidntEncryptMessageException(source)
            }
            currentEnc = message
            currentEnc.encFormat = Message.EncFormat.None
        }
        Timber.d("%s %s", TAG, "encryptMessage() after encrypt")
        return getMimeMessage(source, currentEnc)
    }

    @Throws(pEpException::class, MessagingException::class)
    @WorkerThread
    private fun getEncryptedCopies(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> {
        val messagesToEncrypt: MutableList<Message> = ArrayList()
        val toEncryptMessage = stripUnencryptedRecipients(source)
        messagesToEncrypt.add(toEncryptMessage)

        if (toEncryptMessage.bcc != null) {
            handleEncryptedBCC(source, toEncryptMessage, messagesToEncrypt)
        }
        val result: List<MimeMessage> = ArrayList(encryptMessages(source, extraKeys, messagesToEncrypt))
        messagesToEncrypt.forEach { message -> message.close() }

        return result
    }

    override fun acceptSync() {
        deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeAccepted)
    }

    override fun rejectSync() {
        deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeRejected)
    }

    override fun cancelSync() {
        deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeCancel)
    }

    private fun deliverHandshakeResult(syncResult: SyncHandshakeResult) {
        engine.use { engine ->
            engine.get().deliverHandshakeResult(syncResult, Vector())
        }
    }

    override fun canEncrypt(address: String): Boolean {

        val msg = Message()
        val id = myself(PEpUtils.createIdentity(Address(address), context))
        msg.from = id

        val to = Vector<Identity>()
        to.add(id)
        msg.to = to

        msg.shortmsg = "hello, world"
        msg.longmsg = "this is a test"
        msg.dir = Message.Direction.Outgoing

        engine.use { engine ->
            try {
                engine.get().encrypt_message(msg, null, Message.EncFormat.PEP)
            } catch (e: pEpException) {
                Timber.e(e)
                return false
            }
        }
        return true
    }

    @WorkerThread
    override fun decryptMessage(source: MimeMessage, receivedBy: String): DecryptResult {
        Timber.d("%s %s", TAG, "decryptMessage() enter")
        var srcMsg: Message? = null
        var decReturn: decrypt_message_Return? = null
        return try {
            srcMsg = NotRemovingTransientFilespEpMessageBuilder(source).createMessage(context)
            srcMsg.dir = Message.Direction.Incoming
            srcMsg.recvBy = PEpUtils.createIdentity(Address(receivedBy), context)

            Timber.d("%s %s", TAG, "pEpdecryptMessage() before decrypt")
            decReturn = engine.use { engine ->
                engine.get().decrypt_message(srcMsg, Vector(), 0)
            }
            Timber.d("%s %s", TAG, "pEpdecryptMessage() *after* decrypt")

            Timber.d("%s %s", TAG, "pEpdecryptMessage() after decrypt Subject" + decReturn.dst.shortmsg)
            val message = decReturn.dst
            val decMsg = getMimeMessage(source, message)
            if (decMsg.subject.contains(ECHO_PROTOCOL_MESSAGE_SUBJECT)) {
                if (!decMsg.from.isNullOrEmpty()) {
                    echoMessageReceivedListener?.echoMessageReceived(
                        decMsg.from[0].address,
                        message.to[0].address
                    )
                }
            }

            if (PEpUtils.isAutoConsumeMessage(decMsg)) {
                Timber.e("%s %s", TAG, "Called decrypt on auto-consume message")
                if (K9.DEBUG) {
                    // Using Log.e on purpose
                    try {
                        Log.e(TAG, message.attachments[0].toString())
                    } catch (e: Exception) {
                        Timber.d(e, "%s %s", TAG, "Could not print the autoconsume message contents")
                    }
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
            val flaggedResult = processKeyImportSyncMessages(srcMsg, decReturn, decMsg)
            flaggedResult ?: DecryptResult(decMsg, decReturn.rating, -1, srcMsg.isEncrypted())

        } catch (t: Throwable) {
            Timber.e(t, "MDM ERROR: %s %s %s",
                TAG,
                "\n${source.sentDate}",
                "\n${source.messageId}"
            )
            DecryptResult(source, Rating.pEpRatingUndefined, -1, false)

        } finally {
            srcMsg?.close()
            if (decReturn != null && decReturn.dst !== srcMsg) decReturn.dst.close()
            Timber.d("%s %s", TAG, "decryptMessage() exit")
        }
    }

    override fun decryptMessage(source: MimeMessage, account: Account, callback: ResultCallback<DecryptResult>) {
        Timber.d("%s %s", TAG, "decryptMessage() enter")
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            decryptMessageSuspend(source, account, callback)
        }
    }

    private suspend fun decryptMessageSuspend(source: MimeMessage, account: Account, callback: ResultCallback<DecryptResult>) = withContext(Dispatchers.Default) {
        var srcMsg: Message? = null
        var decReturn: decrypt_message_Return? = null
        //TODO review this; we are in another thread so we should get a new engine anyways??
        try {

            srcMsg = PEpMessageBuilder(source).createMessage(context)
            srcMsg.dir = Message.Direction.Incoming
            srcMsg.recvBy = PEpUtils.createIdentity(Address(account.email), context)

            Timber.d("%s %s", TAG, "decryptMessage() before decrypt")
            decReturn = engine.use { engine ->
                engine.get().decrypt_message(srcMsg, Vector(), 0)
            }
            Timber.d("%s %s", TAG, "decryptMessage() after decrypt")

            when (decReturn.rating) {
                Rating.pEpRatingCannotDecrypt, Rating.pEpRatingHaveNoKey ->
                    notifyError(AppCannotDecryptException(KEY_MISSING_ERROR_MESSAGE), callback)
                else -> {
                    val message = decReturn.dst
                    val decMsg = getMimeMessage(source, message)

                    if (source.folder.name == account.sentFolderName || source.folder.name == account.draftsFolderName) {
                        decMsg.setHeader(MimeHeader.HEADER_PEP_RATING, PEpUtils.ratingToString(getRating(source)))
                    }

                    notifyLoaded(DecryptResult(decMsg, decReturn.rating, decReturn.flags, srcMsg.isEncrypted()), callback)
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "%s %s", TAG, "while decrypting message:")
            notifyError(AppCannotDecryptException("Could not decrypt", t), callback)
        } finally {
            srcMsg?.close()
            if (decReturn != null && decReturn.dst !== srcMsg) decReturn.dst.close()
            Timber.d("%s %s", TAG, "decryptMessage() exit")
        }
    }

    @WorkerThread
    override fun importKey(key: ByteArray): Vector<Identity> {
        return engine.use { engine ->
            engine.get().importKey(key)
        }
    }

    @WorkerThread
    override fun setOwnIdentity(id: Identity, fpr: String): Identity? {
        return try {
            val sanitizedFpr = PEpUtils.sanitizeFpr(fpr)
            engine.use { engine -> engine.get().setOwnKey(id, sanitizedFpr) }
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "error in PEpProviderImpl.setOwnIdentity")
            null
        }
    }

    @WorkerThread
    override fun myself(myId: Identity?): Identity? {
        myId?.user_id = PEP_OWN_USER_ID
        myId?.me = true
        Timber.e("%s %s", TAG, "calling myself")
        return try {
            engine.use { engine -> engine.get().myself(myId) }
        } catch (exception: pEpException) {
            Timber.e(exception, "%s %s", TAG, "error in PEpProviderImpl.myself")
            myId
        }
    }

    override fun loadOutgoingMessageRatingAfterResetTrust(
            identity: Identity, from: Address, toAddresses: List<Address>, ccAddresses: List<Address>,
            bccAddresses: List<Address>, callback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            getRatingSuspend(identity, from, toAddresses, ccAddresses, bccAddresses, callback)
        }
    }

    override fun loadMessageRatingAfterResetTrust(
            mimeMessage: MimeMessage?, isIncoming: Boolean, id: Identity, resultCallback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            loadMessageRatingAfterResetTrustSuspend(mimeMessage, isIncoming, id, resultCallback)
        }
    }

    private suspend fun loadMessageRatingAfterResetTrustSuspend(
            mimeMessage: MimeMessage?, isIncoming: Boolean, id: Identity,
            resultCallback: ResultCallback<Rating>) = withContext(Dispatchers.IO) {
        try {
            engine.use { engine ->
                engine.get().keyResetTrust(id)
                val pEpMessage = PEpMessageBuilder(mimeMessage).createMessage(context)
                val rating: Rating
                if (isIncoming) {
                    pEpMessage.dir = Message.Direction.Incoming
                    rating = engine.get().re_evaluate_message_rating(pEpMessage)
                } else {
                    pEpMessage.dir = Message.Direction.Outgoing
                    rating = engine.get().outgoing_message_rating(pEpMessage)
                }
                notifyLoaded(rating, resultCallback)
            }
        } catch (e: pEpException) {
            notifyError(e, resultCallback)
        }
    }

    @WorkerThread
    override fun incomingMessageRating(message: MimeMessage): Rating {
        return try {
            val pEpMessage = PEpMessageBuilder(message).createMessage(context)
            engine.use { engine -> engine.get().re_evaluate_message_rating(pEpMessage) }
        } catch (e: pEpException) {
            Timber.e(e)
            Rating.pEpRatingUndefined
        }
    }

    override fun incomingMessageRating(message: MimeMessage, callback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            val result = withContext(Dispatchers.IO) {
                incomingMessageRatingSuspend(message)
            }
            callback.onLoaded(result)
        }
    }

    private suspend fun incomingMessageRatingSuspend(message: MimeMessage) = withContext(Dispatchers.IO) {
        try {
            val pEpMessage = PEpMessageBuilder(message).createMessage(context)
            engine.use { engine -> engine.get().re_evaluate_message_rating(pEpMessage) }
        } catch (e: pEpException) {
            Timber.e(e)
            Rating.pEpRatingUndefined
        }
    }

    @WorkerThread //Already done
    override fun getRating(message: com.fsck.k9.mail.Message): Rating {
        val from = message.from[0]
        val to = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO))
        val cc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC))
        val bcc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC))
        return getRating(from, to, cc, bcc)
    }

    override fun getRating(message: com.fsck.k9.mail.Message, callback: ResultCallback<Rating>) {
        val from = message.from[0]
        val to = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO))
        val cc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC))
        val bcc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC))
        getRating(from, to, cc, bcc, callback)
    }

    override fun getRating(from: Address,
                           toAddresses: List<Address>,
                           ccAddresses: List<Address>,
                           bccAddresses: List<Address>,
                           callback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            getRatingSuspend(null, from, toAddresses, ccAddresses, bccAddresses, callback)
        }
    }

    /*
     *     Don't instantiate a new engine
     */
    @WorkerThread //already done
    override fun getRating(from: Address?,
                           toAddresses: List<Address>,
                           ccAddresses: List<Address>,
                           bccAddresses: List<Address>): Rating {
        if (bccAddresses.isNotEmpty()) return Rating.pEpRatingUnencrypted

        val recipientsSize = toAddresses.size + ccAddresses.size + bccAddresses.size
        if (from == null || recipientsSize == 0) return Rating.pEpRatingUndefined

        var message: Message? = null
        try {
            message = createMessageForRating(from, toAddresses, ccAddresses, bccAddresses)

            val result = getRatingOnBackground(message) // stupid way to be able to patch the value in debugger
            Timber.i("%s %s", TAG, "getRating " + result.name)
            return result
        } catch (e: Throwable) {
            Timber.e(e, "%s %s", TAG, "during color test:")
        } finally {
            message?.close()
        }
        return Rating.pEpRatingUndefined
    }

    private suspend fun getRatingSuspend(identity: Identity?, from: Address?, toAddresses: List<Address>,
                                         ccAddresses: List<Address>, bccAddresses: List<Address>,
                                         callback: ResultCallback<Rating>) = withContext(Dispatchers.IO) {
        Timber.i("Counter of PEpProviderImpl +1")
        EspressoTestingIdlingResource.increment()
        when {
            bccAddresses.isNotEmpty() -> notifyLoaded(Rating.pEpRatingUnencrypted, callback)
            else -> {
                var message: Message? = null
                try {
                    if (identity != null) engine.use { engine -> engine.get().keyResetTrust(identity) }
                    val areRecipientsEmpty = toAddresses.isEmpty() && ccAddresses.isEmpty() && bccAddresses.isEmpty()
                    if (from == null || areRecipientsEmpty) notifyLoaded(Rating.pEpRatingUndefined, callback)

                    message = createMessageForRating(from, toAddresses, ccAddresses, bccAddresses)
                    val result = getRatingOnBackground(message) // stupid way to be able to patch the value in debugger
                    Timber.i("%s %s", TAG, "getRating " + result.name)
                    notifyLoaded(result, callback)
                } catch (e: Throwable) {
                    Timber.e(e, "%s %s", TAG, "during color test:")
                    notifyError(e, callback)
                } finally {
                    Timber.i("Counter of PEpProviderImpl  -1")
                    EspressoTestingIdlingResource.decrement()
                    message?.close()
                }
            }
        }
    }

    @WorkerThread
    private fun getRatingOnBackground(message: Message): Rating {
        return try {
            engine.use { engine -> engine.get().outgoing_message_rating(message) }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "during getRating:")
            Rating.pEpRatingUndefined
        }
    }

    private fun createMessageForRating(from: Address?,
                                       toAddresses: List<Address>,
                                       ccAddresses: List<Address>,
                                       bccAddresses: List<Address>): Message {
        val idFrom = PEpUtils.createIdentity(from, context)
        idFrom.user_id = PEP_OWN_USER_ID
        idFrom.me = true

        val message = Message()
        message.from = idFrom
        message.to = PEpUtils.createIdentities(toAddresses, context)
        message.cc = PEpUtils.createIdentities(ccAddresses, context)
        message.bcc = PEpUtils.createIdentities(bccAddresses, context)
        message.shortmsg = "hello, world" // FIXME: do I need them?
        message.longmsg = "Lorem ipsum"
        message.dir = Message.Direction.Outgoing
        return message
    }

    @WorkerThread //Already done
    override fun getRating(address: Address): Rating {
        val identity = PEpUtils.createIdentity(address, context)
        return getRating(identity)
    }

    @WorkerThread //already done
    override fun getRating(identity: Identity): Rating {
        return try {
            engine.use { engine -> engine.get().identity_rating(identity) }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getRating: ")
            Rating.pEpRatingUndefined
        }
    }

    override fun getRating(address: Address, callback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            val identity = PEpUtils.createIdentity(address, context)
            getRatingSuspend(identity, callback)
        }

    }

    override fun getRating(identity: Identity, callback: ResultCallback<Rating>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            getRatingSuspend(identity, callback)
        }
    }

    private suspend fun getRatingSuspend(identity: Identity, callback: ResultCallback<Rating>) = withContext(Dispatchers.IO) {
        try {
            val rating = engine.use { engine -> engine.get().identity_rating(identity) }
            notifyLoaded(rating, callback)
        } catch (e: Exception) {
            notifyError(e, callback)
        }
    }

    override fun startSync() {
        val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        ioScope.launch {
            try {
                Timber.i("%s %s", TAG, "Trying to start sync thread engine.get().startSync()")
                engine.use { engine -> engine.get().startSync() }
            } catch (exception: pEpException) {
                Timber.e("%s %s", TAG, "Could not engine.get().startSync()", exception)
            }
        }

    }

    override fun stopSync() {
        Timber.d("%s %s", TAG, "stopSync")
        engine.use { engine -> engine.get().stopSync() }
    }

    @WorkerThread
    override fun trustwords(id: Identity, language: String): String {
        throw UnsupportedOperationException()
    }

    @WorkerThread
    override fun trustwords(myself: Identity, partner: Identity, lang: String, isShort: Boolean): String? {
        return try {
            engine.use { engine -> engine.get().get_trustwords(myself, partner, lang, !isShort) }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "trustwords: ")
            null
        }
    }

    override fun trustwords(myself: Identity, partner: Identity, lang: String, isShort: Boolean,
                            callback: SimpleResultCallback<String>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            trustwordsSuspend(myself, partner, lang, isShort, callback)
        }
    }

    private suspend fun trustwordsSuspend(
            myself: Identity, partner: Identity, lang: String, isShort: Boolean,
            callback: SimpleResultCallback<String>) = withContext(Dispatchers.IO) {
        try {
            val result = engine.use { engine ->
                engine.get().get_trustwords(myself, partner, lang, !isShort)
            }
            notifyLoaded(result, callback)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "trustwords: ")
            notifyError(e, callback)
        }
    }

    override fun obtainTrustwords(self: Identity, other: Identity, lang: String,
                                  areKeysyncTrustwords: Boolean,
                                  callback: ResultCallback<HandshakeData>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            obtainTrustwordsSuspend(self, other, lang, areKeysyncTrustwords, callback)
        }
    }

    private suspend fun obtainTrustwordsSuspend(
            self: Identity, other: Identity, lang: String, areKeysyncTrustwords: Boolean,
            callback: ResultCallback<HandshakeData>) = withContext(Dispatchers.IO) {
        try {
            engine.use { engine ->
                val myself: Identity
                val another: Identity
                if (!areKeysyncTrustwords) {
                    self.user_id = PEP_OWN_USER_ID
                    self.me = true
                    myself = engine.get().myself(self)
                    another = engine.get().updateIdentity(other)
                } else {
                    myself = self
                    another = other
                }
                val longTrustwords = engine.get().get_trustwords(myself, another, lang, true)
                val shortTrustwords = engine.get().get_trustwords(myself, another, lang, false)
                notifyLoaded(HandshakeData(longTrustwords, shortTrustwords, myself, another), callback)
            }
        } catch (e: Exception) {
            notifyError(e, callback)
        }

    }


    override fun trustPersonaKey(id: Identity) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            trustPersonaKeySuspend(id)
        }
    }

    private suspend fun trustPersonaKeySuspend(id: Identity) = withContext(Dispatchers.IO) {
        Timber.i("%s %s", TAG, "Calling trust personal key")
        engine.use { engine -> engine.get().trustPersonalKey(id) }
    }

    override fun trustOwnKey(id: Identity) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            trustOwnKeySuspend(id)
        }
    }

    private suspend fun trustOwnKeySuspend(id: Identity) = withContext(Dispatchers.IO) {
        Timber.i("%s %s", TAG, "Calling trust own key")
        engine.use { engine -> engine.get().trustOwnKey(id) }
    }

    override fun keyMistrusted(id: Identity) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            keyMistrustedSuspend(id)
        }
    }

    private suspend fun keyMistrustedSuspend(id: Identity) = withContext(Dispatchers.IO) {
        engine.use { engine -> engine.get().keyMistrusted(id) }
    }

    override fun resetTrust(id: Identity) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            resetTrustSuspend(id)
        }
    }

    private suspend fun resetTrustSuspend(id: Identity) = withContext(Dispatchers.IO) {
        engine.use { engine -> engine.get().keyResetTrust(id) }
    }

    @WorkerThread
    override fun keyResetIdentity(ident: Identity, fpr: String?) {
        val identity = updateIdentity(ident)
        try {
            engine.use { engine -> engine.get().key_reset_identity(identity, fpr) }
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        }
    }

    @WorkerThread
    override fun keyResetUser(userId: String, fpr: String?) {
        try {
            engine.use { engine -> engine.get().key_reset_user(userId, fpr) }
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        }
    }

    @WorkerThread
    override fun keyResetAllOwnKeys() {
        try {
            engine.use { engine -> engine.get().key_reset_all_own_keys() }
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        }
    }

    @WorkerThread
    @Throws(pEpException::class) // TODO: 13/1/23 review where to handle this exception.
    override fun leaveDeviceGroup() {
        engine.use { engine -> engine.get().leave_device_group() }
    }

    @WorkerThread
    override fun updateIdentity(id: Identity): Identity {
        return engine.use { engine -> engine.get().updateIdentity(id) }
    }

    @WorkerThread
    override fun getBlacklistInfo(): List<KeyListItem>? {
        try {
            val identities: MutableList<KeyListItem> = ArrayList()
            val keys = engine.use { engine ->
                engine.get().OpenPGP_list_keyinfo("")
            }
            keys?.forEach { key ->
      //          identities.add(KeyListItem(key.first, key.second, engine.get().blacklist_is_listed(key.first)))
            }
            return identities
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo")
        }
        return null
    }

    @WorkerThread
    override fun addToBlacklist(fpr: String) {
      //  engine.get().blacklist_add(fpr)
    }

    @WorkerThread
    override fun deleteFromBlacklist(fpr: String) {
    //    engine.get().blacklist_delete(fpr)
    }

    @WorkerThread
    override fun getMasterKeysInfo(): List<KeyListItem>? {
        try {
            val identities: MutableList<KeyListItem> = ArrayList()
            val keys = engine.use { engine -> engine.get().OpenPGP_list_keyinfo("") }
            keys?.forEach { key -> identities.add(KeyListItem(key.first, key.second)) }
            return identities
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo")
        }
        return null
    }

    @Deprecated("private key detection is not supported anymore, alternatives are pEp sync and import from FS")
    override fun getOwnKeyDetails(message: Message): KeyDetail? {
        try {
            val id = engine.use { engine -> engine.get().own_message_private_key_details(message) }
            return KeyDetail(id.fpr, Address(id.address, id.username))
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "getOwnKeyDetails: ")
        }
        return null
    }

    override fun loadOwnIdentities(callback: ResultCallback<List<Identity>>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            loadOwnIdentitiesSuspend(callback)
        }
    }

    private suspend fun loadOwnIdentitiesSuspend(callback: ResultCallback<List<Identity>>) = withContext(Dispatchers.IO) {
        try {
            val identitiesVector: List<Identity> = engine.use { engine ->
                engine.get().own_identities_retrieve()
            }
            notifyLoaded(identitiesVector, callback)
        } catch (error: pEpException) {
            notifyError(error, callback)
        }
    }

    override fun setIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            setIdentityFlagSuspend(identity, flags, completedCallback)
        }
    }

    private suspend fun setIdentityFlagSuspend(identity: Identity, flags: Int,
                                               completedCallback: CompletedCallback) = withContext(Dispatchers.IO) {
        try {
            engine.use { engine -> engine.get().set_identity_flags(identity, flags) }
            notifyCompleted(completedCallback)
        } catch (e: pEpException) {
            notifyError(e, completedCallback)
        }

    }

    override fun unsetIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            unsetIdentityFlagSuspend(identity, flags, completedCallback)
        }
    }

    private suspend fun unsetIdentityFlagSuspend(identity: Identity, flags: Int,
                                                 completedCallback: CompletedCallback) = withContext(Dispatchers.IO) {
        try {
            engine.use { engine -> engine.get().unset_identity_flags(identity, flags) }
            notifyCompleted(completedCallback)
        } catch (e: pEpException) {
            notifyError(e, completedCallback)
        }

    }

    @WorkerThread
    override fun setIdentityFlag(identity: Identity, sync: Boolean) {
        try {
            engine.use { engine ->
                when {
                    sync -> engine.get().enable_identity_for_sync(identity)
                    else -> engine.get().disable_identity_for_sync(identity)
                }
            }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
        }
    }

    @WorkerThread
    override fun unsetIdentityFlag(identity: Identity, flags: Int) {
        try {
            engine.use { engine -> engine.get().unset_identity_flags(identity, flags) }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
        }
    }

    @WorkerThread
    override fun printLog() {
        val uiScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        uiScope.launch {
            log.split("\n")
                    .filter { it.isNotBlank() }
                    .toTypedArray()
                    .forEach { logLine -> Timber.i("%s %s", TAG, logLine) }
        }
    }

    override fun getLog(callback: CompletedCallback): String {
        var result = ""
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            result = getLogSuspend()
            callback.onComplete()
        }
        return result
    }

    override fun getLog(): String {
        var result = ""
        val uiScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        uiScope.launch {
            result = getLogSuspend()
        }
        return result
    }

    private suspend fun getLogSuspend(): String = withContext(Dispatchers.IO) {
        engine.use { engine -> engine.get().getCrashdumpLog(100) }
    }

    fun Message.isEncrypted(): Boolean {
        return encFormat != Message.EncFormat.None
    }

    companion object {
        private const val TAG = "pEpEngine-provider"
        private const val PEP_SIGNALING_BYPASS_DOMAIN = "@peptunnel.com"
        private const val ECHO_PROTOCOL_MESSAGE_SUBJECT = "key management message (Distribution)"

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

        suspend fun provision(
            coroutineContext: CoroutineContext,
            provisionUrl: String
        ): Result<Unit> {
            return withContext(coroutineContext) {
                kotlin.runCatching {
                    //engine.get().provision(provisionUrl)
                    delay(3000L + Random().nextInt(5)*1000)
                }.mapError { ProvisioningFailedException(it.message, it) }
            }
        }
    }

}