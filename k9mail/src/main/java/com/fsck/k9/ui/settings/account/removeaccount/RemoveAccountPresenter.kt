package com.fsck.k9.ui.settings.account.removeaccount

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.*
import javax.inject.Inject

class RemoveAccountPresenter @Inject constructor(
    private val preferences: Preferences
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

    }

    fun onCancelButtonClicked() {
        view.finish()
    }

    private suspend fun checkMessagesLeftInOutboxFolder(): Boolean = withContext(Dispatchers.IO) {
        true
    }
}