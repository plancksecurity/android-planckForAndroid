package com.fsck.k9.ui.settings.account.removeaccount

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import kotlinx.coroutines.*
import javax.inject.Inject

class RemoveAccountPresenter @Inject constructor(
    private val k9Wrapper: K9Wrapper,
    private val preferences: Preferences,
    private val controller: MessagingController
) {
    private lateinit var view: RemoveAccountView
    private lateinit var model: RemoveAccountModel

    fun initialize(
        view: RemoveAccountView,
        model: RemoveAccountModel,
        accountUuid: String
    ) {
        this.view = view
        this.model = model
        val argAccount = preferences.getAccount(accountUuid)
        if (argAccount != null) {
            model.account = argAccount
            showInitialScreen()
        } else {
            view.finish()
            return
        }
    }

    private fun showInitialScreen() {
        launchInUIScope {
            setStep(
                if(checkMessagesLeftInOutboxFolder()) RemoveAccountStep.MESSAGES_IN_OUTBOX
                else RemoveAccountStep.NORMAL
            )
        }
    }

    private fun setStep(step: RemoveAccountStep) {
        model.step = step
        renderStep(step)
    }

    private fun renderStep(step: RemoveAccountStep) {
        when(step) {
            RemoveAccountStep.LOADING -> view.showLoading()
            RemoveAccountStep.FINISHED -> {
                view.hideLoading()
                view.accountDeleted()
            }
            else -> {
                view.showDialogAtStep(step, getAccount().description.orEmpty())
                view.hideLoading()
            }
        }
    }

    fun onAcceptButtonClicked() {
        onRemoveAccountConfirmedByUser()
    }

    fun onCancelButtonClicked() {
        view.finish()
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
            deleteAccountWork()
        }
    }

    private fun removeAccountSendingPendingMessagesIfNeeded() {
        launchInUIScope {
            if (checkMessagesLeftInOutboxFolder()) {
                setStep(RemoveAccountStep.LOADING)

                sendPendingMessages()

                if(checkMessagesLeftInOutboxFolder()) {
                    setStep(RemoveAccountStep.SEND_FAILED)
                } else {
                    deleteAccountWork()
                }
            } else {
                deleteAccountWork()
            }
        }
    }

    private suspend fun deleteAccountWork() = withContext(Dispatchers.IO) {
        try {
            getAccount().localStore?.delete()
        } catch (e: Exception) {
            // Ignore, this may lead to localStores on sd-cards that
            // are currently not inserted to be left
        }

        controller.deleteAccount(getAccount())
        preferences.deleteAccount(getAccount())
        k9Wrapper.setServicesEnabled()
        setStep(RemoveAccountStep.FINISHED)
    }

    private fun getAccount(): Account = model.account

    private suspend fun sendPendingMessages() = withContext(Dispatchers.IO) {
        controller.sendPendingMessagesAndHandleSendingNotificationSynchronous(getAccount())
        launch { delay(PROGRESS_DIALOG_MIN_DELAY) }
    }

    private suspend fun checkMessagesLeftInOutboxFolder(): Boolean = withContext(Dispatchers.IO) {
        controller.hasMessagesPendingToSend(getAccount())
    }

    private fun launchInUIScope(block: suspend CoroutineScope.() -> Unit) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch { block() }
    }

    companion object {
        private const val PROGRESS_DIALOG_MIN_DELAY = 600L
    }
}