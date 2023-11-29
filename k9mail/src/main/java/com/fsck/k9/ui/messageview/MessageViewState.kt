package com.fsck.k9.ui.messageview

import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo

sealed interface MessageViewState {
    object Idle : MessageViewState
    object Loading : MessageViewState
    data class ErrorLoadingMessage(val throwable: Throwable? = null) : MessageViewState
    object ErrorDecryptingMessageKeyMissing : MessageViewState
    data class ErrorDecryptingMessage(val throwable: Throwable) : MessageViewState
    data class ErrorDownloadingMessageNotFound(val throwable: Throwable) : MessageViewState
    data class ErrorDownloadingNetworkError(val throwable: Throwable) : MessageViewState
    data class ErrorDecodingMessage(val info: MessageViewInfo, val throwable: Throwable) :
        MessageViewState

    data class DecryptedMessageLoaded(
        val message: LocalMessage,
        val moveToSuspiciousFolder: Boolean
    ) : MessageViewState

    data class EncryptedMessageLoaded(val message: LocalMessage) : MessageViewState
    data class MessageDecoded(val info: MessageViewInfo) : MessageViewState
}