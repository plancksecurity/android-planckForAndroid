package com.fsck.k9.planck


import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.planck.PlanckProvider.*
import com.fsck.k9.planck.PlanckProvider.Companion.ENCRYPTED_MESSAGE_POSITION
import com.fsck.k9.planck.PlanckProvider.Companion.KEY_MISSING_ERROR_MESSAGE
import com.fsck.k9.planck.PlanckProvider.Companion.PLANCK_ALWAYS_SECURE_TRUE
import com.fsck.k9.planck.PlanckProvider.Companion.PLANCK_OWN_USER_ID
import com.fsck.k9.planck.PlanckProvider.Companion.TIMEOUT
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.exceptions.AppCannotDecryptException
import com.fsck.k9.planck.infrastructure.exceptions.AppDidntEncryptMessageException
import com.fsck.k9.planck.infrastructure.exceptions.AuthFailurePassphraseNeeded
import com.fsck.k9.planck.infrastructure.exceptions.AuthFailureWrongPassphrase
import com.fsck.k9.planck.infrastructure.exceptions.CannotCreateMessageException
import com.fsck.k9.planck.infrastructure.extensions.mapError
import com.fsck.k9.planck.infrastructure.threading.EngineThreadLocal
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread
import com.fsck.k9.planck.ui.HandshakeData
import com.fsck.k9.planck.ui.blacklist.KeyListItem
import foundation.pEp.jniadapter.*
import foundation.pEp.jniadapter.Sync.*
import foundation.pEp.jniadapter.exceptions.*
import kotlinx.coroutines.*
import security.planck.echo.MessageReceivedListener
import security.planck.provisioning.ProvisioningFailedException
import timber.log.Timber
import java.util.*
import kotlin.coroutines.CoroutineContext

class PlanckProviderImplKotlin(
    private val postExecutionThread: PostExecutionThread,
    private val context: Context,
    private val engine: EngineThreadLocal,
) : PlanckProvider {

    private val sendMessageSet = false
    private val showHandshakeSet = false
    private var messageReceivedListener: MessageReceivedListener? = null
    private val engineScope = CoroutineScope(PlanckDispatcher)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun setEchoMessageReceivedListener(listener: MessageReceivedListener?) {
        messageReceivedListener = listener
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
            PlanckUtils.isAutoConsumeMessage(decryptedMimeMessage) -> {
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
            val message = PlanckMessageBuilder(source).createMessage(context)
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
        val message = PlanckMessageBuilder(src).createMessage(context)
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
        return getRating(identity).getOrDefault(Rating.pEpRatingUndefined).value > Rating.pEpRatingUnencrypted.value
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

    override fun close() {
        engine.close()
    }

    override fun setPassiveModeEnabled(enable: Boolean) = runBlocking(PlanckDispatcher) {
        engine.get().config_passive_mode(enable)
    }

    override fun setSubjectProtection(isProtected: Boolean) {
        engine.get().config_unencrypted_subject(!isProtected)
    }

    override fun configPassphrase(passphrase: String) {
        engine.get().config_passphrase(passphrase)
    }

    override fun configPassphraseForNewKeys(enable: Boolean, passphrase: String?) {
        engine.get().config_passphrase_for_new_keys(enable, passphrase)
    }

    override fun setSyncSendMessageCallback(callback: MessageToSendCallback) {
        engine.get().setMessageToSendCallback(callback)
    }

    override fun setSyncHandshakeCallback(activity: NotifyHandshakeCallback) {
        engine.get().setNotifyHandshakeCallback(activity)
    }

    @WorkerThread
    override fun disableSyncForAllIdentites() {
        engine.get().disable_all_sync_channels()
    }

    override fun syncReset() = runBlocking(PlanckDispatcher) {
        engine.get().sync_reinit()
    }

    @WorkerThread
    override fun updateSyncAccountsConfig() = runBlocking (PlanckDispatcher) {
        disableSyncForAllIdentites()
        for (account in Preferences.getPreferences(context).accounts) {
            var id = PlanckUtils.createIdentity(
                Address(account.email, account.name), context
            )
            id = myself(id)
            setIdentityFlag(id, account.isPlanckSyncEnabled)
        }
    }

    override fun setFastPollingCallback(needsFastPollCallback: NeedsFastPollCallback) {
        engine.get().setNeedsFastPollCallback(needsFastPollCallback)
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
    override fun obtainLanguages(): Map<String, PlanckLanguage> = runBlocking(PlanckDispatcher) {
        try {
            val supportedLocales = listOf("en", "de")
            val pEpRawLanguages = engine.get()._languagelist

            parseRawLanguages(pEpRawLanguages, supportedLocales)
        } catch (e: pEpException) {
            Timber.e(e)
            emptyMap()
        }
    }

    private fun parseRawLanguages(
        pEpRawLanguages: String,
        supportedLocales: List<String>
    ): MutableMap<String, PlanckLanguage> {
        val rawLanguages = pEpRawLanguages
            .split("\n")
            .filter { it.isNotBlank() }
            .toTypedArray()
        val availableLanguages: MutableMap<String, PlanckLanguage> = HashMap()

        for (rawLanguage in rawLanguages) {
            val languageInfo = rawLanguage.split(",").toTypedArray()
            val locale = getElementAtPosition(languageInfo[0])
            if (locale in supportedLocales) {
                val language = getElementAtPosition(languageInfo[1])
                val title = getElementAtPosition(languageInfo[2])
                val planckLanguage =
                    PlanckLanguage(locale, language, title)
                availableLanguages[locale] = planckLanguage
            }
        }
        return availableLanguages
    }

    @Deprecated("not needed with KeySync")
    override fun generatePrivateKeyMessage(message: MimeMessage, fpr: String): com.fsck.k9.mail.Message? {
        return try {
            val containerMsg = PlanckMessageBuilder(message).createMessage(context)
            containerMsg.dir = Message.Direction.Outgoing
            getMimeMessage(engine.get().encrypt_message_and_add_priv_key(containerMsg, fpr))
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "generatePrivateKeyMessage: ")
            null
        }
    }

    override val isSyncRunning: Boolean
        get() = runBlocking(PlanckDispatcher) {
            engine.get().isSyncRunning
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
        return engine.get().encrypt_message(result, null, result.encFormat)
    }

    @WorkerThread
    @Throws(RuntimeException::class)
    override fun encryptMessage(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage> {
        // TODO: 06/12/16 add unencrypted for some
        Timber.d("%s %s", TAG, "encryptMessage() enter")
        val resultMessages: MutableList<MimeMessage> = ArrayList()
        val message = PlanckMessageBuilder(source).createMessage(context)
        return try {
            if (source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY).isNotEmpty()) {
                val key = source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY)[0]
                var replyTo = message.replyTo
                if (replyTo == null) {
                    replyTo = Vector()
                }
                replyTo.add(PlanckUtils.createIdentity(Address(key + PEP_SIGNALING_BYPASS_DOMAIN, key), context))
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
            message = PlanckMessageBuilder(source).createMessage(context)
            message.dir = Message.Direction.Outgoing
            Timber.d("%s %s", TAG, "encryptMessage() before encrypt to self")
            val from = message.from
            from.user_id = PLANCK_OWN_USER_ID
            from.me = true
            message.from = from
            var currentEnc = engine.get().encrypt_message_for_self(message.from, message, convertExtraKeys(keys))
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
        from.user_id = PLANCK_OWN_USER_ID
        from.me = true
        message.from = from
        val desiredEncFormat = if (source.isSet(Flag.X_PEP_DISABLED)) Message.EncFormat.None else Message.EncFormat.PEP
        var currentEnc = engine.get().encrypt_message(message, convertExtraKeys(extraKeys), desiredEncFormat)
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

    private fun deliverHandshakeResult(syncResult: SyncHandshakeResult) = runBlocking(PlanckDispatcher) {
        engine.get().deliverHandshakeResult(syncResult, Vector())
    }

    override fun canEncrypt(address: String): Boolean {

        val msg = Message()
        val id = myself(PlanckUtils.createIdentity(Address(address), context))
        msg.from = id

        val to = Vector<Identity>()
        to.add(id)
        msg.to = to

        msg.shortmsg = "hello, world"
        msg.longmsg = "this is a test"
        msg.dir = Message.Direction.Outgoing

        try {
            engine.get().encrypt_message(msg, null, Message.EncFormat.PEP)
        } catch (e: pEpException) {
            Timber.e(e)
            return false
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
            srcMsg.recvBy = PlanckUtils.createIdentity(Address(receivedBy), context)

            Timber.d("%s %s", TAG, "pEpdecryptMessage() before decrypt")
            decReturn = engine.use { engine -> engine.get().decrypt_message(srcMsg, Vector(), 0) }

            Timber.d("%s %s", TAG, "pEpdecryptMessage() *after* decrypt")

            Timber.d("%s %s", TAG, "pEpdecryptMessage() after decrypt Subject" + decReturn.dst.shortmsg)
            val message = decReturn.dst
            val decMsg = getMimeMessage(source, message)
            if (!decMsg.from.isNullOrEmpty()) {
                messageReceivedListener?.messageReceived()
            }

            if (PlanckUtils.isAutoConsumeMessage(decMsg)) {
                Timber.e("%s %s", TAG, "Called decrypt on auto-consume message")
                if (K9.isDebug()) {
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

                if (isSMime(source, srcMsg.attachments)) {
                    decMsg.setFlag(Flag.X_SMIME_SIGNED, true)
                }
            }

            val neverUnprotected = (decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE).isNotEmpty()
                    && decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE)[0] == PLANCK_ALWAYS_SECURE_TRUE)
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

    private fun isSMime(source: MimeMessage, attachments: Vector<Blob>?): Boolean {
        return source.contentType.isSMimeContentType() ||
                attachments?.any { attachment ->
                    attachment.mime_type.isSMimeContentType()
                } ?: false
    }

    private fun String?.isSMimeContentType(): Boolean {
        return this?.let {
            val contentType = MimeUtility.getHeaderParameter(this, null) ?: return false
            (contentType.equals(MIME_TYPE_SMIME, ignoreCase = true)
                    || contentType.equals(MIME_TYPE_SMIME_SIGNATURE, ignoreCase = true)
                    || contentType.equals(MIME_TYPE_X_SMIME_SIGNATURE, ignoreCase = true)
                    || contentType.equals(MIME_TYPE_SMIME_10, ignoreCase = true))
        } ?: false
    }

    override fun decryptMessage(source: MimeMessage, account: Account, callback: ResultCallback<DecryptResult>) {
        Timber.d("%s %s", TAG, "decryptMessage() enter")
        engineScope.launch {
            decryptMessageSuspend(source, account, callback)
        }
    }

    private fun decryptMessageSuspend(source: MimeMessage, account: Account, callback: ResultCallback<DecryptResult>) {
        var srcMsg: Message? = null
        var decReturn: decrypt_message_Return? = null
        //TODO review this; we are in another thread so we should get a new engine anyways??
        try {

            srcMsg = PlanckMessageBuilder(source).createMessage(context)
            srcMsg.dir = Message.Direction.Incoming
            srcMsg.recvBy = PlanckUtils.createIdentity(Address(account.email), context)

            Timber.d("%s %s", TAG, "decryptMessage() before decrypt")
            decReturn = engine.get().decrypt_message(srcMsg, Vector(), 0)
            Timber.d("%s %s", TAG, "decryptMessage() after decrypt")

            when (decReturn.rating) {
                Rating.pEpRatingCannotDecrypt, Rating.pEpRatingHaveNoKey ->
                    notifyError(AppCannotDecryptException(KEY_MISSING_ERROR_MESSAGE), callback)
                else -> {
                    val message = decReturn.dst
                    val decMsg = getMimeMessage(source, message)

                    if (source.folder.name == account.sentFolderName || source.folder.name == account.draftsFolderName) {
                        decMsg.setHeader(MimeHeader.HEADER_PEP_RATING, PlanckUtils.ratingToString(getRating(source)))
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
        return engine.get().importKey(key)
    }

    @WorkerThread
    override fun importExtraKey(key: ByteArray): Vector<String> {
        return engine.get().importExtraKey(key)
    }

    @WorkerThread
    override fun setOwnIdentity(id: Identity, fpr: String): Identity? {
        return try {
            val sanitizedFpr = PlanckUtils.sanitizeFpr(fpr)
            engine.get().setOwnKey(id, sanitizedFpr)
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "error in PEpProviderImpl.setOwnIdentity")
            null
        }
    }

    @WorkerThread
    override fun myself(myId: Identity?): Identity? = runBlocking(PlanckDispatcher) {
        myId?.user_id = PLANCK_OWN_USER_ID
        myId?.me = true
        Timber.e("%s %s", TAG, "calling myself")
        try {
            engine.get().myself(myId)
        } catch (exception: pEpException) {
            Timber.e(exception, "%s %s", TAG, "error in PEpProviderImpl.myself")
            myId
        }
    }

    override fun loadOutgoingMessageRatingAfterResetTrust(
        identity: Identity,
        from: Address,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): ResultCompat<Rating> {
        return getRatingSuspend(identity, from, toAddresses, ccAddresses, bccAddresses)
    }

    override fun loadMessageRatingAfterResetTrust(
        mimeMessage: MimeMessage?,
        isIncoming: Boolean,
        id: Identity
    ): ResultCompat<Rating> = ResultCompat.of {
        engine.get().keyResetTrust(id)
        val pEpMessage = PlanckMessageBuilder(mimeMessage)
            .createMessage(context)
        if (isIncoming) {
            pEpMessage.dir = Message.Direction.Incoming
            engine.get().re_evaluate_message_rating(pEpMessage)
        } else {
            pEpMessage.dir = Message.Direction.Outgoing
            engine.get().outgoing_message_rating(pEpMessage)
        }
    }

    private fun loadMessageRatingAfterResetTrustSuspend(
            mimeMessage: MimeMessage?, isIncoming: Boolean, id: Identity,
            resultCallback: ResultCallback<Rating>) {
        try {
            engine.get().keyResetTrust(id)
            val pEpMessage = PlanckMessageBuilder(mimeMessage)
                .createMessage(context)
            val rating: Rating
            if (isIncoming) {
                pEpMessage.dir = Message.Direction.Incoming
                rating = engine.get().re_evaluate_message_rating(pEpMessage)
            } else {
                pEpMessage.dir = Message.Direction.Outgoing
                rating = engine.get().outgoing_message_rating(pEpMessage)
            }
            notifyLoaded(rating, resultCallback)
        } catch (e: pEpException) {
            notifyError(e, resultCallback)
        }
    }

    @WorkerThread
    override fun incomingMessageRating(
        message: MimeMessage
    ): ResultCompat<Rating> = ResultCompat.of {
        val planckMessage = PlanckMessageBuilder(message).createMessage(context)
        engine.get().re_evaluate_message_rating(planckMessage)
    }

    override fun incomingMessageRating(message: MimeMessage, callback: ResultCallback<Rating>) {
        uiScope.launch {
            callback.onLoaded(incomingMessageRatingSuspend(message))
        }
    }

    private suspend fun incomingMessageRatingSuspend(message: MimeMessage) = withContext(PlanckDispatcher) {
        try {
            val pEpMessage = PlanckMessageBuilder(message).createMessage(context)
            engine.get().re_evaluate_message_rating(pEpMessage)
        } catch (e: pEpException) {
            Timber.e(e)
            Rating.pEpRatingUndefined // FIXME: SORT LAUNCH/WITHCONTEXT AFTER REMOVING THIS DEFAULT RATING
        }
    }

    @WorkerThread //Already done
    override fun getRating(message: com.fsck.k9.mail.Message): Rating = runBlocking(PlanckDispatcher) {
        val from = message.from[0]
        val to = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO))
        val cc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC))
        val bcc = listOf(*message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC))
        getRating(from, to, cc, bcc)
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
        uiScope.launch {
            withContext(PlanckDispatcher) {
                getRatingSuspend(null, from, toAddresses, ccAddresses, bccAddresses)
            }.onSuccess { callback.onLoaded(it) }
                .onFailure { callback.onError(it) }
        }
    }

    override fun getRatingResult(
        from: Address,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): ResultCompat<Rating> {
        return getRatingSuspend(null, from, toAddresses, ccAddresses, bccAddresses)
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

    private fun getRatingSuspend(
        identity: Identity?,
        from: Address?,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): ResultCompat<Rating> {
        Timber.i("Counter of PEpProviderImpl +1")
        EspressoTestingIdlingResource.increment()
        return when {
            bccAddresses.isNotEmpty() -> ResultCompat.success(Rating.pEpRatingUnencrypted)
            else -> {
                var message: Message? = null
                ResultCompat.of {
                    if (identity != null) engine.get().keyResetTrust(identity)
                    val areRecipientsEmpty = toAddresses.isEmpty() && ccAddresses.isEmpty() && bccAddresses.isEmpty()
                    if (from == null || areRecipientsEmpty) Rating.pEpRatingUndefined
                    else {
                        message = createMessageForRating(from, toAddresses, ccAddresses, bccAddresses)
                        val result = getRatingOnBackground(message!!) // stupid way to be able to patch the value in debugger
                        Timber.i("%s %s", TAG, "getRating " + result.name)
                        result
                    }
                }.also {
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
        val idFrom = PlanckUtils.createIdentity(from, context)
        idFrom.user_id = PLANCK_OWN_USER_ID
        idFrom.me = true

        val message = insistToInitializeMessageOrThrow()
        message.from = idFrom
        message.to = PlanckUtils.createIdentities(toAddresses, context)
        message.cc = PlanckUtils.createIdentities(ccAddresses, context)
        message.bcc = PlanckUtils.createIdentities(bccAddresses, context)
        message.shortmsg = "hello, world" // FIXME: do I need them?
        message.longmsg = "Lorem ipsum"
        message.dir = Message.Direction.Outgoing
        return message
    }

    private fun insistToInitializeMessageOrThrow(): Message {
        var error: Throwable? = null
        repeat(MESSAGE_CREATION_ATTEMPTS) { count ->
            try {
                return Message()
            } catch (ex: Throwable) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "ERROR CREATING MESSAGE IN ATTEMPT ${count + 1}", ex)
                }
                error = ex
                runBlocking { delay(MESSAGE_CREATION_ATTEMPT_COOLDOWN) }
            }
        }

        throw CannotCreateMessageException(error!!)
    }

    @WorkerThread //Already done
    override fun getRating(address: Address): ResultCompat<Rating> = runBlocking(PlanckDispatcher) {
        val identity = PlanckUtils.createIdentity(address, context)
        getRating(identity)
    }

    @WorkerThread //already done
    override fun getRating(identity: Identity): ResultCompat<Rating> {
        return ResultCompat.of { identityRating(identity) }
            .onFailure { Timber.e(it, "%s %s", TAG, "getRating: ") }
    }

    override fun getRating(address: Address, callback: ResultCallback<Rating>) {
        uiScope.launch {
            val identity = PlanckUtils.createIdentity(address, context)
            getRatingSuspend(identity, callback)
        }

    }

    override fun getRating(identity: Identity, callback: ResultCallback<Rating>) {
        engineScope.launch {
            getRatingSuspend(identity, callback)
        }
    }

    private suspend fun getRatingSuspend(identity: Identity, callback: ResultCallback<Rating>) {
        withContext(PlanckDispatcher) {
            kotlin.runCatching { identityRating(identity) }
        }.onSuccess { notifyLoaded(it, callback) }
            .onFailure { notifyError(it, callback) }
    }

    private fun identityRating(identity: Identity): Rating {
        return try {
            val updatedIdentity = updateIdentity(identity)
            val manager = queryGroupMailManager(updatedIdentity)
            groupRating(updatedIdentity, manager)
        } catch (e: pEpGroupNotFound) {
            engine.get().identity_rating(identity)
        }
    }

    override fun startSync() {
        val ioScope = CoroutineScope(PlanckDispatcher + SupervisorJob())

        ioScope.launch {
            try {
                Timber.i("%s %s", TAG, "Trying to start sync thread engine.get().startSync()")
                engine.get().startSync()
            } catch (exception: pEpException) {
                Timber.e("%s %s", TAG, "Could not engine.get().startSync()", exception)
            }
        }

    }

    override fun stopSync() = runBlocking(PlanckDispatcher) {
        Timber.d("%s %s", TAG, "stopSync")
        engine.get().stopSync()
    }

    @WorkerThread
    override fun trustwords(id: Identity, language: String): String {
        throw UnsupportedOperationException()
    }

    @WorkerThread
    override fun trustwords(
        myself: Identity,
        partner: Identity,
        lang: String,
        isShort: Boolean
    ): ResultCompat<String> {
        return ResultCompat.of { engine.get().get_trustwords(myself, partner, lang, !isShort) }
    }

    override fun trustwords(myself: Identity, partner: Identity, lang: String, isShort: Boolean,
                            callback: SimpleResultCallback<String>) {
        engineScope.launch {
            trustwordsSuspend(myself, partner, lang, isShort, callback)
        }
    }

    private fun trustwordsSuspend(
            myself: Identity, partner: Identity, lang: String, isShort: Boolean,
            callback: SimpleResultCallback<String>) {
        try {
            val result = engine.get().get_trustwords(myself, partner, lang, !isShort)
            notifyLoaded(result, callback)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "trustwords: ")
            notifyError(e, callback)
        }
    }

    override fun obtainTrustwords(self: Identity, other: Identity, lang: String,
                                  areKeysyncTrustwords: Boolean,
                                  callback: ResultCallback<HandshakeData>) {
        engineScope.launch {
            obtainTrustwordsSuspend(self, other, lang, areKeysyncTrustwords, callback)
        }
    }

    private fun obtainTrustwordsSuspend(
            self: Identity, other: Identity, lang: String, areKeysyncTrustwords: Boolean,
            callback: ResultCallback<HandshakeData>) {
        try {
            val myself: Identity
            val another: Identity
            if (!areKeysyncTrustwords) {
                self.user_id = PLANCK_OWN_USER_ID
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
        } catch (e: Exception) {
            notifyError(e, callback)
        }

    }

    @WorkerThread
    override fun trustPersonalKey(id: Identity) {
        Timber.i("%s %s", TAG, "Calling trust personal key")
        engine.get().trustPersonalKey(id)
    }

    override fun trustOwnKey(id: Identity) {
        engineScope.launch {
            trustOwnKeySuspend(id)
        }
    }

    private fun trustOwnKeySuspend(id: Identity) {
        Timber.i("%s %s", TAG, "Calling trust own key")
        engine.get().trustOwnKey(id)
    }

    @WorkerThread
    override fun keyMistrusted(id: Identity) {
        engine.get().keyMistrusted(id)
    }

    override fun resetTrust(id: Identity) {
        engineScope.launch {
            resetTrustSuspend(id)
        }
    }

    private fun resetTrustSuspend(id: Identity) {
        engine.get().keyResetTrust(id)
    }

    @WorkerThread
    override fun keyResetIdentity(ident: Identity, fpr: String?) = runBlocking(PlanckDispatcher) {
        val identity = updateIdentity(ident)
        try {
            engine.get().key_reset_identity(identity, fpr)
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:")
        }
    }

    @WorkerThread
    override fun keyResetUser(userId: String, fpr: String?) = runBlocking(PlanckDispatcher) {
        try {
            engine.get().key_reset_user(userId, fpr)
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:")
        }
    }

    @WorkerThread
    override fun keyResetAllOwnKeys() {
        try {
            engine.get().key_reset_all_own_keys()
        } catch (e: pEpPassphraseRequired) { // TODO: 04/08/2020 Review if still needed, or callback covering it
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        } catch (e: pEpWrongPassphrase) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:")
        }
    }

    @WorkerThread
    override fun leaveDeviceGroup(): ResultCompat<Unit> = runBlocking(PlanckDispatcher) {
        ResultCompat.of { engine.get().leave_device_group() }
    }

    @WorkerThread
    override fun updateIdentity(id: Identity): Identity = runBlocking(PlanckDispatcher) {
        engine.get().updateIdentity(id)
    }

    override val blacklistInfo: List<KeyListItem>?
        @WorkerThread
        get() = runBlocking(PlanckDispatcher) {
            try {
                val identities: MutableList<KeyListItem> = ArrayList()
                val keys = engine.get().OpenPGP_list_keyinfo("")
                keys?.forEach { key ->
                    //          identities.add(KeyListItem(key.first, key.second, engine.get().blacklist_is_listed(key.first)))
                }
                return@runBlocking identities
            } catch (e: pEpException) {
                Timber.e(e, "%s %s", TAG, "getBlacklistInfo")
            }
            null
        }

    @WorkerThread
    override fun addToBlacklist(fpr: String) = runBlocking(PlanckDispatcher) {
      //  engine.get().blacklist_add(fpr)
    }

    @WorkerThread
    override fun deleteFromBlacklist(fpr: String) = runBlocking(PlanckDispatcher) {
    //    engine.get().blacklist_delete(fpr)
    }

    override val masterKeysInfo: List<KeyListItem>?
        @WorkerThread
        get() {
            try {
                val identities: MutableList<KeyListItem> = ArrayList()
                val keys = engine.get().OpenPGP_list_keyinfo("")
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
            val id = engine.get().own_message_private_key_details(message)
            return KeyDetail(id.fpr, Address(id.address, id.username))
        } catch (e: Exception) {
            Timber.e(e, "%s %s", TAG, "getOwnKeyDetails: ")
        }
        return null
    }

    override fun loadOwnIdentities(callback: ResultCallback<List<Identity>>) {
        engineScope.launch {
            loadOwnIdentitiesSuspend(callback)
        }
    }

    private fun loadOwnIdentitiesSuspend(callback: ResultCallback<List<Identity>>) {
        try {
            val identitiesVector: List<Identity> = engine.get().own_identities_retrieve()
            notifyLoaded(identitiesVector, callback)
        } catch (error: pEpException) {
            notifyError(error, callback)
        }
    }

    override fun setIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        engineScope.launch {
            setIdentityFlagSuspend(identity, flags, completedCallback)
        }
    }

    private fun setIdentityFlagSuspend(identity: Identity, flags: Int,
                                               completedCallback: CompletedCallback) {
        try {
            engine.get().set_identity_flags(identity, flags)
            notifyCompleted(completedCallback)
        } catch (e: pEpException) {
            notifyError(e, completedCallback)
        }

    }

    override fun unsetIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback) {
        engineScope.launch {
            unsetIdentityFlagSuspend(identity, flags, completedCallback)
        }
    }

    private fun unsetIdentityFlagSuspend(identity: Identity, flags: Int,
                                                 completedCallback: CompletedCallback) {
        try {
            engine.get().unset_identity_flags(identity, flags)
            notifyCompleted(completedCallback)
        } catch (e: pEpException) {
            notifyError(e, completedCallback)
        }

    }

    @WorkerThread
    override fun setIdentityFlag(identity: Identity, sync: Boolean) = runBlocking(PlanckDispatcher) {
        try {
            when {
                sync -> engine.get().enable_identity_for_sync(identity)
                else -> engine.get().disable_identity_for_sync(identity)
            }
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
        }
    }

    @WorkerThread
    override fun unsetIdentityFlag(identity: Identity, flags: Int) {
        try {
            engine.get().unset_identity_flags(identity, flags)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ")
        }
    }

    @WorkerThread
    override fun printLog() {
        uiScope.launch {
            log.split("\n")
                    .filter { it.isNotBlank() }
                    .toTypedArray()
                    .forEach { logLine -> Timber.i("%s %s", TAG, logLine) }
        }
    }

    override fun getLog(callback: CompletedCallback): String {
        var result = ""
        uiScope.launch {
            result = getLogSuspend()
            callback.onComplete()
        }
        return result
    }

    override val log: String
        @WorkerThread
        get() = runBlocking(PlanckDispatcher) {
            getLogSuspend()
        }

    private fun getLogSuspend(): String {
        return engine.get().getCrashdumpLog(100)
    }

    fun Message.isEncrypted(): Boolean {
        return encFormat != Message.EncFormat.None
    }

    @WorkerThread
    override fun createGroup(
        groupIdentity: Identity,
        manager: Identity,
        members: Vector<Identity>,
    ) {
        engine.get().adapter_group_create(groupIdentity, manager, members)
    }

    @WorkerThread
    override fun queryGroupMailManager(group: Identity): Identity = engine.get().adapter_group_query_manager(group)

    @WorkerThread
    override fun queryGroupMailMembers(group: Identity): Vector<Identity>? =
        engine.get().adapter_group_query_members(group)

    @WorkerThread
    override fun joinGroupMail(group: Identity, member: Identity, manager: Identity) =
        engine.get().adapter_group_join(group, member, manager)

    @WorkerThread
    override fun queryGroupMailManagerAndMembers(group: Identity): ResultCompat<Vector<Identity>> {
        return ResultCompat.of {
            Vector(listOf(queryGroupMailManager(group))).apply {
                queryGroupMailMembers(group)?.let { addAll(it) }
            }
        }
    }

    @WorkerThread
    override fun dissolveGroup(group: Identity, managerOrMember: Identity) {
        engine.get().adapter_group_dissolve(group, managerOrMember)
    }

    @WorkerThread
    override fun inviteMemberToGroup(group: Identity, member: Identity) {
        engine.get().adapter_group_invite_member(group, member)
    }

    @WorkerThread
    override fun removeMemberFromGroup(group: Identity, member: Identity) {
        engine.get().adapter_group_remove_member(group, member)
    }

    @WorkerThread
    override fun groupRating(group: Identity, manager: Identity): Rating {
        return engine.get().group_rating(group, manager)
    }

    override val isDeviceGrouped: Boolean
        @WorkerThread
        get() = runBlocking(PlanckDispatcher) {
            ResultCompat.of { engine.get().deviceGrouped() ?: false }
                .onFailure { Timber.e(it) }.getOrDefault(false)
        }

    @WorkerThread
    override fun getSignatureForText(text: String): ResultCompat<String> =
        ResultCompat.of { engine.get().signature_for_text(text) }

    @WorkerThread
    override fun verifySignature(textToVerify: String, signature: String): ResultCompat<Boolean> =
        ResultCompat.of {
            try {
                engine.get().verify_signature(textToVerify, signature)
                true
            } catch (ex: pEpDecryptSignatureDoesNotMatch) {
                false
            }
        }

    companion object {
        private const val TAG = "pEpEngine-provider"
        private const val PEP_SIGNALING_BYPASS_DOMAIN = "@peptunnel.com"
        private const val MESSAGE_CREATION_ATTEMPTS = 20
        private const val MESSAGE_CREATION_ATTEMPT_COOLDOWN = 10L
        private const val MIME_TYPE_X_SMIME_SIGNATURE = "application/x-pkcs7-signature"
        private const val MIME_TYPE_SMIME_SIGNATURE = "application/pkcs7-signature"
        private const val MIME_TYPE_SMIME = "application/pkcs7-mime"
        private const val MIME_TYPE_SMIME_10 = "application/pkcs10"

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
                    .setTo(PlanckUtils.createAddressesList(message.to))
                    .setCc(PlanckUtils.createAddressesList(message.cc))
                    .setBcc(PlanckUtils.createAddressesList(message.bcc))
                    .setInReplyTo(PlanckUtils.clobberVector(message.inReplyTo))
                    .setReferences(PlanckUtils.clobberVector(message.references))
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