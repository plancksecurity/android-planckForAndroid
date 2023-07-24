package com.fsck.k9.planck.ui.privacy.status

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
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.SimpleResultCallback
import com.fsck.k9.planck.PlanckProvider.TrustAction
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.infrastructure.MessageView
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import javax.inject.Inject

class PlanckStatusPresenter @Inject internal constructor(
    private val planckProvider: PlanckProvider,
    private val cache: PlanckUIArtefactCache,
    @param:MessageView private val displayHtml: DisplayHtml,
    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper,
    private val planckIdentityMapper: PlanckIdentityMapper
) {
    private var view: PlanckStatusView? = null
    private var identities: List<PlanckIdentity>? = null
    private var localMessage: LocalMessage? = null
    private var isMessageIncoming = false
    private var senderAddress: Address? = null
    private var currentRating: Rating? = null
    private var latestHandshakeId: Identity? = null
    private var forceUnencrypted = false
    private var isAlwaysSecure = false
    private val mainThreadHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val newIdentities = msg.obj as List<PlanckIdentity>
            when (msg.what) {
                ON_TRUST_RESET -> trustWasReset(newIdentities, Rating.getByInt(msg.arg1))
                UPDATE_IDENTITIES -> identitiesUpdated(newIdentities)
                LOAD_RECIPIENTS -> recipientsLoaded(newIdentities)
            }
        }
    }

    fun initialize(
        planckStatusView: PlanckStatusView?,
        isMessageIncoming: Boolean,
        senderAddress: Address?,
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
        val workerThread: WorkerThread = WorkerThread(recipients, LOAD_RECIPIENTS)
        workerThread.start()
    }

    private fun recipientsLoaded(newIdentities: List<PlanckIdentity>) {
        identities = newIdentities
        if (!identities!!.isEmpty()) {
            view!!.setupRecipients(identities)
        } else {
            view!!.showItsOnlyOwnMsg()
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
        planckProvider.loadOutgoingMessageRatingAfterResetTrust(
            id,
            senderAddress,
            addresses,
            emptyList(),
            emptyList(),
            object : PlanckProvider.ResultCallback<Rating?> {
                override fun onLoaded(rating: Rating) {
                    onTrustReset(rating, id)
                }

                override fun onError(throwable: Throwable) {}
            })
    }

    private fun onTrustReset(rating: Rating?, id: Identity) {
        val workerThread: WorkerThread = WorkerThread(identities, ON_TRUST_RESET, rating)
        workerThread.start()
    }

    private fun trustWasReset(newIdentities: List<PlanckIdentity>, rating: Rating) {
        onRatingChanged(rating)
        view!!.updateIdentities(newIdentities)
    }

    private fun resetIncomingMessageTrust(id: Identity) {
        planckProvider.loadMessageRatingAfterResetTrust(
            localMessage,
            isMessageIncoming,
            id,
            object : PlanckProvider.ResultCallback<Rating?> {
                override fun onLoaded(result: Rating) {
                    onTrustReset(result, id)
                }

                override fun onError(throwable: Throwable) {}
            })
    }

    private fun setupOutgoingMessageRating(callback: PlanckProvider.ResultCallback<Rating>) {
        val addresses = recipientAddresses
        planckProvider.getRating(senderAddress, addresses, emptyList(), emptyList(), callback)
    }

    val recipientAddresses: List<Address>
        get() {
            val addresses: MutableList<Address> = ArrayList(
                identities!!.size
            )
            for (identity in identities!!) {
                addresses.add(Address(identity.address))
            }
            return addresses
        }

    private fun onRatingChanged(rating: Rating) {
        currentRating = rating
        if (isMessageIncoming && localMessage != null) {
            localMessage!!.planckRating = rating
        }
        view!!.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure)
    }

    fun onHandshakeResult(id: Identity?, trust: Boolean) {
        latestHandshakeId = id
        refreshRating(object : SimpleResultCallback<Rating?>() {
            override fun onLoaded(rating: Rating) {
                onRatingChanged(rating)
                if (trust) {
                    showUndoAction(TrustAction.TRUST)
                } else {
                    view!!.showMistrustFeedback(latestHandshakeId!!.username)
                }
                updateIdentities()
            }
        })
    }

    fun resetpEpData(id: Identity) {
        try {
            planckProvider.keyResetIdentity(id, null)
            refreshRating(object : SimpleResultCallback<Rating?>() {
                override fun onLoaded(rating: Rating) {
                    onRatingChanged(rating)
                    onTrustReset(currentRating, id)
                }
            })
            view!!.showResetPartnerKeySuccessFeedback()
        } catch (e: Exception) {
            view!!.showResetPartnerKeyErrorFeedback()
        }
    }

    private fun refreshRating(callback: PlanckProvider.ResultCallback<Rating>) {
        if (isMessageIncoming) {
            planckProvider.incomingMessageRating(localMessage, callback)
        } else {
            setupOutgoingMessageRating(callback)
        }
    }

    private fun updateIdentities() {
        val recipients = cache.recipients
        val workerThread: WorkerThread = WorkerThread(recipients, UPDATE_IDENTITIES)
        workerThread.start()
    }

    private fun identitiesUpdated(newIdentities: List<PlanckIdentity>) {
        identities = newIdentities
        view!!.updateIdentities(identities)
    }

    private fun showUndoAction(trustAction: TrustAction) {
        when (trustAction) {
            TrustAction.TRUST -> view!!.showUndoTrust(latestHandshakeId!!.username)
            TrustAction.MISTRUST -> view!!.showUndoMistrust(latestHandshakeId!!.username)
        }
    }

    fun callback(): MessageLoaderCallbacks {
        return object : MessageLoaderCallbacks {
            override fun onMessageDataLoadFinished(message: LocalMessage) {
                localMessage = message
                currentRating = localMessage!!.planckRating
            }

            override fun onMessageDataLoadFailed() {
                view!!.showDataLoadError()
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
        if (latestHandshakeId != null) {
            resetTrust(latestHandshakeId!!)
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
        view!!.setupBackIntent(currentRating, forceUnencrypted, isAlwaysSecure)
    }

    fun isAlwaysSecure(): Boolean {
        return isAlwaysSecure
    }

    fun setAlwaysSecure(alwaysSecure: Boolean) {
        isAlwaysSecure = alwaysSecure
        view!!.setupBackIntent(currentRating, forceUnencrypted, alwaysSecure)
    }

    private inner class WorkerThread : Thread {
        private var identities: List<Identity>
        private var what: Int
        private var rating: Rating? = null

        constructor(identities: List<Identity>, what: Int) {
            this.identities = identities
            this.what = what
        }

        constructor(identities: List<PlanckIdentity>?, what: Int, rating: Rating?) {
            this.identities = ArrayList<Identity>(identities)
            this.what = what
            this.rating = rating
        }

        override fun run() {
            val updatedIdentities = planckIdentityMapper.mapRecipients(identities)
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