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
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.TrustAction
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.infrastructure.MessageView
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlanckStatusPresenter @Inject internal constructor(
    private val planckProvider: PlanckProvider,
    private val cache: PlanckUIArtefactCache,
    @param:MessageView private val displayHtml: DisplayHtml,
    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper,
    private val planckIdentityMapper: PlanckIdentityMapper
) {
    private lateinit var view: PlanckStatusView
    private var identities: List<PlanckIdentity> = emptyList()
    private var localMessage: LocalMessage? = null
    private var isMessageIncoming = false
    private lateinit var senderAddress: Address
    private var currentRating: Rating? = null
    private var latestHandshakeId: Identity? = null
    private var forceUnencrypted = false
    private var isAlwaysSecure = false

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    private fun updateIdentitiesAndNotify() {
        uiScope.launch {
            updateIdentitiesSuspend()
            view.updateIdentities(identities)
        }
    }

    private suspend fun updateIdentitiesSuspend() = withContext(PlanckDispatcher) {
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

    private suspend fun resetTrust(id: Identity) = withContext(PlanckDispatcher) {
        if (isMessageIncoming) {
            resetIncomingMessageTrust(id)
        } else {
            val addresses = recipientAddresses
            resetOutgoingMessageTrust(id, addresses)
        }.mapCatching {
            updateIdentities()
            it
        }
    }.onSuccess {rating ->
        onRatingChanged(rating)
        view.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure)
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

    private suspend fun onTrustReset(rating: Rating) {
        updateIdentitiesSuspend()
        trustWasReset(identities, rating)
    }

    private fun trustWasReset(newIdentities: List<PlanckIdentity>, rating: Rating) {
        onRatingChanged(rating)
        view.updateIdentities(newIdentities)
    }

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

    val recipientAddresses: List<Address>
        get() {
            val addresses: MutableList<Address> = ArrayList(
                identities.size
            )
            for (identity in identities) {
                addresses.add(Address(identity.address))
            }
            return addresses
        }

    private fun onRatingChanged(rating: Rating) {
        currentRating = rating
        if (isMessageIncoming && localMessage != null) {
            localMessage!!.planckRating = rating
        }
        view.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure)
    }

    fun onHandshakeResult(id: Identity?, trust: Boolean) {
        uiScope.launch {
            latestHandshakeId = id
            refreshRating()
                .onSuccess {
                    if (trust) {
                        showUndoAction(TrustAction.TRUST)
                    } else {
                        view.showMistrustFeedback(latestHandshakeId!!.username)
                    }
                    updateIdentitiesAndNotify()
                }
        }
    }

    fun resetpEpData(id: Identity) {
        uiScope.launch {
            ResultCompat.of { planckProvider.keyResetIdentity(id, null) }
                .flatMapSuspend {
                    refreshRating()
                }.onSuccessSuspend {
                    onTrustReset(it)
                    view.showResetPartnerKeySuccessFeedback()
                }.onFailure {
                    view.showResetPartnerKeyErrorFeedback()
                }
        }
    }

    private suspend fun refreshRating(): ResultCompat<Rating> = withContext(PlanckDispatcher) {
        if (isMessageIncoming) {
            planckProvider.incomingMessageRating(localMessage)
        } else {
            setupOutgoingMessageRating()
        }.map {
            onRatingChanged(it)
            it
        }
    }

    private fun showUndoAction(trustAction: TrustAction) {
        when (trustAction) {
            TrustAction.TRUST -> view.showUndoTrust(latestHandshakeId?.username)
            TrustAction.MISTRUST -> view.showUndoMistrust(latestHandshakeId?.username)
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