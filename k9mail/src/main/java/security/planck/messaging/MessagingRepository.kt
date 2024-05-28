package security.planck.messaging

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.MessagingListener
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.extensions.isMessageIncomplete
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.message.MessageBuilder
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.exceptions.KeyMissingException
import com.fsck.k9.planck.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.preferences.Storage
import com.fsck.k9.ui.messageview.MessageViewEffect
import com.fsck.k9.ui.messageview.MessageViewState
import com.fsck.k9.ui.messageview.MessageViewUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val storage: Storage,
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val infoExtractor: MessageViewInfoExtractor,
    private val appIoScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun buildAndSendMessage(
        messageBuilder: MessageBuilder,
        account: Account
    ) = withContext(dispatcherProvider.io()) {
        try {
            val mimeMessage = messageBuilder.buildSync()
            controller.sendMessage(account, mimeMessage, null)
        } catch (e: Exception) {
            Timber.e(e, "Error building message in background")
        }
    }

    suspend fun loadMessage(
        messageViewUpdate: MessageViewUpdate,
    ) = withContext(dispatcherProvider.io()) {
        messageViewUpdate.stateFlow.value = MessageViewState.Loading
        messageViewUpdate.effectFlow.value = MessageViewEffect.NoEffect
        loadMessageFromDatabase(
            messageViewUpdate
        ).join()
    }

    private fun loadMessageFromDatabase(
        messageViewUpdate: MessageViewUpdate,
    ) = appIoScope.launch {
        with(messageViewUpdate) {
            kotlin.runCatching {
                controller.loadMessage(account, messageReference.folderName, messageReference.uid)
            }.onFailure {
                Timber.e(it)
                effectFlow.value = MessageViewEffect.ErrorLoadingMessage(it)
            }.onSuccess { message ->
                message?.let {
                    messageLoaded(message, messageViewUpdate)
                } ?: let { effectFlow.value = MessageViewEffect.ErrorLoadingMessage() }
            }
        }
    }

    private suspend fun messageLoaded(
        message: LocalMessage,
        messageViewUpdate: MessageViewUpdate,
    ) {
        message.recoverRating()
        val loadedState = if (message.hasToBeDecrypted()) {
            MessageViewState.EncryptedMessageLoaded(message)
        } else {
            storage.edit().removeCouldNotDecryptMessageId(message.messageId)
            MessageViewState.DecryptedMessageLoaded(message)
        }

        with(messageViewUpdate) {
            withContext(dispatcherProvider.main()) {
                stateFlow.value = loadedState
            }
            if (message.isMessageIncomplete()) {
                downloadMessageBody(messageViewUpdate, false)
            } else if (message.hasToBeDecrypted()) {
                decryptMessage(message, messageViewUpdate)
            } else {
                decodeMessage(message, messageViewUpdate)
            }
        }
    }

    private fun decodeMessage(
        message: LocalMessage,
        messageViewUpdate: MessageViewUpdate,
    ) {
        with(messageViewUpdate) {
            kotlin.runCatching {
                infoExtractor.extractMessageForView(
                    message,
                    null,
                    account.isOpenPgpProviderConfigured
                )
            }.onFailure {
                Timber.e(it)
                stateFlow.value =
                    MessageViewState.ErrorDecodingMessage(
                        createErrorStateMessageViewInfo(message),
                        it
                    )
            }.onSuccess {
                stateFlow.value = MessageViewState.MessageDecoded(it)
            }
        }
    }

    private fun downloadMessageBody(
        messageViewUpdate: MessageViewUpdate,
        complete: Boolean,
    ) {
        with(messageViewUpdate) {
            if (complete) {
                controller.loadMessageRemote(
                    account,
                    messageReference.folderName,
                    messageReference.uid,
                    getDownloadMessageListener(messageViewUpdate)
                )
            } else {
                controller.loadMessageRemotePartial(
                    account,
                    messageReference.folderName,
                    messageReference.uid,
                    getDownloadMessageListener(messageViewUpdate)
                )
            }
        }
    }

    suspend fun downloadCompleteMessage(
        messageViewUpdate: MessageViewUpdate,
    ) = withContext(dispatcherProvider.io()) {
        messageViewUpdate.stateFlow.value = MessageViewState.Loading
        messageViewUpdate.effectFlow.value = MessageViewEffect.NoEffect
        appIoScope.launch {
            downloadMessageBody(messageViewUpdate, true)
        }.join()
    }

    private fun getDownloadMessageListener(
        messageViewUpdate: MessageViewUpdate,
    ): MessagingListener =
        object : SimpleMessagingListener() {
            override fun loadMessageRemoteFinished(account: Account, folder: String, uid: String) {
                if (messageViewUpdate.messageReference.equals(account.uuid, folder, uid)) {
                    downloadMessageFinished(messageViewUpdate)
                }
            }

            override fun loadMessageRemoteFailed(
                account: Account,
                folder: String,
                uid: String,
                t: Throwable
            ) {
                downloadMessageFailed(t, messageViewUpdate)
            }
        }

    private fun downloadMessageFailed(
        t: Throwable,
        messageViewUpdate: MessageViewUpdate,
    ) {
        messageViewUpdate.effectFlow.value = when (t) {
            is IllegalArgumentException ->
                MessageViewEffect.ErrorDownloadingMessageNotFound(t)

            else ->
                MessageViewEffect.ErrorDownloadingNetworkError(t)
        }
    }

    private fun downloadMessageFinished(
        messageViewUpdate: MessageViewUpdate,
    ) {
        loadMessageFromDatabase(messageViewUpdate)
    }

    private fun createErrorStateMessageViewInfo(localMessage: LocalMessage): MessageViewInfo {
        val isMessageIncomplete: Boolean = !localMessage.isSet(Flag.X_DOWNLOADED_FULL)
        return MessageViewInfo.createWithErrorState(localMessage, isMessageIncomplete)
    }

    private suspend fun decryptMessage(
        message: LocalMessage,
        messageViewUpdate: MessageViewUpdate,
    ) = with(messageViewUpdate) {
        planckProvider.decryptMessage(message, account)
            .onFailure { throwable ->
                messageDecryptFailed(throwable, messageViewUpdate.stateFlow)
            }.flatMapSuspend { decryptResult ->
                messageDecrypted(decryptResult, message, messageViewUpdate)
            }
    }


    private fun messageDecryptFailed(
        throwable: Throwable,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        updateFlow.value = when (throwable) {
            is KeyMissingException ->
                MessageViewState.ErrorDecryptingMessageKeyMissing

            else ->
                MessageViewState.ErrorDecryptingMessage(throwable)
        }
    }

    private fun messageDecrypted(
        decryptResult: PlanckProvider.DecryptResult,
        message: LocalMessage,
        messageViewUpdate: MessageViewUpdate,
    ) = with(messageViewUpdate) {
        kotlin.runCatching {
            val decryptedMessage: MimeMessage = decryptResult.msg
            // sync UID so we know our mail...
            decryptedMessage.uid = message.uid
            // set rating in header not to lose it
            decryptedMessage.setHeader(
                MimeHeader.HEADER_PEP_RATING,
                PlanckUtils.ratingToString(decryptResult.rating)
            )
            // Store the updated message locally
            val folder = message.folder
            folder.storeSmallMessage(decryptedMessage) {
                val moveToSuspiciousFolder = PlanckUtils.isRatingDangerous(decryptResult.rating)
                if (moveToSuspiciousFolder) {
                    moveMessageToSuspiciousFolder(account, messageReference)
                    effectFlow.value = MessageViewEffect.MessageMovedToSuspiciousFolder
                } else {
                    loadMessageFromDatabase(messageViewUpdate)
                }
            }
        }.onFailure {
            stateFlow.value = MessageViewState.ErrorDecryptingMessage(it)
        }
    }

    private fun LocalMessage.recoverRating() {
        // recover pEpRating from db, if is null,
        // then we take the one in the header and store it
        planckRating ?: let {
            planckRating = PlanckUtils.extractRating(this)
        }
    }

    private fun moveMessageToSuspiciousFolder(
        account: Account,
        messageReference: MessageReference,
    ) {
        controller.moveMessage(
            account,
            messageReference.folderName,
            messageReference,
            account.planckSuspiciousFolderName
        )
    }
}