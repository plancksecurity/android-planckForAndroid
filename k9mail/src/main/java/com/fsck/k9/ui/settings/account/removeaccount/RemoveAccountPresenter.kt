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
    lateinit var account: Account
    private var step: RemoveAccountStep = RemoveAccountStep.INITIAL

    fun initialize(
        view: RemoveAccountView,
        accountUuid: String
    ) {
        this.view = view
        val argAccount = preferences.getAccount(accountUuid)
        if (argAccount != null) {
            this.account = argAccount
            showInitialScreen()
        } else {
            view.finish()
            return
        }
    }

    private fun showInitialScreen() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            setStep(
                if(checkMessagesLeftInOutboxFolder()) RemoveAccountStep.MESSAGES_IN_OUTBOX
                else RemoveAccountStep.NORMAL
            )
        }
    }

    private fun setStep(step: RemoveAccountStep) {
        this.step = step
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
                view.showDialogAtStep(step, account.description.orEmpty())
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
        when(step) {
            RemoveAccountStep.SEND_FAILED -> removeAccountDefault()
            RemoveAccountStep.NORMAL,
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> removeAccountSendingPendingMessagesIfNeeded()
            else -> {}
        }
    }

    private fun removeAccountDefault() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            deleteAccountWork()
        }
    }

    private fun removeAccountSendingPendingMessagesIfNeeded() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            if (checkMessagesLeftInOutboxFolder()) {
                setStep(RemoveAccountStep.LOADING)

                sendPendindMessages()

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
            account.localStore?.delete()
        } catch (e: Exception) {
            // Ignore, this may lead to localStores on sd-cards that
            // are currently not inserted to be left
        }

        controller.deleteAccount(account)
        preferences.deleteAccount(account)
        k9Wrapper.setServicesEnabled()
        setStep(RemoveAccountStep.FINISHED)
    }

    private suspend fun sendPendindMessages() {}

    private suspend fun checkMessagesLeftInOutboxFolder(): Boolean = withContext(Dispatchers.IO) {
        controller.hasMessagesPendingToSend(account)
    }
}