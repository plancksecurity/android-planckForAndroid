package com.fsck.k9.ui.messageview

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SenderKeyResetHelper(
    private val context: Context,
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
) {
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var currentRating: Rating = Rating.pEpRatingUndefined
    private lateinit var message: LocalMessage
    private lateinit var view: SenderKeyResetHelperView
    private var resetPartnerKeyView: ResetPartnerKeyView? = null
    private var resetState = ResetPartnerKeyView.State.CONFIRMATION

    fun initialize(view: SenderKeyResetHelperView, message: LocalMessage) {
        this.view = view
        this.message = message
        this.currentRating = message.planckRating
    }

    fun checkCanResetSenderKeys(message: LocalMessage, account: Account) {
        uiScope.launch {
            val canResetSenderKey = messageConditionsForSenderKeyReset(message, account) &&
                    meetsConditionsForSenderKeyReset(
                        planckProvider.getRating(message.sender.first())
                            .getOrDefault(Rating.pEpRatingUndefined),
                        message
                    )
            if (canResetSenderKey) {
                view.allowResetSenderKey()
            }
        }
    }

    fun partnerKeyResetFinished() {
        resetState = ResetPartnerKeyView.State.CONFIRMATION
        resetPartnerKeyView = null
    }

    fun resetPlanckData(message: LocalMessage) {
        uiScope.launch {
            resetState = ResetPartnerKeyView.State.LOADING
            resetPartnerKeyView?.showState(resetState)
            ResultCompat.of {
                val resetIdentity = PlanckUtils.createIdentity(message.sender.first(), context)
                planckProvider.keyResetIdentity(resetIdentity, null)
            }.flatMapSuspend {
                refreshRating(message)
            }.onSuccess {
                view.updateRating(it)
                resetState = ResetPartnerKeyView.State.SUCCESS
                resetPartnerKeyView?.showState(resetState)
            }.onFailure {
                Timber.e(it)
                resetState = ResetPartnerKeyView.State.ERROR
                resetPartnerKeyView?.showState(resetState)
            }
        }
    }

    private fun meetsConditionsForSenderKeyReset(
        senderRating: Rating,
        message: LocalMessage
    ): Boolean {
        return !PlanckUtils.isRatingUnsecure(senderRating) || (message.planckRating == Rating.pEpRatingMistrust)
    }

    private fun messageConditionsForSenderKeyReset(
        message: LocalMessage,
        account: Account
    ): Boolean {
        return message.account.uuid == account.uuid // same account of message
                && message.sender.size == 1 // only one sender
                && preferences.availableAccounts.none {
                    it.email == message.sender.first().address
                } // sender not one of my own accounts
                && message.getRecipients(Message.RecipientType.TO).size == 1 // only one recipient in TO
                && message.getRecipients(Message.RecipientType.CC)
                    .isNullOrEmpty() // no recipients in CC
                && message.getRecipients(Message.RecipientType.BCC)
                    .isNullOrEmpty() // no recipients in BCC
                && message.getRecipients(Message.RecipientType.TO)
                    .first().address == account.email // only recipient is me
    }

    private suspend fun refreshRating(message: LocalMessage): ResultCompat<Rating> =
        withContext(PlanckDispatcher) {
            planckProvider.incomingMessageRating(message)
        }.alsoDoCatching { rating ->
            currentRating = rating
            message.planckRating = rating
        }
}