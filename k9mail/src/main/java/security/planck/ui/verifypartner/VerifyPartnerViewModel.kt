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

@HiltViewModel
class VerifyPartnerViewModel @Inject constructor(
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
    private lateinit var messageReference: MessageReference
    private var isMessageIncoming = false
    private var localMessage: LocalMessage? = null
    private var partner: PlanckIdentity? = null

    private val ratingLiveData = MutableLiveData(Rating.pEpRatingUndefined)
    val rating: LiveData<Rating> = ratingLiveData
    private val stateLiveData: MutableLiveData<VerifyPartnerState> =
        MutableLiveData(VerifyPartnerState.Idle)
    val state: LiveData<VerifyPartnerState> = stateLiveData

    private var trustwordsLanguage = getInitialTrustwordsLanguage()
    var shortTrustWords = true
    private var latestTrust = false
    val partnerEmail: String = cache.recipients.first().address
    val myselfEmail: String
        get() = myself.address

    fun initialize(
        sender: String,
        myself: String,
        messageReference: MessageReference,
        isMessageIncoming: Boolean
    ) {
        viewModelScope.launch {
            stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
            populateData(sender, myself, messageReference, isMessageIncoming)
            loadMessage().onSuccess {
                localMessage = it
                rating.value
                getHandshakeData()
            }.onFailure {
                // display error
                stateLiveData.value = VerifyPartnerState.ErrorLoadingMessage
            }
        }
    }

    fun startHandshake(trust: Boolean) {
        latestTrust = trust
    }

    private fun performHandshake() { // from the confirmation
        viewModelScope.launch {
            //setTrustConfimationState(BackgroundTaskDialogView.State.LOADING)
            changePartnerTrust().flatMapSuspend {
                refreshRating()
            }.onSuccessSuspend {
                stateLiveData.value = if (latestTrust) {
                    VerifyPartnerState.TrustDone
                } else {
                    VerifyPartnerState.MistrustDone
                }
                //setTrustConfimationState(BackgroundTaskDialogView.State.SUCCESS)
                //updateIdentityAndNotify()
            }.onFailure {
                // display error
                stateLiveData.value = if (latestTrust) {
                    VerifyPartnerState.ErrorTrusting
                } else {
                    VerifyPartnerState.ErrorMistrusting
                }
                //setTrustConfimationState(BackgroundTaskDialogView.State.ERROR)
            }
        }
    }

    fun rejectHandshake(partner: Identity) {
        viewModelScope.launch {
            //identityView.enableButtons(false) // disable buttons or display progressbar
            withContext(dispatcherProvider.planckDispatcher()) {
                planckProvider.keyMistrusted(partner)
            }
            // enable buttons or hide progressbar
        }
    }

    fun confirmHandshake(partner: Identity) {
        viewModelScope.launch {
            //identityView.enableButtons(false) // disable buttons or display progressbar
            withContext(dispatcherProvider.planckDispatcher()) {
                planckProvider.trustPersonalKey(
                    this@VerifyPartnerViewModel.partner
                )
            }
            // enable buttons or hide progressbar
        }
    }

    private suspend fun updateIdentityAndNotify() {
        updateIdentity()
    }

    private suspend fun updateIdentity() = withContext(dispatcherProvider.planckDispatcher()) {
        partner = identityMapper.updateAndMapRecipient(cache.recipients.first())
    }

    private fun setupOutgoingMessageRating(): ResultCompat<Rating> {
        return planckProvider.getRatingResult(
            sender,
            listOf(Address(partner!!.address!!)),
            emptyList(),
            emptyList()
        )
    }

    private suspend fun onRatingChanged(rating: Rating): ResultCompat<Unit> {
        this.ratingLiveData.value = rating
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
            localMessage?.let {
                it.planckRating = rating
            }
            Unit
        }
    }

    private suspend fun changePartnerTrust(): ResultCompat<Unit> =
        withContext(dispatcherProvider.planckDispatcher()) {
            ResultCompat.of {
                if (latestTrust) planckProvider.trustPersonalKey(partner)
                else planckProvider.keyMistrusted(partner)
            }
        }

    private suspend fun refreshRating(): ResultCompat<Rating> =
        retrieveNewRating().alsoDoFlatSuspend { onRatingChanged(it) }

    private suspend fun retrieveNewRating(): ResultCompat<Rating> =
        withContext(dispatcherProvider.planckDispatcher()) {
            if (isMessageIncoming) {
                planckProvider.incomingMessageRating(localMessage)
            } else {
                setupOutgoingMessageRating()
            }
        }

    fun changeTrustwordsLanguage(languagePosition: Int) {
        val planckLanguages = PlanckUtils.getPlanckLocales()
        changeTrustwords(planckLanguages[languagePosition])
    }

    fun switchTrustwordsLength() {
        viewModelScope.launch {
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
            trustwordsLanguage = language
            getOrRefreshTrustWords()
        }
    }

    private suspend fun getHandshakeData() {
        stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
        updateIdentity()
        if (PlanckUtils.isPEpUser(partner)) {
            getOrRefreshTrustWords()
        } else {
            stateLiveData.value = VerifyPartnerState.HandshakeReady(
                myself.fpr, partner!!.fpr, ""
            )
        }
    }

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
            stateLiveData.value = VerifyPartnerState.HandshakeReady(
                myself.fpr, partner!!.fpr, trustwords
            )
        }.onFailure {
            // error state syncState.value = SyncState.Error(it)
            stateLiveData.value = VerifyPartnerState.ErrorGettingTrustwords
        }

    private suspend fun populateData(
        sender: String,
        myself: String,
        messageReference: MessageReference,
        isMessageIncoming: Boolean
    ) = withContext(dispatcherProvider.planckDispatcher()) {
        this@VerifyPartnerViewModel.sender = Address(sender)
        this@VerifyPartnerViewModel.myself =
            planckProvider.myself(PlanckUtils.createIdentity(Address(myself), context))
        this@VerifyPartnerViewModel.messageReference = messageReference
        this@VerifyPartnerViewModel.isMessageIncoming = isMessageIncoming
    }

    private suspend fun loadMessage(): Result<LocalMessage> = withContext(dispatcherProvider.io()) {
        kotlin.runCatching {
            val account = preferences.getAccount(messageReference.accountUuid)
            controller.loadMessage(account, messageReference.folderName, messageReference.uid)
        }
    }

    fun positiveAction() {
        when (stateLiveData.value) {
            is VerifyPartnerState.HandshakeReady -> {
                startHandshake(true)
                // display confirmation
                stateLiveData.value = VerifyPartnerState.ConfirmTrust
            }

            VerifyPartnerState.ConfirmTrust -> {
                performHandshake()
            }
            //stateLiveData.value = VerifyPartnerState.TrustDone
            VerifyPartnerState.ConfirmMistrust ->
                performHandshake()

            else -> error("unknown option: ${stateLiveData.value}")
        }
    }

    fun negativeAction() {
        when (stateLiveData.value) {
            is VerifyPartnerState.HandshakeReady -> {
                startHandshake(false)
                // display confirmation
                stateLiveData.value = VerifyPartnerState.ConfirmMistrust
            }

            VerifyPartnerState.ConfirmTrust -> {
                goBack()
            }
            //stateLiveData.value = VerifyPartnerState.TrustDone
            VerifyPartnerState.ConfirmMistrust ->
                goBack()

            else -> error("unknown option: ${stateLiveData.value}")
            //stateLiveData.value = VerifyPartnerState.TrustDone
        }
    }

    private fun goBack() {
        viewModelScope.launch {
            getHandshakeData()
        }
    }


}