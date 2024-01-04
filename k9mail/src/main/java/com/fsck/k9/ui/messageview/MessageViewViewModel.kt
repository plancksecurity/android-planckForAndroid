package com.fsck.k9.ui.messageview

import android.app.Application
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
import com.fsck.k9.extensions.isValidForHandshake
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
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
import com.fsck.k9.planck.infrastructure.livedata.Event
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
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.dialog.BackgroundTaskDialogView
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageViewViewModel @Inject constructor(
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val infoExtractor: MessageViewInfoExtractor,
    private val context: Application,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    lateinit var messageReference: MessageReference
        private set
    lateinit var account: Account
        private set
    lateinit var message: LocalMessage
        private set

    private val messageViewStateLiveData: MutableLiveData<MessageViewState> =
        MutableLiveData(Idle)
    val messageViewState: LiveData<MessageViewState> = messageViewStateLiveData
    private val allowHandshakeSenderLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val allowHandshakeSender: LiveData<Event<Boolean>> = allowHandshakeSenderLiveData
    private var moveToSuspiciousFolder = false

    private val flaggedToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val flaggedToggled: LiveData<Event<Boolean>> = flaggedToggledLiveData

    private val readToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val readToggled: LiveData<Event<Boolean>> = readToggledLiveData

    private val resetPartnerKeyStateLd: MutableLiveData<BackgroundTaskDialogView.State> =
        MutableLiveData(BackgroundTaskDialogView.State.CONFIRMATION)
    val resetPartnerKeyState: LiveData<BackgroundTaskDialogView.State> = resetPartnerKeyStateLd

    fun initialize(messageReferenceString: String?) {
        kotlin.runCatching {
            this.messageReference = MessageReference.parse(messageReferenceString!!)
                ?: error("null reference")
            this.account = preferences.getAccount(messageReference.accountUuid)
                ?: error("account was removed")
        }.onFailure {
            messageViewStateLiveData.value = ErrorLoadingMessage(it, true)
        }
    }

    fun hasMessage(): Boolean = ::message.isInitialized

    fun downloadCompleteMessage() {
        viewModelScope.launch {
            downloadMessageBody(true)
        }
    }

    fun loadMessage() {
        viewModelScope.launch {
            loadMessageFromDatabase()
        }
    }

    fun canResetSenderKeys(): Boolean {
        return ::message.isInitialized
                && (message.account?.isPlanckPrivacyProtected ?: false)
                && messageConditionsForSenderKeyReset(message)
                && ratingConditionsForSenderKeyReset(message.planckRating)
    }

    private suspend fun checkCanHandshakeSender() {
        (message.isValidForHandshake()
                && PlanckUtils.isRatingReliable(getSenderRating(message))).also {
            allowHandshakeSenderLiveData.postValue(Event(it))
        }
    }

    fun toggleFlagged() {
        if (::message.isInitialized) {
            viewModelScope.launch {
                withContext(dispatcherProvider.io()) {
                    val newState = !message.isSet(Flag.FLAGGED)
                    controller.setFlag(
                        account,
                        message.folder.name,
                        listOf(message),
                        Flag.FLAGGED,
                        newState
                    )
                }
                flaggedToggledLiveData.value = Event(true)
            }
        }
    }

    fun toggleRead() {
        if (::message.isInitialized) {
            viewModelScope.launch {
                withContext(dispatcherProvider.io()) {
                    controller.setFlag(
                        account,
                        message.folder.name,
                        listOf(message),
                        Flag.SEEN,
                        !message.isSet(Flag.SEEN)
                    )
                }
                readToggledLiveData.value = Event(true)
            }
        }
    }

    fun resetPlanckData() {
        viewModelScope.launch {
            resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.LOADING
            kotlin.runCatching {
                val resetIdentity = PlanckUtils.createIdentity(message.from.first(), context)
                withContext(dispatcherProvider.planckDispatcher()) {
                    planckProvider.keyResetIdentity(resetIdentity, null)
                }
            }.onSuccess {
                loadMessageFromDatabase()
                resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.SUCCESS
            }.onFailure {
                Timber.e(it)
                resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }

    private suspend fun getSenderRating(message: LocalMessage): Rating =
        planckProvider.getRating(message.from.first()).getOrDefault(Rating.pEpRatingUndefined)

    private fun messageConditionsForSenderKeyReset(message: LocalMessage): Boolean =
        !message.hasToBeDecrypted()
                && message.from != null // sender not null
                && message.from.size == 1 // only one sender
                && preferences.availableAccounts.none { it.email == message.from.first().address } // sender not one of my own accounts
                && message.getRecipients(Message.RecipientType.TO).size == 1 // only one recipient in TO
                && message.getRecipients(Message.RecipientType.CC)
            .isNullOrEmpty() // no recipients in CC
                && message.getRecipients(Message.RecipientType.BCC)
            .isNullOrEmpty() // no recipients in BCC

    private fun ratingConditionsForSenderKeyReset(
        messageRating: Rating
    ): Boolean {
        return !PlanckUtils.isRatingUnsecure(messageRating) || (messageRating == Rating.pEpRatingMistrust)
    }

    private suspend fun loadMessageFromDatabase(): Result<LocalMessage?> =
        withContext(dispatcherProvider.io()) {
            kotlin.runCatching {
                controller.loadMessage(account, messageReference.folderName, messageReference.uid)
            }.onFailure {
                Timber.e(it)
                messageViewStateLiveData.postValue(ErrorLoadingMessage(it))
            }.onSuccess { message ->
                message?.let {
                    messageLoaded(message)
                } ?: messageViewStateLiveData.postValue(ErrorLoadingMessage())
            }
        }

    private suspend fun messageLoaded(
        message: LocalMessage
    ) {
        this.message = message
        message.recoverRating()
        val loadedState = if (!message.hasToBeDecrypted()) {
            checkCanHandshakeSender()
            DecryptedMessageLoaded(message, moveToSuspiciousFolder)
        } else {
            EncryptedMessageLoaded(message)
        }
        withContext(dispatcherProvider.main()) {
            messageViewStateLiveData.value = loadedState
        }
        if (message.isMessageIncomplete()) {
            downloadMessageBody(false)
        } else if (message.hasToBeDecrypted()) {
            decryptMessage(message)
        } else if (!moveToSuspiciousFolder) {
            decodeMessage(message)
        }
    }

    private suspend fun downloadMessageBody(complete: Boolean) =
        withContext(dispatcherProvider.io()) {
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

    private suspend fun decryptMessage(message: LocalMessage) =
        planckProvider.decryptMessage(message, account)
            .onFailure { throwable ->
                messageDecryptFailed(throwable)
            }.flatMapSuspend { decryptResult ->
                messageDecrypted(decryptResult, message)
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
    ) = kotlin.runCatching {
        val decryptedMessage: MimeMessage = decryptResult.msg
        // sync UID so we know our mail...
        decryptedMessage.uid = message.uid
        // set rating in header not to lose it
        decryptedMessage.setHeader(MimeHeader.HEADER_PEP_RATING, PlanckUtils.ratingToString(decryptResult.rating))
        // Store the updated message locally
        val folder = message.folder
        folder.storeSmallMessage(decryptedMessage) {
            moveToSuspiciousFolder = PlanckUtils.isRatingDangerous(decryptResult.rating)
            loadMessage()
        }
    }.onFailure {
        messageViewStateLiveData.postValue(ErrorDecryptingMessage(it))
    }

    private fun LocalMessage.recoverRating() {
        // recover pEpRating from db, if is null,
        // then we take the one in the header and store it
        planckRating ?: let {
            planckRating = PlanckUtils.extractRating(this)
        }
    }

    fun partnerKeyResetFinished() {
        resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.CONFIRMATION
    }
}