package com.fsck.k9.ui.messageview

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
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

class SenderKeyResetHelper(
    private val context: Context,
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
) {
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var currentRating: Rating = Rating.pEpRatingUndefined
    private lateinit var message: LocalMessage

    fun checkCanResetSenderKeys(message: LocalMessage, account: Account) {
        uiScope.launch {
            val canResetSenderKey = messageConditionsForSenderKeyReset(message, account) &&
                    meetsConditionsForSenderKeyReset(
                        planckProvider.getRating(message.sender.first())
                            .getOrDefault(Rating.pEpRatingUndefined),
                        message
                    )
            if (canResetSenderKey) {
                // here call to the view or set a livedata to allow the option of sender key reset
            }
        }
    }

    private fun meetsConditionsForSenderKeyReset(senderRating: Rating, message: LocalMessage): Boolean {
        return !PlanckUtils.isRatingUnsecure(senderRating) || (message.planckRating == Rating.pEpRatingMistrust)
    }

    private fun messageConditionsForSenderKeyReset(message: LocalMessage, account: Account): Boolean {
        return message.account.uuid == account.uuid && // same account of message
                message.sender.size == 1 && // only one sender
                preferences.availableAccounts.none { it.email == message.sender.first().address } && // sender not one of my own accounts
                message.getRecipients(Message.RecipientType.TO).size == 1 && // only one recipient in TO
                message.getRecipients(Message.RecipientType.CC).isNullOrEmpty() && // no recipients in CC
                message.getRecipients(Message.RecipientType.BCC).isNullOrEmpty() && // no recipients in BCC
                message.getRecipients(Message.RecipientType.TO).first().address == account.email // only recipient is me
    }

    fun resetPlanckData(message: LocalMessage) {
        uiScope.launch {
            ResultCompat.of {
                val resetIdentity = PlanckUtils.createIdentity(message.sender.first(), context)
                planckProvider.keyResetIdentity(resetIdentity, null)
            }.flatMapSuspend {
                refreshRating(message)
            }.onSuccess {
                // here we need to give back new rating to the MessageViewFragment.
            }.onFailure {
                // here we need to report the error in some way
            }
        }
    }

    private suspend fun refreshRating(message: LocalMessage): ResultCompat<Rating> = withContext(PlanckDispatcher) {
        planckProvider.incomingMessageRating(message)
    }.alsoDoCatching { rating ->
        currentRating = rating
        message.planckRating = rating
    }

    private suspend fun onRatingChanged(rating: Rating): ResultCompat<Unit> {
        currentRating = rating
        return saveRatingToMessage(rating)
    }

    private suspend fun saveRatingToMessage(
        rating: Rating
    ): ResultCompat<Unit> = withContext(Dispatchers.IO) {
        ResultCompat.of {
            message.planckRating = rating
        }
    }
}