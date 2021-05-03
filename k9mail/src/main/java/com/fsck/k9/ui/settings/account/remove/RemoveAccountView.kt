package com.fsck.k9.ui.settings.account.remove

interface RemoveAccountView {
    fun finish()

    fun accountDeleted()

    fun showLoading()

    fun hideLoading()

    fun showDialogAtStep(
        step: RemoveAccountStep,
        accountDescription: String
    )
}

enum class RemoveAccountStep {
    INITIAL, NORMAL, MESSAGES_IN_OUTBOX, LOADING, SEND_FAILED, FINISHED
}