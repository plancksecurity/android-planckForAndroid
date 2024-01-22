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
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.livedata.Event
import com.fsck.k9.ui.messageview.MessageViewEffect.ErrorLoadingMessage
import com.fsck.k9.ui.messageview.MessageViewEffect.NoEffect
import com.fsck.k9.ui.messageview.MessageViewState.DecryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.EncryptedMessageLoaded
import com.fsck.k9.ui.messageview.MessageViewState.Idle
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.messaging.MessagingRepository
import javax.inject.Inject

@HiltViewModel
class MessageViewViewModel(
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val planckProvider: PlanckProvider,
    private val messagingRepository: MessagingRepository,
    private val messageViewUpdate: MessageViewUpdate,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @Inject
    constructor(
        preferences: Preferences,
        controller: MessagingController,
        planckProvider: PlanckProvider,
        messagingRepository: MessagingRepository,
        dispatcherProvider: DispatcherProvider,
    ) : this(
        preferences,
        controller,
        planckProvider,
        messagingRepository,
        MessageViewUpdate(),
        dispatcherProvider
    )

    val messageReference: MessageReference
        get() = messageViewUpdate.messageReference
    val account: Account
        get() = messageViewUpdate.account
    lateinit var message: LocalMessage
        private set

    private val messageViewStateLiveData: MutableLiveData<MessageViewState> =
        MutableLiveData(Idle)
    val messageViewState: LiveData<MessageViewState> = messageViewStateLiveData
    private val messageViewEffectLiveData: MutableLiveData<Event<MessageViewEffect>> =
        MutableLiveData(Event(NoEffect))
    val messageViewEffect: LiveData<Event<MessageViewEffect>> = messageViewEffectLiveData
    private val allowHandshakeSenderLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val allowHandshakeSender: LiveData<Event<Boolean>> = allowHandshakeSenderLiveData

    private val flaggedToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val flaggedToggled: LiveData<Event<Boolean>> = flaggedToggledLiveData

    private val readToggledLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))
    val readToggled: LiveData<Event<Boolean>> = readToggledLiveData

    val messageSubject: String?
        get() = if (::message.isInitialized) message.subject else null
    val messageFrom: Array<Address>
        get() = message.from

    init {
        messageViewUpdate.stateFlow.onEach { state ->
            if (state is EncryptedMessageLoaded) message = state.message
            else if (state is DecryptedMessageLoaded) {
                message = state.message
                checkCanHandshakeSender()
            }
            messageViewStateLiveData.value = state
        }.launchIn(viewModelScope)
        messageViewUpdate.effectFlow.onEach { effect ->
            messageViewEffectLiveData.value = Event(effect)
        }.launchIn(viewModelScope)
    }

    fun initialize(messageReferenceString: String?) {
        kotlin.runCatching {
            messageViewUpdate.messageReference = MessageReference.parse(messageReferenceString!!)
                ?: error("null reference")
            messageViewUpdate.account = preferences.getAccount(messageReference.accountUuid)
                ?: error("account was removed")
        }.onFailure {
            messageViewUpdate.effectFlow.value = ErrorLoadingMessage(it, true)
        }
    }

    fun hasMessage(): Boolean = ::message.isInitialized

    fun downloadCompleteMessage() {
        viewModelScope.launch {
            messagingRepository.downloadCompleteMessage(
                messageViewUpdate
            )
        }
    }

    fun loadMessage() {
        viewModelScope.launch {
            messagingRepository.loadMessage(messageViewUpdate)
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

    private suspend fun getSenderRating(message: LocalMessage): Rating =
        planckProvider.getRating(message.from.first()).getOrDefault(Rating.pEpRatingUndefined)

    private fun messageConditionsForSenderKeyReset(message: LocalMessage): Boolean =
        !message.hasToBeDecrypted()
                && message.from != null // sender not null
                && message.from.size == 1 // only one sender
                && preferences.availableAccounts.none {
            it.email.equals(message.from.first().address, true)
        } // sender not one of my own accounts
                && message.getRecipients(RecipientType.TO).size == 1 // only one recipient in TO
                && message.getRecipients(RecipientType.CC)
            .isNullOrEmpty() // no recipients in CC
                && message.getRecipients(RecipientType.BCC)
            .isNullOrEmpty() // no recipients in BCC

    private fun ratingConditionsForSenderKeyReset(
        messageRating: Rating
    ): Boolean {
        return !PlanckUtils.isRatingUnsecure(messageRating) || (messageRating == Rating.pEpRatingMistrust)
    }

    fun isMessageValidForHandshake(): Boolean =
        ::message.isInitialized && message.isValidForHandshake()

    fun isMessageSMime(): Boolean = ::message.isInitialized && message.isSet(Flag.X_SMIME_SIGNED)
    fun isMessageFlagged(): Boolean = ::message.isInitialized && message.isSet(Flag.FLAGGED)
    fun isMessageRead(): Boolean = ::message.isInitialized && message.isSet(Flag.SEEN)
    fun makeMessageReference(): MessageReference = message.makeMessageReference()
    fun getMessageRecipients(recipientType: RecipientType): Array<Address> =
        message.getRecipients(recipientType)

    fun extractMessageRating(): Rating = PlanckUtils.extractRating(message)
}