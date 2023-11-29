package com.fsck.k9.ui.messageview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.MessagingListener
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.extensions.isMessageIncomplete
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.exceptions.KeyMissingException
import com.fsck.k9.ui.messageview.MessageViewState.DecryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.EncryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.ErrorDecodingMessage
import com.fsck.k9.ui.messageview.MessageViewState.ErrorDecryptingMessage
import com.fsck.k9.ui.messageview.MessageViewState.ErrorDecryptingMessageKeyMissing
import com.fsck.k9.ui.messageview.MessageViewState.ErrorDownloadingMessageNotFound
import com.fsck.k9.ui.messageview.MessageViewState.ErrorDownloadingNetworkError
import com.fsck.k9.ui.messageview.MessageViewState.ErrorLoadingMessage
import com.fsck.k9.ui.messageview.MessageViewState.Idle
import com.fsck.k9.ui.messageview.MessageViewState.MessageDecoded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageViewViewModel @Inject constructor(
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val infoExtractor: MessageViewInfoExtractor,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private lateinit var messageReference: MessageReference
    private lateinit var account: Account

    private val messageViewStateLiveData: MutableLiveData<MessageViewState> =
        MutableLiveData(Idle)
    val messageViewState: LiveData<MessageViewState> = messageViewStateLiveData
    private var moveToSuspiciousFolder = false

    fun initialize(messageReference: MessageReference) {
        this.messageReference = messageReference
        this.account = preferences.getAccount(messageReference.accountUuid)
    }

    fun downloadCompleteMessage() {
        viewModelScope.launch {
            withContext(dispatcherProvider.io()) {
                downloadMessageBody(true)
            }
        }
    }

    fun loadMessage() {
        viewModelScope.launch {
            loadMessageFromDatabase()
        }
    }

    private suspend fun loadMessageFromDatabase(): Result<LocalMessage?> =
        withContext(dispatcherProvider.io()) {
            kotlin.runCatching {
                controller.loadMessage(account, messageReference.folderName, messageReference.uid)
            }.onFailure {
                Timber.e(it)
                messageViewStateLiveData.postValue(ErrorLoadingMessage(it))
            }.onSuccess { message ->
                // probably bring here logic from the fragment
                message?.let {
                    messageLoaded(message)
                } ?: messageViewStateLiveData.postValue(ErrorLoadingMessage())
            }
        }

    private suspend fun messageLoaded(
        message: LocalMessage
    ) {
        val loadedState =
            if (message.hasToBeDecrypted()) EncryptedMessageLoaded(message)
            else DecryptedMessageLoaded(message, moveToSuspiciousFolder)
        withContext(dispatcherProvider.main()) {
            messageViewStateLiveData.value = loadedState
        }
        if (message.isMessageIncomplete()) {
            downloadMessageBody(false)
        } else if (message.hasToBeDecrypted()) {
            decryptMessage(message)
        } else {
            decodeMessage(message)
        }
    }

    private fun downloadMessageBody(complete: Boolean) {
        if (complete) {
            controller.loadMessageRemote(
                account,
                messageReference.folderName,
                messageReference.uid,
                downloadMessageListener
            )
        } else {
            controller.loadMessageRemotePartial(
                account,
                messageReference.folderName,
                messageReference.uid,
                downloadMessageListener
            )
        }
    }

    private val downloadMessageListener: MessagingListener = object : SimpleMessagingListener() {
        override fun loadMessageRemoteFinished(account: Account, folder: String, uid: String) {
            if (messageReference.equals(account.uuid, folder, uid)) {
                downloadMessageFinished()
            }
        }

        override fun loadMessageRemoteFailed(
            account: Account,
            folder: String,
            uid: String,
            t: Throwable
        ) {
            downloadMessageFailed(t)
        }
    }

    private fun downloadMessageFailed(t: Throwable) {
        messageViewStateLiveData.postValue(
            when (t) {
                is IllegalArgumentException ->
                    ErrorDownloadingMessageNotFound(t)

                else ->
                    ErrorDownloadingNetworkError(t)
            }
        )
    }

    private fun downloadMessageFinished() {
        loadMessage()
    }

    private fun decodeMessage(message: LocalMessage) {
        kotlin.runCatching {
            infoExtractor.extractMessageForView(
                message,
                null,
                account.isOpenPgpProviderConfigured
            )
        }.onFailure {
            Timber.e(it)
            messageViewStateLiveData.postValue(
                ErrorDecodingMessage(createErrorStateMessageViewInfo(message), it)
            )
        }.onSuccess {
            messageViewStateLiveData.postValue(MessageDecoded(it))
        }
    }

    private fun createErrorStateMessageViewInfo(localMessage: LocalMessage): MessageViewInfo {
        val isMessageIncomplete: Boolean = !localMessage.isSet(Flag.X_DOWNLOADED_FULL)
        return MessageViewInfo.createWithErrorState(localMessage, isMessageIncomplete)
    }

    private suspend fun decryptMessage(message: LocalMessage) {
        planckProvider.decryptMessage(message, account)
            .onSuccess { decryptResult ->
                messageDecrypted(decryptResult, message)
            }.onFailure { throwable ->
                messageDecryptFailed(throwable)
            }
    }

    private fun messageDecryptFailed(throwable: Throwable) {
        messageViewStateLiveData.postValue(
            when (throwable) {
                is KeyMissingException ->
                    ErrorDecryptingMessageKeyMissing

                else ->
                    ErrorDecryptingMessage(throwable)
            }
        )
    }

    private fun messageDecrypted(
        decryptResult: PlanckProvider.DecryptResult,
        message: LocalMessage
    ) {
        kotlin.runCatching {
            val decryptedMessage: MimeMessage = decryptResult.msg
            // sync UID so we know our mail...
            decryptedMessage.uid = message.uid
            // Store the updated message locally
            val folder = message.folder
            folder.storeSmallMessage(decryptedMessage) {}
        }.onSuccess {
            moveToSuspiciousFolder = PlanckUtils.isRatingDangerous(decryptResult.rating)
            messageViewStateLiveData.postValue(
                DecryptedMessageLoaded(it, moveToSuspiciousFolder)
            )
        }.onFailure {
            messageViewStateLiveData.postValue(ErrorDecryptingMessage(it))
        }
    }
}