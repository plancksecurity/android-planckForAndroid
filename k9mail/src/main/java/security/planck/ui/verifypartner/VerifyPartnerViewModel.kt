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
    private lateinit var localMessage: LocalMessage
    private lateinit var partner: PlanckIdentity

    private val ratingLiveData: MutableLiveData<Rating?> = MutableLiveData(null)
    val rating: LiveData<Rating?> = ratingLiveData
    private val stateLiveData: MutableLiveData<VerifyPartnerState> =
        MutableLiveData(VerifyPartnerState.Idle)
    val state: LiveData<VerifyPartnerState> = stateLiveData

    private var trustwordsLanguage = getInitialTrustwordsLanguage()
    private var shortTrustWords = true
    private var latestTrust = false
    private val partnerName: String = cache.recipients.first().username
    private val myselfName: String
        get() = myself.username

    fun initialize(
        sender: String,
        myself: String,
        messageReference: MessageReference,
        isMessageIncoming: Boolean
    ) {
        viewModelScope.launch {
            stateLiveData.value = VerifyPartnerState.LoadingHandshakeData
            populateData(sender, myself, messageReference, isMessageIncoming)
            loadMessage().onSuccessSuspend {
                it?.let { message ->
                    localMessage = message
                    ratingLiveData.value = localMessage.planckRating
                    getHandshakeData()
                }
            }.onFailure {
                // display error
                stateLiveData.value = VerifyPartnerState.ErrorLoadingMessage
            }
        }
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
        stateLiveData.value = if (latestTrust) {
            VerifyPartnerState.TrustDone(partnerName)
        } else {
            VerifyPartnerState.MistrustDone(partnerName)
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
        partner = identityMapper.updateAndMapRecipient(cache.recipients.first())
    }

    private fun setupOutgoingMessageRating(): ResultCompat<Rating> {
        return planckProvider.getRatingResult(
            sender,
            listOf(Address(partner.address!!)),
            emptyList(),
            emptyList()
        )
    }

    private suspend fun onRatingChanged(rating: Rating): ResultCompat<Unit> {
        ratingLiveData.value = rating
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
            stateLiveData.value = handshakeReady(
                trustwords = "" // no trustwords available for non-planck user
            )
        }
    }

    private fun handshakeReady(
        trustwords: String,
        shortTrustwords: Boolean = true,
    ): VerifyPartnerState.HandshakeReady =
        VerifyPartnerState.HandshakeReady(
            myself = myselfName,
            partner = partnerName,
            ownFpr = myself.fpr,
            partnerFpr = partner.fpr,
            trustwords = trustwords,
            shortTrustwords = shortTrustwords,
            allowChangeTrust = PlanckUtils.isHandshakeRating(ratingLiveData.value)
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
        messageReference: MessageReference,
        isMessageIncoming: Boolean
    ) = withContext(dispatcherProvider.planckDispatcher()) {
        this@VerifyPartnerViewModel.sender = Address(sender)
        this@VerifyPartnerViewModel.myself =
            planckProvider.myself(PlanckUtils.createIdentity(Address(myself), context))
        this@VerifyPartnerViewModel.messageReference = messageReference
        this@VerifyPartnerViewModel.isMessageIncoming = isMessageIncoming
    }

    private suspend fun loadMessage(): ResultCompat<LocalMessage?> =
        withContext(dispatcherProvider.io()) {
            ResultCompat.of {
                val account = preferences.getAccount(messageReference.accountUuid)
                controller.loadMessage(account, messageReference.folderName, messageReference.uid)
            }
        }

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