package com.fsck.k9.ui.messageview

import com.fsck.k9.activity.MessageReference

sealed interface MessageViewEffect {
    object NoEffect : MessageViewEffect
    data class ErrorLoadingMessage(
        val throwable: Throwable? = null,
        val close: Boolean = false
    ) : MessageViewEffect

    data class ErrorDownloadingMessageNotFound(val throwable: Throwable) : MessageViewEffect
    data class ErrorDownloadingNetworkError(val throwable: Throwable) : MessageViewEffect

    object MessageMovedToSuspiciousFolder : MessageViewEffect

    data class MessageOperationError(val throwable: Throwable) : MessageViewEffect

    data class NavigateToResetPartnerKey(val partner: String) : MessageViewEffect

    data class NavigateToVerifyPartner(
        val partner: String,
        val myAddress: String,
        val messageReference: MessageReference,
    ) : MessageViewEffect
}