package com.fsck.k9.ui.endtoend

import android.app.PendingIntent

interface AutocryptKeyTransferView {
    fun setAddress(address: String)

    fun sceneBegin()

    fun sceneGeneratingAndSending()

    fun sceneSendError()

    fun sceneFinished(transition: Boolean = false)

    fun setLoadingStateGenerating()

    fun setLoadingStateSending()

    fun setLoadingStateSendingFailed()

    fun setLoadingStateFinished()

    fun finishWithInvalidAccountError()

    fun finishWithProviderConnectError(providerName: String)

    fun launchUserInteractionPendingIntent(pendingIntent: PendingIntent)

    fun finishAsCancelled()
}