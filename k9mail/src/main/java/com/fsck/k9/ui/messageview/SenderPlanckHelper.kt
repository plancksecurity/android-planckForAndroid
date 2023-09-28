package com.fsck.k9.ui.messageview

import android.app.Application
import com.fsck.k9.Preferences
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import dagger.hilt.android.scopes.ActivityScoped
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.dialog.BackgroundTaskDialogView
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class SenderPlanckHelper @Inject constructor(
    private val context: Application,
    private val planckProvider: PlanckProvider,
    private val preferences: Preferences,
) {
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var message: LocalMessage
    private lateinit var view: SenderPlanckHelperView
    private var resetPartnerKeyView: BackgroundTaskDialogView? = null
    private var resetState = BackgroundTaskDialogView.State.CONFIRMATION

    fun initialize(message: LocalMessage, view: SenderPlanckHelperView) {
        this.message = message
        this.view = view
    }

    private fun isInitialized(): Boolean = ::message.isInitialized

    fun initializeResetPartnerKeyView(resetPartnerKeyView: BackgroundTaskDialogView) {
        this.resetPartnerKeyView = resetPartnerKeyView
        resetPartnerKeyView.showState(resetState)
    }

    fun canResetSenderKeys(message: LocalMessage?): Boolean {
        message ?: return false
        return isInitialized() && messageConditionsForSenderKeyReset(message) &&
                ratingConditionsForSenderKeyReset(message.planckRating)
    }

    fun checkCanHandshakeSender() {
        uiScope.launch {
            if (messageConditionsForSenderHandshake(message)
                && PlanckUtils.isHandshakeRating(getSenderRating())
            ) {
                view.allowHandshakeWithSender()
            }
        }
    }

    fun partnerKeyResetFinished() {
        resetState = BackgroundTaskDialogView.State.CONFIRMATION
        resetPartnerKeyView = null
    }

    fun resetPlanckData() {
        uiScope.launch {
            waitForInitialization()
            resetState = BackgroundTaskDialogView.State.LOADING
            resetPartnerKeyView?.showState(resetState)
            ResultCompat.of {
                val resetIdentity = PlanckUtils.createIdentity(message.from.first(), context)
                planckProvider.keyResetIdentity(resetIdentity, null)
            }.onSuccess {
                resetState = BackgroundTaskDialogView.State.SUCCESS
                resetPartnerKeyView?.showState(resetState)
            }.onFailure {
                Timber.e(it)
                resetState = BackgroundTaskDialogView.State.ERROR
                resetPartnerKeyView?.showState(resetState)
            }
        }
    }

    private suspend fun waitForInitialization() {
        while (!isInitialized()) {
            delay(20)
        }
    }

    private fun ratingConditionsForSenderKeyReset(
        messageRating: Rating
    ): Boolean {
        return !PlanckUtils.isRatingUnsecure(messageRating) || (messageRating == Rating.pEpRatingMistrust)
    }

    private suspend fun getSenderRating(): Rating = withContext(PlanckDispatcher) {
        planckProvider.getRating(message.from.first())
    }.getOrDefault(Rating.pEpRatingUndefined)

    private fun messageConditionsForSenderHandshake(message: LocalMessage): Boolean =
        message.from != null // sender not null
                && message.from.size == 1 // only one sender
                && PlanckUtils.isHandshakeRating(message.planckRating)
                && message.getRecipients(Message.RecipientType.TO).size == 1 // only one recipient in TO
                && message.getRecipients(Message.RecipientType.CC)
            .isNullOrEmpty() // no recipients in CC
                && message.getRecipients(Message.RecipientType.BCC)
            .isNullOrEmpty() // no recipients in BCC

    private fun messageConditionsForSenderKeyReset(message: LocalMessage): Boolean =
        !message.hasToBeDecrypted()
                && message.from != null // sender not null
                && message.from.size == 1 // only one sender
                && preferences.availableAccounts.none { it.email == message.from.first().address } // sender not one of my own accounts
                && message.getRecipients(Message.RecipientType.TO).size == 1 // only one recipient in TO
                && message.getRecipients(Message.RecipientType.CC)
            .isNullOrEmpty() // no recipients in CC
                && message.getRecipients(Message.RecipientType.BCC)
            .isNullOrEmpty() // no recipients in BCC

}