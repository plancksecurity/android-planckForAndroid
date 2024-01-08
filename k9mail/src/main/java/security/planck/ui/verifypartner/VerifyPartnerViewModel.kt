package security.planck.ui.verifypartner

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val DEFAULT_TRUSTWORDS_LANGUAGE = "en"

/**
 * VerifyPartnerViewModel
 *
 * [ViewModel] for [VerifyPartnerFragment].
 */
@HiltViewModel
class VerifyPartnerViewModel @Inject
/**
 * Constructor
 *
 * Creates an istance of [VerifyPartnerViewModel]
 *
 * @param context [Application] The application
 * @param preferences [Preferences] to retrieve accounts
 * @param controller [MessagingController] to do message-related operations
 * @param identityMapper [PlanckIdentityMapper] to map and update identities
 * @param planckProvider [PlanckProvider] to do planck operations
 * @param cache [PlanckUIArtefactCache] to retrieve cached identities
 * @param dispatcherProvider [DispatcherProvider] to provide coroutine dispatchers
 */
constructor(
    private val context: Application,
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val identityMapper: PlanckIdentityMapper,
    private val planckProvider: PlanckProvider,
    private val cache: PlanckUIArtefactCache,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private lateinit var sender: Address
    private lateinit var myself: Identity
    private var messageReference: MessageReference? = null
    private var isMessageIncoming = false
    private lateinit var localMessage: LocalMessage
    private lateinit var partner: PlanckIdentity

    private var currentRating: Rating? = null
    private val stateLiveData: MutableLiveData<VerifyPartnerState> =
        MutableLiveData(VerifyPartnerState.Idle)

    /**
     * @property state
     *
     * [VerifyPartnerState] that can be observed by the view to get updates on the model state.
     */
    val state: LiveData<VerifyPartnerState> = stateLiveData

    private var result = mutableMapOf<String, Any?>()

    private var trustwordsLanguage = getInitialTrustwordsLanguage()
    private var shortTrustWords = true
    private var latestTrust = false
    private val partnerName: String = cache.recipients.first().address
    private val myselfName: String
        get() = myself.address

    /**
     * initialize
     *
     * Initialize this instance of [VerifyPartnerViewModel].
     *
     * @param sender Mail address of the sender of the message
     * @param myself My own mail address
     * @param messageReference that represents the message to work with
     * @param isMessageIncoming whether the message direction is incoming or outgoing.
     */
    fun initialize(
        sender: String,
        myself: String,
        messageReference: MessageReference?,
        isMessageIncoming: Boolean
    ) {
        viewModelScope.launch {
            stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
            populateData(sender, myself, messageReference, isMessageIncoming)
                .flatMapSuspend { loadMessage() }
                .onSuccessSuspend {
                    it?.let { message ->
                        if (message.isSet(Flag.DELETED)) {
                            stateLiveData.value = VerifyPartnerState.DeletedMessage
                        } else {
                            localMessage = message
                            currentRating = message.planckRating
                            getHandshakeData()
                        }
                    } ?: getHandshakeData()
                }.onFailure {
                    // display error
                    stateLiveData.value = VerifyPartnerState.ErrorLoadingMessage
                }
        }
    }

    /**
     * finish
     *
     * Should be called when closing the screen.
     * It delivers a result with current rating if not yet delivered.
     */
    fun finish() {
        stateLiveData.value = VerifyPartnerState.Finish(
            if (result.isEmpty()) {  // deliver result if not yet delivered
                mapOf(VerifyPartnerFragment.RESULT_KEY_RATING to null)
            } else emptyMap()
        )
    }

    private fun startHandshake(trust: Boolean) {
        latestTrust = trust
    }

    private fun performHandshake() { // actually trust or mistrust partner
        viewModelScope.launch {
            setHandshakeProgress()
            changePartnerTrust().flatMapSuspend {
                refreshRating()
            }.onSuccessSuspend {
                setHandshakeDone()
            }.onFailure {
                // display error
                setHandshakeError()
            }
        }
    }

    private fun setHandshakeError() {
        stateLiveData.value = if (latestTrust) {
            VerifyPartnerState.ErrorTrusting(partnerName)
        } else {
            VerifyPartnerState.ErrorMistrusting(partnerName)
        }
    }

    private fun setHandshakeDone() {
        result[VerifyPartnerFragment.RESULT_KEY_RATING] = currentRating?.toString()
        stateLiveData.value = if (latestTrust) {
            VerifyPartnerState.TrustDone(partnerName, result)
        } else {
            VerifyPartnerState.MistrustDone(partnerName, result)
        }
    }

    private fun setHandshakeProgress() {
        stateLiveData.value = if (latestTrust) {
            VerifyPartnerState.TrustProgress(partnerName)
        } else {
            VerifyPartnerState.MistrustProgress(partnerName)
        }
    }

    private suspend fun updateIdentity() = withContext(dispatcherProvider.planckDispatcher()) {
        ResultCompat.of { partner = identityMapper.updateAndMapRecipient(cache.recipients.first()) }
    }

    private suspend fun onRatingChanged(rating: Rating): ResultCompat<Unit> {
        currentRating = rating
        return if (isMessageIncoming) {
            saveRatingToMessage(rating)
        } else {
            ResultCompat.success(Unit)
        }
    }

    private suspend fun saveRatingToMessage(
        rating: Rating
    ): ResultCompat<Unit> = withContext(dispatcherProvider.io()) {
        ResultCompat.of {
            localMessage.planckRating = rating
        }
    }

    private suspend fun changePartnerTrust(): ResultCompat<Unit> =
        withContext(dispatcherProvider.planckDispatcher()) {
            ResultCompat.of {
                if (latestTrust) planckProvider.trustPersonalKey(partner)
                else planckProvider.keyMistrusted(partner)
            }
        }

    private suspend fun refreshRating(): ResultCompat<Unit> =
        retrieveNewRating().flatMapSuspend { onRatingChanged(it) }

    private suspend fun retrieveNewRating(): ResultCompat<Rating> =
        withContext(dispatcherProvider.planckDispatcher()) {
            if (isMessageIncoming) {
                getIncomingMessageRating()
            } else {
                getOutgoingMessageRating()
            }
        }

    private suspend fun getOutgoingMessageRating(): ResultCompat<Rating> =
        planckProvider.getRatingResult(
            sender,
            listOf(Address.create(partner.address)),
            emptyList(),
            emptyList()
        )

    private fun getIncomingMessageRating(): ResultCompat<Rating> =
        planckProvider.incomingMessageRating(localMessage)

    /**
     * changeTrustwordsLanguage
     *
     * Change language of the trustwords.
     *
     * @param languagePosition Position of the desired language in planck locales.
     */
    fun changeTrustwordsLanguage(languagePosition: Int) {
        val planckLanguages = PlanckUtils.getPlanckLocales()
        changeTrustwords(planckLanguages[languagePosition])
    }

    /**
     * switchTrustwordsLength
     *
     * Switch the length of the trustwords, from short to long and from long to short.
     */
    fun switchTrustwordsLength() {
        viewModelScope.launch {
            stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
            shortTrustWords = !shortTrustWords
            getOrRefreshTrustWords()
        }
    }

    private fun getInitialTrustwordsLanguage(): String {
        var language = K9.getK9CurrentLanguage()
        if (!PlanckUtils.trustWordsAvailableForLang(language)) {
            language = DEFAULT_TRUSTWORDS_LANGUAGE
        }
        return language
    }

    private fun changeTrustwords(language: String) {
        viewModelScope.launch {
            stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
            trustwordsLanguage = language
            getOrRefreshTrustWords()
        }
    }

    private suspend fun getHandshakeData() {
        stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
        updateIdentity().onFailure {
            stateLiveData.value = VerifyPartnerState.ErrorGettingTrustwords
        }.onSuccessSuspend {
            if (PlanckUtils.isPEpUser(partner)) {
                getOrRefreshTrustWords()
            } else {
                stateLiveData.value = handshakeReady(
                    trustwords = "" // no trustwords available for non-planck user
                )
            }
        }
    }

    private fun handshakeReady(
        trustwords: String,
        shortTrustwords: Boolean = true,
    ): VerifyPartnerState.HandshakeReady =
        VerifyPartnerState.HandshakeReady(
            myself = myselfName,
            partner = partnerName,
            ownFpr = PlanckUtils.formatFpr(myself.fpr),
            partnerFpr = PlanckUtils.formatFpr(partner.fpr),
            trustwords = trustwords,
            shortTrustwords = shortTrustwords,
            allowChangeTrust = PlanckUtils.isHandshakeRating(partner.rating)
        )


    private suspend fun getOrRefreshTrustWords() =
        withContext(dispatcherProvider.planckDispatcher()) {
            planckProvider.trustwords(
                myself,
                partner,
                trustwordsLanguage,
                shortTrustWords,
            )
        }.onSuccess { trustwords ->
            // display trustwords on Screen
            stateLiveData.value = if (trustwords.isNullOrBlank()) {
                VerifyPartnerState.ErrorGettingTrustwords
            } else handshakeReady(
                trustwords = trustwords,
                shortTrustwords = shortTrustWords,
            )
        }.onFailure {
            stateLiveData.value = VerifyPartnerState.ErrorGettingTrustwords
        }

    private suspend fun populateData(
        sender: String,
        myself: String,
        messageReference: MessageReference?,
        isMessageIncoming: Boolean
    ): ResultCompat<Unit> = withContext(dispatcherProvider.planckDispatcher()) {
        ResultCompat.ofSuspend {
            if (isMessageIncoming && messageReference == null) {
                error("incoming message needs a message reference always")
            }
            this@VerifyPartnerViewModel.sender = Address.create(sender)
            this@VerifyPartnerViewModel.myself =
                planckProvider.myselfSuspend(PlanckUtils.createIdentity(Address.create(myself), context))!!
            this@VerifyPartnerViewModel.messageReference = messageReference
            this@VerifyPartnerViewModel.isMessageIncoming = isMessageIncoming
        }
    }

    private suspend fun loadMessage(): ResultCompat<LocalMessage?> =
        withContext(dispatcherProvider.io()) {
            ResultCompat.of {
                messageReference?.let { messageReference ->
                    val account = preferences.getAccount(messageReference.accountUuid)
                    controller.loadMessage(
                        account,
                        messageReference.folderName,
                        messageReference.uid
                    )
                }
            }
        }

    /**
     * positiveAction
     *
     * User chose the positive action
     */
    fun positiveAction() {
        when (stateLiveData.value) {
            is VerifyPartnerState.HandshakeReady -> {
                startHandshake(true)
                // display confirmation
                stateLiveData.value = VerifyPartnerState.ConfirmTrust(partnerName)
            }

            is VerifyPartnerState.ConfirmTrust -> {
                performHandshake()
            }

            is VerifyPartnerState.ConfirmMistrust ->
                performHandshake()

            else -> error("unexpected state: ${stateLiveData.value}")
        }
    }

    /**
     * negativeAction
     *
     * User chose the negative action
     */
    fun negativeAction() {
        when (stateLiveData.value) {
            is VerifyPartnerState.HandshakeReady -> {
                startHandshake(false)
                stateLiveData.value = VerifyPartnerState.ConfirmMistrust(partnerName)
            }

            is VerifyPartnerState.ConfirmTrust -> {
                goBack()
            }

            is VerifyPartnerState.ConfirmMistrust ->
                goBack()

            else -> error("unexpected state: ${stateLiveData.value}")
        }
    }

    private fun goBack() {
        viewModelScope.launch {
            getHandshakeData()
        }
    }


}