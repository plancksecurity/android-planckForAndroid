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
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.extensions.isValidForHandshake
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.livedata.Event
import com.fsck.k9.ui.messageview.MessageViewState.DecryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.EncryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.ErrorLoadingMessage
import com.fsck.k9.ui.messageview.MessageViewState.Idle
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.messaging.MessagingRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageViewViewModel @Inject constructor(
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val infoExtractor: MessageViewInfoExtractor,
    private val context: Application,
    private val messagingRepository: MessagingRepository,
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

    private val flaggedToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val flaggedToggled: LiveData<Event<Boolean>> = flaggedToggledLiveData

    private val readToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val readToggled: LiveData<Event<Boolean>> = readToggledLiveData

    private val resetPartnerKeyStateLd: MutableLiveData<BackgroundTaskDialogView.State> =
        MutableLiveData(BackgroundTaskDialogView.State.CONFIRMATION)
    val resetPartnerKeyState: LiveData<BackgroundTaskDialogView.State> = resetPartnerKeyStateLd

    private val updateFlow: MutableStateFlow<MessageViewState> = MutableStateFlow(Idle)

    val messageSubject: String?
        get() = if (::message.isInitialized) message.subject else null
    val messageFrom: Array<Address>
        get() = message.from

    init {
        updateFlow.onEach { state ->
            if (state is EncryptedMessageLoaded) message = state.message
            else if (state is DecryptedMessageLoaded) message = state.message
            messageViewStateLiveData.value = state
        }.launchIn(viewModelScope)
    }

    fun initialize(messageReferenceString: String?) {
        kotlin.runCatching {
            this.messageReference = MessageReference.parse(messageReferenceString!!)
                ?: error("null reference")
            this.account = preferences.getAccount(messageReference.accountUuid)
                ?: error("account was removed")
        }.onFailure {
            updateFlow.value = ErrorLoadingMessage(it, true)
            //messageViewStateLiveData.value = ErrorLoadingMessage(it, true)
        }
    }

    fun hasMessage(): Boolean = ::message.isInitialized

    fun downloadCompleteMessage() {
        viewModelScope.launch {
            messagingRepository.downloadMessageBody(account,messageReference, true, updateFlow)
        }
    }

    fun loadMessage() {
        viewModelScope.launch {
            messagingRepository.loadMessage(account, messageReference, updateFlow)
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
                messagingRepository.loadMessage(account, messageReference, updateFlow)
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

    fun partnerKeyResetFinished() {
        resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.CONFIRMATION
    }

    fun isMessageValidForHandshake(): Boolean = ::message.isInitialized && message.isValidForHandshake()
    fun isMessageSMime(): Boolean = ::message.isInitialized && message.isSet(Flag.X_SMIME_SIGNED)
    fun isMessageFlagged(): Boolean = ::message.isInitialized && message.isSet(Flag.FLAGGED)
    fun isMessageRead(): Boolean = ::message.isInitialized && message.isSet(Flag.SEEN)
    fun makeMessageReference(): MessageReference = message.makeMessageReference()
    fun getMessageRecipients(recipientType: RecipientType): Array<Address> = message.getRecipients(recipientType)

    fun extractMessageRating(): Rating = PlanckUtils.extractRating(message)
}