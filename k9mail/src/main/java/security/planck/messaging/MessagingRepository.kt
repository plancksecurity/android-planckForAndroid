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
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.exceptions.KeyMissingException
import com.fsck.k9.planck.infrastructure.extensions.flatMapSuspend
import com.fsck.k9.ui.messageview.MessageViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val infoExtractor: MessageViewInfoExtractor,
    private val appIoScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun loadMessage(
        account: Account,
        messageReference: MessageReference,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) = withContext(dispatcherProvider.io()) {
        updateFlow.value = MessageViewState.Loading
        loadMessageFromDatabase(
            account, messageReference, updateFlow
        ).join()
    }

    private fun loadMessageFromDatabase(
        account: Account,
        messageReference: MessageReference,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) = appIoScope.launch {
        kotlin.runCatching {
            controller.loadMessage(account, messageReference.folderName, messageReference.uid)
        }.onFailure {
            Timber.e(it)
            updateFlow.value = MessageViewState.ErrorLoadingMessage(it)
        }.onSuccess { message ->
            message?.let {
                messageLoaded(account, message, updateFlow)
            } ?: let { updateFlow.value = MessageViewState.ErrorLoadingMessage() }
        }
    }

    private suspend fun messageLoaded(
        account: Account,
        message: LocalMessage,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        message.recoverRating()
        val loadedState = if (!message.hasToBeDecrypted()) {
            MessageViewState.DecryptedMessageLoaded(message)
        } else {
            MessageViewState.EncryptedMessageLoaded(message)
        }

        withContext(dispatcherProvider.main()) {
            updateFlow.value = loadedState
        }
        if (message.isMessageIncomplete()) {
            downloadMessageBody(account, message.makeMessageReference(), false, updateFlow)
        } else if (message.hasToBeDecrypted()) {
            decryptMessage(account, message, updateFlow)
        } else {
            decodeMessage(account, message, updateFlow)
        }
    }

    private fun decodeMessage(
        account: Account,
        message: LocalMessage,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        kotlin.runCatching {
            infoExtractor.extractMessageForView(
                message,
                null,
                account.isOpenPgpProviderConfigured
            )
        }.onFailure {
            Timber.e(it)
            updateFlow.value =
                MessageViewState.ErrorDecodingMessage(createErrorStateMessageViewInfo(message), it)
        }.onSuccess {
            updateFlow.value = MessageViewState.MessageDecoded(it)
        }
    }

    private fun downloadMessageBody(
        account: Account,
        messageReference: MessageReference,
        complete: Boolean,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        if (complete) {
            controller.loadMessageRemote(
                account,
                messageReference.folderName,
                messageReference.uid,
                getDownloadMessageListener(messageReference, updateFlow)
            )
        } else {
            controller.loadMessageRemotePartial(
                account,
                messageReference.folderName,
                messageReference.uid,
                getDownloadMessageListener(messageReference, updateFlow)
            )
        }
    }

    suspend fun downloadCompleteMessage(
        account: Account,
        messageReference: MessageReference,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) = withContext(dispatcherProvider.io()) {
        appIoScope.launch {
            downloadMessageBody(account, messageReference, true, updateFlow)
        }.join()
    }

    private fun getDownloadMessageListener(
        messageReference: MessageReference,
        updateFlow: MutableStateFlow<MessageViewState>,
    ): MessagingListener =
        object : SimpleMessagingListener() {
            override fun loadMessageRemoteFinished(account: Account, folder: String, uid: String) {
                if (messageReference.equals(account.uuid, folder, uid)) {
                    downloadMessageFinished(account, messageReference, updateFlow)
                }
            }

            override fun loadMessageRemoteFailed(
                account: Account,
                folder: String,
                uid: String,
                t: Throwable
            ) {
                downloadMessageFailed(t, updateFlow)
            }
        }

    private fun downloadMessageFailed(
        t: Throwable,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        updateFlow.value = when (t) {
            is IllegalArgumentException ->
                MessageViewState.ErrorDownloadingMessageNotFound(t)

            else ->
                MessageViewState.ErrorDownloadingNetworkError(t)
        }
    }

    private fun downloadMessageFinished(
        account: Account,
        messageReference: MessageReference,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) {
        loadMessageFromDatabase(account, messageReference, updateFlow)
    }

    private fun createErrorStateMessageViewInfo(localMessage: LocalMessage): MessageViewInfo {
        val isMessageIncomplete: Boolean = !localMessage.isSet(Flag.X_DOWNLOADED_FULL)
        return MessageViewInfo.createWithErrorState(localMessage, isMessageIncomplete)
    }

    private suspend fun decryptMessage(
        account: Account,
        message: LocalMessage,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) =
        planckProvider.decryptMessage(message, account)
            .onFailure { throwable ->
                messageDecryptFailed(throwable, updateFlow)
            }.flatMapSuspend { decryptResult ->
                messageDecrypted(decryptResult, account, message, updateFlow)
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
        account: Account,
        message: LocalMessage,
        updateFlow: MutableStateFlow<MessageViewState>,
    ) = kotlin.runCatching {
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
                moveMessageToSuspiciousFolder(account, message.makeMessageReference())
                updateFlow.value = MessageViewState.MessageMovedToSuspiciousFolder
            } else {
                loadMessageFromDatabase(account, message.makeMessageReference(), updateFlow)
            }
        }
    }.onFailure {
        updateFlow.value = MessageViewState.ErrorDecryptingMessage(it)
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