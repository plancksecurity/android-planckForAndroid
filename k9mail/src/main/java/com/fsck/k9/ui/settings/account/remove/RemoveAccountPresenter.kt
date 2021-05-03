package com.fsck.k9.ui.settings.account.remove

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.pEp.DispatcherProvider
import kotlinx.coroutines.*
import javax.inject.Inject

class RemoveAccountPresenter @Inject constructor(
    private val k9Wrapper: K9Wrapper,
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val lifecycle: Lifecycle,
    private val dispatcherProvider: DispatcherProvider
): LifecycleObserver {
    private lateinit var view: RemoveAccountView
    private lateinit var model: RemoveAccountModel
    private lateinit var scopeProvider: CoroutineScopeProvider
    private lateinit var viewDelegate: RemoveAccountViewDelegate

    init {
        lifecycle.addObserver(this)
    }

    fun initialize(
        view: RemoveAccountView,
        model: RemoveAccountModel,
        scopeProvider: CoroutineScopeProvider,
        viewDelegate: RemoveAccountViewDelegate,
        accountUuid: String,
        initializeModel: Boolean
    ) {
        this.view = view
        this.model = model
        this.scopeProvider = scopeProvider
        this.viewDelegate = viewDelegate

        initializeModelIfNeeded(accountUuid, initializeModel)
    }

    private fun initializeModelIfNeeded(
        accountUuid: String,
        initialize: Boolean
    ) {
        if (initialize) {
            val argAccount = preferences.getAccount(accountUuid)
            if (argAccount != null) {
                model.initialize(argAccount)
            } else {
                viewDelegate.finish()
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @Suppress("unused")
    private fun showCurrentScreen() {
        launchInUIScope {
            renderStep(model.step)
            if(!model.isStarted()) {
                showInitialScreen()
            }
        }
    }

    private fun showInitialScreen() {
        launchInUIScope {
            setStep(
                if(checkMessagesInOutboxAndInformUser()) RemoveAccountStep.MESSAGES_IN_OUTBOX
                else RemoveAccountStep.NORMAL
            )
        }
    }

    private fun setStep(step: RemoveAccountStep) {
        model.step = step
        renderStep(step)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun renderStep(step: RemoveAccountStep) {
        runWithLifecycleSafety {
            when(step) {
                RemoveAccountStep.SENDING_MESSAGES,
                RemoveAccountStep.INITIAL,
                RemoveAccountStep.CHECKING_FOR_MESSAGES,
                RemoveAccountStep.REMOVING_ACCOUNT -> view.showLoading(step)
                RemoveAccountStep.FINISHED -> viewDelegate.accountRemoved()
                else -> {
                    view.showDialogAtStep(step, getAccount().description.orEmpty())
                    view.hideLoading()
                }
            }
        }
    }

    fun onAcceptButtonClicked() {
        onRemoveAccountConfirmedByUser()
    }

    fun onCancelButtonClicked() {
        viewDelegate.finish()
    }

    private fun onRemoveAccountConfirmedByUser() {
        when(model.step) {
            RemoveAccountStep.SEND_FAILED -> removeAccountDefault()
            RemoveAccountStep.NORMAL,
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> removeAccountSendingPendingMessagesIfNeeded()
            else -> {}
        }
    }

    private fun removeAccountDefault() {
        launchInUIScope {
            removeAccount()
        }
    }

    private fun removeAccountSendingPendingMessagesIfNeeded() {
        launchInUIScope {
            if (checkMessagesInOutboxAndInformUser()) {
                sendPendingMessagesAndInformUser()

                if(checkMessagesInOutboxAndInformUser()) {
                    setStep(RemoveAccountStep.SEND_FAILED)
                } else {
                    removeAccount()
                }
            } else {
                removeAccount()
            }
        }
    }

    private suspend fun removeAccount() {
        setStep(RemoveAccountStep.REMOVING_ACCOUNT)
        deleteAccountWork()
        setStep(RemoveAccountStep.FINISHED)
    }

    private suspend fun deleteAccountWork() = withContext(dispatcherProvider.io()) {
        launch { delay(PROGRESS_DIALOG_MIN_DELAY) }
        try {
            getAccount().localStore?.delete()
        } catch (e: Exception) {
            // Ignore, this may lead to localStores on sd-cards that
            // are currently not inserted to be left
        }

        controller.deleteAccount(getAccount())
        preferences.deleteAccount(getAccount())
        k9Wrapper.setServicesEnabled()
    }

    private fun getAccount(): Account = model.account

    private suspend fun sendPendingMessagesAndInformUser() {
        setStep(RemoveAccountStep.SENDING_MESSAGES)
        sendPendingMessages()
    }

    private suspend fun sendPendingMessages() = withContext(dispatcherProvider.io()) {
        controller.sendPendingMessagesAndHandleSendingNotificationSynchronous(getAccount())
        launch { delay(PROGRESS_DIALOG_MIN_DELAY) }
    }

    private suspend fun checkMessagesInOutboxAndInformUser(): Boolean {
        setStep(RemoveAccountStep.CHECKING_FOR_MESSAGES)
        return checkMessagesLeftInOutboxFolder()
    }

    private suspend fun checkMessagesLeftInOutboxFolder(): Boolean = withContext(dispatcherProvider.io()) {
        launch { delay(PROGRESS_DIALOG_MIN_DELAY) }
        controller.hasMessagesPendingToSend(getAccount())
    }

    private fun launchInUIScope(block: suspend CoroutineScope.() -> Unit) {
        scopeProvider.getScope().launch { block() }
    }

    private fun runWithLifecycleSafety(block: () -> Unit) {
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            block()
        }
    }

    companion object {
        private const val PROGRESS_DIALOG_MIN_DELAY = 600L
    }
}