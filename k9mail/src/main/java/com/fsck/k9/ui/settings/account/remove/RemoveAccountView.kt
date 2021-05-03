package com.fsck.k9.ui.settings.account.remove

import com.fsck.k9.databinding.ActivityRemoveAccountBinding

interface RemoveAccountView {
    fun showLoading()

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
    INITIAL, NORMAL, MESSAGES_IN_OUTBOX, LOADING, SEND_FAILED, FINISHED
}