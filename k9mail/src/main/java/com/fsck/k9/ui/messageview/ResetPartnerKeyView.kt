package com.fsck.k9.ui.messageview

interface ResetPartnerKeyView {
    fun showState(state: State)

    enum class State {
        CONFIRMATION,
        LOADING,
        ERROR,
        SUCCESS
    }
}