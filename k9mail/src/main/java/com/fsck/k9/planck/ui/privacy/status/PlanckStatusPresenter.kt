package com.fsck.k9.planck.ui.privacy.status

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.planck.DefaultDispatcherProvider
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.infrastructure.MessageView
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper
import dagger.hilt.android.scopes.ActivityScoped
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.dialog.BackgroundTaskDialogView.State
import javax.inject.Inject

@ActivityScoped
class PlanckStatusPresenter @Inject internal constructor(
    private val planckProvider: PlanckProvider,
    private val cache: PlanckUIArtefactCache,
    @param:MessageView private val displayHtml: DisplayHtml,
    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper,
    private val planckIdentityMapper: PlanckIdentityMapper,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
) {
    private lateinit var view: PlanckStatusView
    private var identities: List<PlanckIdentity> = emptyList()
    private var localMessage: LocalMessage? = null
    private var isMessageIncoming = false
    private lateinit var senderAddress: Address
    private var currentRating: Rating? = null
    private var latestHandshakeId: Identity? = null
    private var latestTrust = false
    private var forceUnencrypted = false
    private var isAlwaysSecure = false
    private var trustConfirmationState = State.CONFIRMATION
    private var trustConfirmationView: BackgroundTaskDialogView? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val planckDispatcher: CoroutineDispatcher
        get() = dispatcherProvider.planckDispatcher()

    private val recipientAddresses: List<Address>
        get() = identities.map { Address(it.address) }

    fun initialize(
        planckStatusView: PlanckStatusView,
        isMessageIncoming: Boolean,
        senderAddress: Address,
        forceUnencrypted: Boolean,
        alwaysSecure: Boolean
    ) {
        view = planckStatusView
        this.isMessageIncoming = isMessageIncoming
        this.senderAddress = senderAddress
        this.forceUnencrypted = forceUnencrypted
        isAlwaysSecure = alwaysSecure
    }

    fun initializeTrustConfirmationView(trustConfirmationView: BackgroundTaskDialogView) {
        this.trustConfirmationView = trustConfirmationView
        trustConfirmationView.showState(trustConfirmationState)
    }

    fun handshakeFinished() {
        trustConfirmationState = State.CONFIRMATION
        trustConfirmationView = null
        view.finish()
    }

    fun loadMessage(messageReference: MessageReference?) {
        messageReference?.let {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(
                messageReference,
                callback(),
                displayHtml
            )
        }
    }

    fun loadRecipients() {
        uiScope.launch {
            updateIdentitiesSuspend()
            recipientsLoaded()
        }
    }

    fun startHandshake(identity: PlanckIdentity, trust: Boolean) {
        latestHandshakeId = identity
        latestTrust = trust
        showTrustConfirmation(trust, identity)
    }

    fun performHandshake() {
        uiScope.launch {
            setTrustConfimationState(State.LOADING)
            changePartnerTrust().flatMapSuspend {
                refreshRating()
            }.onSuccess {
                setTrustConfimationState(State.SUCCESS)
            }.onFailure {
                setTrustConfimationState(State.ERROR)
            }
        }
    }

    private suspend fun updateIdentitiesSuspend() = withContext(planckDispatcher) {
        updateIdentities()
    }

    private fun updateIdentities() {
        identities = planckIdentityMapper.mapRecipients(cache.recipients)
    }

    private fun recipientsLoaded() {
        if (identities.isNotEmpty()) {
            view.setupRecipients(identities)
        } else {
            view.showItsOnlyOwnMsg()
        }
    }

    private suspend fun resetTrust(id: Identity) = withContext(planckDispatcher) {
        if (isMessageIncoming) {
            resetIncomingMessageTrust(id)
        } else {
            val addresses = recipientAddresses
            resetOutgoingMessageTrust(id, addresses)
        }.alsoDoCatching {
            updateIdentities()
        }
    }.alsoDoFlatSuspend {
        onRatingChanged(it)
    }.onSuccess {
        view.updateIdentities(identities)
    }

    private fun resetOutgoingMessageTrust(
        id: Identity,
        addresses: List<Address>
    ): ResultCompat<Rating> = planckProvider.loadOutgoingMessageRatingAfterResetTrust(
        id,
        senderAddress,
        addresses,
        emptyList(),
        emptyList()
    )

    private fun resetIncomingMessageTrust(
        id: Identity
    ): ResultCompat<Rating> = planckProvider.loadMessageRatingAfterResetTrust(
        localMessage,
        isMessageIncoming,
        id
    )

    private fun setupOutgoingMessageRating(): ResultCompat<Rating> {
        val addresses = recipientAddresses
        return planckProvider.getRatingResult(senderAddress, addresses, emptyList(), emptyList())
    }

    private suspend fun onRatingChanged(rating: Rating): ResultCompat<Unit> {
        currentRating = rating
        return if (isMessageIncoming) {
            saveRatingToMessage(rating)
        } else {
            ResultCompat.success(Unit)
        }.also {
            view.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure)
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

    private fun showTrustConfirmation(trust: Boolean, identity: PlanckIdentity) {
        if (trust) view.showTrustConfirmationView(identity.address)
        else view.showMistrustConfirmationView(identity.address)
    }

    private suspend fun changePartnerTrust(): ResultCompat<Unit> = withContext(planckDispatcher) {
        ResultCompat.of {
            if (latestTrust) planckProvider.trustPersonaKey(latestHandshakeId)
            else planckProvider.keyMistrusted(latestHandshakeId)
        }
    }

    private fun setTrustConfimationState(state: State) {
        trustConfirmationState = state
        trustConfirmationView?.showState(state)
    }

    private suspend fun refreshRating(): ResultCompat<Rating> =
        retrieveNewRating().alsoDoFlatSuspend { onRatingChanged(it) }

    private suspend fun retrieveNewRating(): ResultCompat<Rating> = withContext(planckDispatcher) {
        if (isMessageIncoming) {
            planckProvider.incomingMessageRating(localMessage)
        } else {
            setupOutgoingMessageRating()
        }
    }

    fun callback(): MessageLoaderCallbacks {
        return object : MessageLoaderCallbacks {
            override fun onMessageDataLoadFinished(message: LocalMessage) {
                localMessage = message
                currentRating = localMessage?.planckRating
            }

            override fun onMessageDataLoadFailed() {
                view.showDataLoadError()
            }

            override fun onMessageViewInfoLoadFinished(messageViewInfo: MessageViewInfo) {}
            override fun onMessageViewInfoLoadFailed(messageViewInfo: MessageViewInfo) {}
            override fun setLoadingProgress(current: Int, max: Int) {}
            override fun onDownloadErrorMessageNotFound() {}
            override fun onDownloadErrorNetworkError() {}
            override fun startIntentSenderForMessageLoaderHelper(
                si: IntentSender, requestCode: Int, fillIntent: Intent,
                flagsMask: Int, flagValues: Int, extraFlags: Int
            ) {
            }
        }
    }

    fun undoTrust() {
        uiScope.launch {
            latestHandshakeId?.let { resetTrust(it) }
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_FORCE_UNENCRYPTED, forceUnencrypted)
        outState.putBoolean(STATE_ALWAYS_SECURE, isAlwaysSecure)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            forceUnencrypted = savedInstanceState.getBoolean(STATE_FORCE_UNENCRYPTED)
            isAlwaysSecure = savedInstanceState.getBoolean(STATE_ALWAYS_SECURE)
        }
    }

    fun isForceUnencrypted(): Boolean {
        return forceUnencrypted
    }

    fun setForceUnencrypted(forceUnencrypted: Boolean) {
        this.forceUnencrypted = forceUnencrypted
        view.setupBackIntent(currentRating, forceUnencrypted, isAlwaysSecure)
    }

    fun isAlwaysSecure(): Boolean {
        return isAlwaysSecure
    }

    fun setAlwaysSecure(alwaysSecure: Boolean) {
        isAlwaysSecure = alwaysSecure
        view.setupBackIntent(currentRating, forceUnencrypted, alwaysSecure)
    }

    companion object {
        private const val STATE_FORCE_UNENCRYPTED = "forceUnencrypted"
        private const val STATE_ALWAYS_SECURE = "alwaysSecure"
    }
}