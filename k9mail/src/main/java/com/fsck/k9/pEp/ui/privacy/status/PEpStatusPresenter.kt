package com.fsck.k9.pEp.ui.privacy.status

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpProvider.SimpleResultCallback
import com.fsck.k9.pEp.PEpProvider.TrustAction
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import javax.inject.Inject

class PEpStatusPresenter @Inject internal constructor(
    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper,
    private val pEpIdentityMapper: PEpIdentityMapper
) {
    private lateinit var view: PEpStatusView
    private lateinit var cache: PePUIArtefactCache
    private lateinit var pEpProvider: PEpProvider
    private var identities: List<PEpIdentity> = emptyList()
    private var localMessage: LocalMessage? = null
    private var isMessageIncoming = false
    private lateinit var senderAddress: Address
    private var currentRating: Rating? = null
    private var latestHandshakeId: Identity? = null
    private var forceUnencrypted = false
    private var isAlwaysSecure = false
    private lateinit var displayHtml: DisplayHtml

    private val mainThreadHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val newIdentities = msg.obj as List<PEpIdentity>
            when (msg.what) {
                ON_TRUST_RESET -> trustWasReset(newIdentities, Rating.getByInt(msg.arg1))
                UPDATE_IDENTITIES -> identitiesUpdated(newIdentities)
                LOAD_RECIPIENTS -> recipientsLoaded(newIdentities)
            }
        }
    }

    fun initialize(
        pEpStatusView: PEpStatusView, uiCache: PePUIArtefactCache, pEpProvider: PEpProvider,
        displayHtml: DisplayHtml, isMessageIncoming: Boolean, senderAddress: Address,
        forceUnencrypted: Boolean, alwaysSecure: Boolean
    ) {
        view = pEpStatusView
        cache = uiCache
        this.pEpProvider = pEpProvider
        this.displayHtml = displayHtml
        this.isMessageIncoming = isMessageIncoming
        this.senderAddress = senderAddress
        this.forceUnencrypted = forceUnencrypted
        isAlwaysSecure = alwaysSecure
    }

    fun loadMessage(messageReference: MessageReference?) {
        if (messageReference != null) {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(
                messageReference,
                callback(),
                displayHtml
            )
        }
    }

    fun loadRecipients() {
        val recipients: List<Identity> = cache.recipients
        val workerThread = WorkerThread(recipients, LOAD_RECIPIENTS)
        workerThread.start()
    }

    private fun recipientsLoaded(newIdentities: List<PEpIdentity>) {
        identities = newIdentities
        if (identities.isNotEmpty()) {
            view.setupRecipients(identities)
        } else {
            view.showItsOnlyOwnMsg()
        }
    }

    private fun resetTrust(id: Identity) {
        if (isMessageIncoming) {
            resetIncomingMessageTrust(id)
        } else {
            val addresses = recipientAddresses
            resetOutgoingMessageTrust(id, addresses)
        }
    }

    private fun resetOutgoingMessageTrust(id: Identity, addresses: List<Address>) {
        pEpProvider.loadOutgoingMessageRatingAfterResetTrust(
            id,
            senderAddress,
            addresses,
            emptyList(),
            emptyList(),
            object : PEpProvider.ResultCallback<Rating> {
                override fun onLoaded(rating: Rating) {
                    onTrustReset(rating, id)
                }

                override fun onError(throwable: Throwable) {}
            })
    }

    private fun onTrustReset(rating: Rating?, id: Identity) {
        val workerThread = WorkerThread(identities, ON_TRUST_RESET, rating)
        workerThread.start()
    }

    private fun trustWasReset(newIdentities: List<PEpIdentity>, rating: Rating) {
        onRatingChanged(rating)
        view.updateIdentities(newIdentities)
    }

    private fun resetIncomingMessageTrust(id: Identity) {
        pEpProvider.loadMessageRatingAfterResetTrust(
            localMessage,
            isMessageIncoming,
            id,
            object : PEpProvider.ResultCallback<Rating> {
                override fun onLoaded(result: Rating) {
                    onTrustReset(result, id)
                }

                override fun onError(throwable: Throwable) {}
            })
    }

    private fun setupOutgoingMessageRating(callback: PEpProvider.ResultCallback<Rating>) {
        val addresses = recipientAddresses
        pEpProvider.getRating(senderAddress, addresses, emptyList(), emptyList(), callback)
    }

    private val recipientAddresses: List<Address>
        get() = identities.map {Address(it.address) }

    private fun onRatingChanged(rating: Rating?) {
        currentRating = rating
        localMessage?.setpEpRating(rating)
        view.setRating(rating)
        view.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure)
    }

    fun onHandshakeResult(id: Identity?, trust: Boolean) {
        latestHandshakeId = id
        refreshRating(object : SimpleResultCallback<Rating>() {
            override fun onLoaded(rating: Rating?) {
                onRatingChanged(rating)
                if (trust) {
                    showUndoAction(TrustAction.TRUST)
                } else {
                    latestHandshakeId?.let {  }
                    view.showMistrustFeedback(latestHandshakeId?.username)
                }
                updateIdentities()
            }
        })
    }

    fun resetpEpData(id: Identity) {
        try {
            pEpProvider.keyResetIdentity(id, null)
            refreshRating(object : SimpleResultCallback<Rating>() {
                override fun onLoaded(rating: Rating) {
                    onRatingChanged(rating)
                    onTrustReset(rating, id)
                }
            })
            view.showResetPartnerKeySuccessFeedback()
        } catch (e: Exception) {
            view.showResetPartnerKeyErrorFeedback()
        }
    }

    private fun refreshRating(callback: PEpProvider.ResultCallback<Rating?>) {
        if (isMessageIncoming) {
            pEpProvider.incomingMessageRating(localMessage, callback)
        } else {
            setupOutgoingMessageRating(callback)
        }
    }


    private fun updateIdentities() {
        val recipients = cache.recipients
        val workerThread = WorkerThread(recipients, UPDATE_IDENTITIES)
        workerThread.start()
    }

    private fun identitiesUpdated(newIdentities: List<PEpIdentity>) {
        identities = newIdentities
        view.updateIdentities(identities)
    }

    @Suppress("SameParameterValue")
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
                currentRating = message.getpEpRating()
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
        latestHandshakeId?.let { resetTrust(it) }
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
        view.updateToolbarColor(
            if (forceUnencrypted) Rating.getByInt(Rating.pEpRatingUnencrypted.value) else currentRating
        )
        view.setupBackIntent(currentRating, forceUnencrypted, isAlwaysSecure)
    }

    fun isAlwaysSecure(): Boolean {
        return isAlwaysSecure
    }

    fun setAlwaysSecure(alwaysSecure: Boolean) {
        isAlwaysSecure = alwaysSecure
        view.setupBackIntent(currentRating, forceUnencrypted, alwaysSecure)
    }

    private inner class WorkerThread : Thread {
        private var identities: List<Identity>
        private var what: Int
        private var rating: Rating? = null

        constructor(identities: List<Identity>, what: Int) {
            this.identities = identities
            this.what = what
        }

        constructor(identities: List<PEpIdentity>, what: Int, rating: Rating?) {
            this.identities = ArrayList<Identity>(identities)
            this.what = what
            this.rating = rating
        }

        override fun run() {
            val updatedIdentities = pEpIdentityMapper.mapRecipients(identities)
            val childThreadMessage = Message()
            childThreadMessage.what = what
            childThreadMessage.obj = updatedIdentities
            if (rating != null) {
                childThreadMessage.arg1 = rating!!.value
            }
            mainThreadHandler.sendMessage(childThreadMessage)
        }
    }

    companion object {
        private const val STATE_FORCE_UNENCRYPTED = "forceUnencrypted"
        private const val STATE_ALWAYS_SECURE = "alwaysSecure"
        private const val LOAD_RECIPIENTS = 1
        private const val ON_TRUST_RESET = 2
        private const val UPDATE_IDENTITIES = 3
    }
}