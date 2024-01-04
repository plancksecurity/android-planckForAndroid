package com.fsck.k9.planck

import com.fsck.k9.Account
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.ui.HandshakeData
import com.fsck.k9.planck.ui.blacklist.KeyListItem
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import foundation.pEp.jniadapter.Sync.MessageToSendCallback
import foundation.pEp.jniadapter.Sync.NeedsFastPollCallback
import foundation.pEp.jniadapter.Sync.NotifyHandshakeCallback
import foundation.pEp.jniadapter.exceptions.pEpException
import security.planck.echo.MessageReceivedListener
import timber.log.Timber
import java.util.Vector

/**
 * Created by dietz on 01.07.15.
 */
interface PlanckProvider {
    fun setEchoMessageReceivedListener(listener: MessageReceivedListener?)

    /**
     * checks the privacy level of the addresses supplied. This method creates a pEp message and
     * calls the jni adapter to obtain the info. According to fdik, this check returns fast (all
     * time consuming stuff (network i/o etc.) is done asynchronously.
     *
     * @param from        from email address
     * @param toAddresses  to addresses
     * @param ccAddresses  cc addresses
     * @param bccAddresses bcc addresses
     * @return the privacy level of a mail sent to the set of recipients
     */
    fun getRating(
        from: Address?,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): Rating

    fun getRating(message: Message): Rating
    fun getRating(message: Message, callback: ResultCallback<Rating>)
    fun getRating(
        from: Address,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>,
        callback: ResultCallback<Rating>
    )

    fun getRatingResult(
        from: Address,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): ResultCompat<Rating>

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real
     * pEp library interaction.
     *
     *
     * Implications from feeding LocalMessages into decryptMessage are currently not completely understood...
     *
     * @param source the (fully qualified) message to be decrypted.
     * @param receivedBy The email address which received the message
     * @return the decrypted message or the original message in case we cannot decrypt
     *
     *
     * TODO: pEp: how do I get the color Perhaps Via header value in return value
     */
    fun decryptMessage(source: MimeMessage, receivedBy: String): DecryptResult

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real pEp library interaction.
     *
     *
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return the decrypted message or error en case we cannot decrypt or engine fails
     *
     *
     */
    fun decryptMessage(
        source: MimeMessage,
        account: Account,
        callback: ResultCallback<DecryptResult>
    )

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real pEp library interaction.
     *
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return [Result] with the decrypted message or error en case we cannot decrypt or engine fails
     */
    suspend fun decryptMessage(
        source: MimeMessage,
        account: Account
    ): Result<DecryptResult>

    /**
     * Encrypts one k9 message. This one hides all the black magic associated with the real
     * pEp library interaction.
     *
     *
     * Implications from feeding LocalMessages into decryptMessage are currently not completely understood...
     *
     *
     * FIXME: where do I handle Bcc: corner case
     * FIXME: where do I handle split for different privacy levels (To1 is green, To2 is yellow) Is this really necessary
     *
     * @param source    the (fully qualified) message to be encrypted.
     * @param extraKeys extra key ids to encrypt msg to...
     * @return the encrypted message
     */
    fun encryptMessage(source: MimeMessage, extraKeys: Array<String>): List<MimeMessage>

    //TODO> When alias available check if it works correctly
    @Throws(MessagingException::class)
    fun encryptMessageToSelf(source: MimeMessage?, keys: Array<String>): MimeMessage?

    /**
     * Checks the trust status (Color) for a given identity
     *
     * @param identity
     * @return identity trust status color
     */
    fun getRating(identity: Identity): ResultCompat<Rating>
    fun getRating(identity: Identity, callback: ResultCallback<Rating>)
    fun getRating(address: Address, callback: ResultCallback<Rating>)
    suspend fun getRating(address: Address): ResultCompat<Rating>

    /**
     * Retrieve long trustwords for a given identity
     *
     * @param id
     * @return trustwords string
     */
    fun trustwords(id: Identity, language: String): String
    fun trustwords(
        myself: Identity,
        partner: Identity,
        lang: String,
        isShort: Boolean
    ): ResultCompat<String>

    fun trustwords(
        myself: Identity,
        partner: Identity,
        lang: String,
        isShort: Boolean,
        callback: SimpleResultCallback<String>
    )

    fun obtainTrustwords(
        myself: Identity, partner: Identity, lang: String, areKeysyncTrustwords: Boolean,
        callback: ResultCallback<HandshakeData>
    )

    /**
     * Close the engine/session associated to the provider
     */
    fun close()

    /**
     * Returns a identity with the attributes related to the given identity filler, like fpr if available.
     *
     * @param id identity to fill
     * @return identity filled
     */
    fun updateIdentity(id: Identity): Identity

    /**
     * Trust on identity
     *
     * @param id identity to trust it
     */
    fun trustPersonalKey(id: Identity)

    /**
     * Trust own identity
     *
     * @param id identity to trust it
     */
    fun trustOwnKey(id: Identity)

    /**
     * Mark key as compromised
     *
     * @param id identity to mark
     */
    fun keyMistrusted(id: Identity)

    /**
     * Reset id trust status, to do handshake again.
     *
     * @param id identity to reset trust
     */
    fun resetTrust(id: Identity)
    fun myself(myId: Identity?): Identity?
    fun setOwnIdentity(id: Identity, fpr: String): Identity?
    fun setPassiveModeEnabled(enable: Boolean)
    fun getOwnKeyDetails(message: foundation.pEp.jniadapter.Message): KeyDetail?
    fun setSubjectProtection(enabled: Boolean)
    fun configPassphrase(passphrase: String)
    fun configPassphraseForNewKeys(enable: Boolean, passphrase: String?)
    val blacklistInfo: List<KeyListItem>?
    val masterKeysInfo: List<KeyListItem>?
    fun addToBlacklist(fpr: String)
    fun deleteFromBlacklist(fpr: String)

    //com.fsck.k9.mail.Message getMimeMessage(Message message);
    suspend fun acceptSync()
    suspend fun rejectSync()
    suspend fun cancelSync()
    fun loadMessageRatingAfterResetTrust(
        mimeMessage: MimeMessage?,
        isIncoming: Boolean,
        id: Identity
    ): ResultCompat<Rating>

    suspend fun getLog(): String
    fun printLog()
    fun loadOwnIdentities(callback: ResultCallback<List<Identity>>)
    fun setIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback)
    fun unsetIdentityFlag(identity: Identity, flags: Int, completedCallback: CompletedCallback)
    fun setIdentityFlag(identity: Identity, sync: Boolean)
    fun unsetIdentityFlag(identity: Identity, flags: Int)
    fun setFastPollingCallback(needsFastPollCallback: NeedsFastPollCallback)
    fun incomingMessageRating(message: MimeMessage): ResultCompat<Rating>
    fun incomingMessageRating(message: MimeMessage, callback: ResultCallback<Rating>)
    fun loadOutgoingMessageRatingAfterResetTrust(
        identity: Identity,
        from: Address,
        toAddresses: List<Address>,
        ccAddresses: List<Address>,
        bccAddresses: List<Address>
    ): ResultCompat<Rating>

    fun obtainLanguages(): Map<String, PlanckLanguage>
    fun generatePrivateKeyMessage(message: MimeMessage, fpr: String): Message?

    @Throws(pEpException::class)
    fun encryptMessage(result: foundation.pEp.jniadapter.Message): foundation.pEp.jniadapter.Message
    fun canEncrypt(address: String): Boolean
    fun importKey(key: ByteArray): Vector<Identity>
    fun importExtraKey(key: ByteArray): Vector<String>
    fun keyResetIdentity(ident: Identity, fpr: String?)
    fun keyResetUser(userId: String, fpr: String?)
    fun keyResetAllOwnKeys()
    suspend fun leaveDeviceGroup(): ResultCompat<Unit>
    suspend fun startSync()
    suspend fun stopSync()
    suspend fun isSyncRunning(): Boolean
    fun setSyncSendMessageCallback(callback: MessageToSendCallback)
    fun setSyncHandshakeCallback(callback: NotifyHandshakeCallback)
    fun disableSyncForAllIdentites()
    suspend fun syncReset()
    suspend fun updateSyncAccountsConfig()
    fun createGroup(
        groupIdentity: Identity,
        manager: Identity,
        members: Vector<Identity>
    )

    fun queryGroupMailManager(group: Identity): Identity
    fun queryGroupMailMembers(group: Identity): Vector<Identity>?
    fun joinGroupMail(group: Identity, member: Identity, manager: Identity)
    fun queryGroupMailManagerAndMembers(group: Identity): ResultCompat<Vector<Identity>>
    fun dissolveGroup(group: Identity, manager: Identity)
    fun inviteMemberToGroup(group: Identity, member: Identity)
    fun removeMemberFromGroup(group: Identity, member: Identity)
    fun groupRating(group: Identity, manager: Identity): Rating

    /**
     * isDeviceGrouped
     * Check if this device is in a planck device group.
     * @return true if in a group, false otherwise.
     */
    suspend fun isDeviceGrouped(): Boolean

    /**
     * getSignatureForText
     * Get signature String for a given input String
     * @param text String of which we want to get the signature.
     * @return [ResultCompat] Success(String result) on success, Failure on error.
     */
    fun getSignatureForText(text: String): ResultCompat<String>

    /**
     * verifySignature
     * Verify a string with a signature to see if current string is equal to the one the signature was created from.
     *
     * @param textToVerify String to verify
     * @param signature Signature string
     * @return [ResultCompat] Success(true) if match, Success(false) if no match, Failure on error.
     */
    fun verifySignature(textToVerify: String, signature: String): ResultCompat<Boolean>
    class KeyDetail(val fpr: String, val address: Address) {

        val username: String
            get() = address.personal
        val stringAddress: String
            get() = address.address
    }

    class DecryptResult {
        @JvmField
        var flags = -1
        @JvmField
        val msg: MimeMessage
        @JvmField
        val rating: Rating
        @JvmField
        val isFormerlyEncryptedReUploadedMessage: Boolean

        constructor(msg: MimeMessage, rating: Rating, flags: Int, isEncrypted: Boolean) {
            this.msg = msg
            this.rating = rating
            this.flags = flags
            isFormerlyEncryptedReUploadedMessage = isFormerlyEncryptedReUploadedMessage(isEncrypted)
        }

        @Deprecated("Legacy constructor to be removed with PEpProviderImpl")
        constructor(msg: MimeMessage, rating: Rating, flags: Int) {
            this.msg = msg
            this.rating = rating
            this.flags = flags
            isFormerlyEncryptedReUploadedMessage = false
        }

        private fun isFormerlyEncryptedReUploadedMessage(isEncrypted: Boolean): Boolean {
            return isEncrypted && rating.value >= Rating.pEpRatingUnreliable.value
        }
    }

    enum class ProtectionScope {
        ACCOUNT, MESSAGE
    }

    enum class TrustAction {
        TRUST, MISTRUST
    }

    interface Callback {
        fun onError(throwable: Throwable)
    }

    interface ResultCallback<Result> : Callback {
        fun onLoaded(result: Result)
    }

    abstract class SimpleResultCallback<Result> : ResultCallback<Result> {
        override fun onError(throwable: Throwable) {
            Timber.e(throwable)
        }
    }

    interface CompletedCallback : Callback {
        fun onComplete()
    }

    companion object {
        /**
         * If is outgoing any copy of the message encrypted (yellow, green, and un secure for some) it will be putted in this position,
         * if not, all copies will be unencrypted.
         */
        const val ENCRYPTED_MESSAGE_POSITION = 0
        const val PLANCK_OWN_USER_ID = "pEp_own_userId"

        const val TIMEOUT = (10 * 60 * 1000).toLong()
        const val PLANCK_ALWAYS_SECURE_TRUE = "yes"
        const val PLANCK_KEY_LIST_SEPARATOR = ","
        const val KEY_MISSING_ERROR_MESSAGE = "keyMissing"
        const val KEY_COULD_NOT_DECRYPT_MESSAGE = "couldNotDecrypt"
    }
}