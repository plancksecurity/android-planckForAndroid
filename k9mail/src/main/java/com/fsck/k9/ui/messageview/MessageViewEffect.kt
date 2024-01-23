package com.fsck.k9.ui.messageview

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
}