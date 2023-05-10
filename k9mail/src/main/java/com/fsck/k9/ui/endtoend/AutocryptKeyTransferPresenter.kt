package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.TransportProvider
import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.experimental.android.UI
//import kotlinx.coroutines.experimental.delay
//import kotlinx.coroutines.experimental.launch
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError
import timber.log.Timber
import javax.inject.Inject


class AutocryptKeyTransferPresenter @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val openPgpApiManager: OpenPgpApiManager,
    private val transportProvider: TransportProvider,
    private val preferences: Preferences
) {
    private lateinit var account: Account
    private lateinit var showTransferCodePi: PendingIntent
    private lateinit var view: AutocryptKeyTransferView
    private lateinit var viewModel: AutocryptKeyTransferViewModel

    internal fun initialize(
        view: AutocryptKeyTransferView,
        viewModel: AutocryptKeyTransferViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        this.view = view
        this.viewModel = viewModel
        viewModel.autocryptSetupMessageLiveEvent.observe(lifecycleOwner, Observer { msg -> msg?.let { onEventAutocryptSetupMessage(it) } })
        viewModel.autocryptSetupTransferLiveEvent.observe(lifecycleOwner, Observer { pi -> onLoadedAutocryptSetupTransfer(pi) })
    }

    fun initFromIntent(accountUuid: String?) {
        if (accountUuid == null) {
            view.finishWithInvalidAccountError()
            return
        }

        account = preferences.getAccount(accountUuid)

        openPgpApiManager.setOpenPgpProvider(account.openPgpProvider, object : OpenPgpApiManagerCallback {
            override fun onOpenPgpProviderStatusChanged() {
                if (openPgpApiManager.openPgpProviderState == OpenPgpApiManager.OpenPgpProviderState.UI_REQUIRED) {
                    view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
                }
            }

            override fun onOpenPgpProviderError(error: OpenPgpProviderError) {
                view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
            }
        })

        view.setAddress(account.identities[0].email)

        viewModel.autocryptSetupTransferLiveEvent.recall()
    }

    fun onClickHome() {
        view.finishAsCancelled()
    }

    fun onClickTransferSend() {
        view.sceneGeneratingAndSending()

        /*launch(UI) {
            view.uxDelay()
            view.setLoadingStateGenerating()

            viewModel.autocryptSetupMessageLiveEvent.loadAutocryptSetupMessageAsync(openPgpApiManager.openPgpApi, account)
        }*/
    }

    fun onClickShowTransferCode() {
        view.launchUserInteractionPendingIntent(showTransferCodePi)
    }

    private fun onEventAutocryptSetupMessage(setupMsg: AutocryptSetupMessage) {
        view.setLoadingStateSending()
        view.sceneGeneratingAndSending()

        val transport = transportProvider.getTransport(context, account)
        viewModel.autocryptSetupTransferLiveEvent.sendMessageAsync(transport, setupMsg)
    }

    private fun onLoadedAutocryptSetupTransfer(result: AutocryptSetupTransferResult?) {
        when (result) {
            null -> view.sceneBegin()
            is AutocryptSetupTransferResult.Success -> {
                showTransferCodePi = result.showTransferCodePi
                view.setLoadingStateFinished()
                view.sceneFinished()
            }
            is AutocryptSetupTransferResult.Failure -> {
                Timber.e(result.exception, "Error sending setup message")
                view.setLoadingStateSendingFailed()
                view.sceneSendError()
            }
        }
    }
}
