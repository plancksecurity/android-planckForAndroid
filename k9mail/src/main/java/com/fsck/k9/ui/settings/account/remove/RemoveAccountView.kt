package com.fsck.k9.ui.settings.account.remove

import com.fsck.k9.databinding.ActivityRemoveAccountBinding

interface RemoveAccountView {
    fun showLoading(step: RemoveAccountStep)

    fun hideLoading()

    fun showDialogAtStep(
        step: RemoveAccountStep,
        accountDescription: String
    )

    fun initialize(
        binding: ActivityRemoveAccountBinding,
        onAcceptButtonClicked: () -> Unit,
        onCancelButtonClicked: () -> Unit
    )
}

enum class RemoveAccountStep {
    INITIAL,
    CHECKING_FOR_MESSAGES,
    NORMAL,
    MESSAGES_IN_OUTBOX,
    SENDING_MESSAGES,
    SEND_FAILED,
    REMOVING_ACCOUNT,
    FINISHED
}