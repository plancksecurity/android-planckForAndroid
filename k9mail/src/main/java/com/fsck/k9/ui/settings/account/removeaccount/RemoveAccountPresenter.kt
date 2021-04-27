package com.fsck.k9.ui.settings.account.removeaccount

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import javax.inject.Inject

class RemoveAccountPresenter @Inject constructor(
    private val preferences: Preferences
) {
    private lateinit var view: RemoveAccountView
    lateinit var account: Account

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

    }

    fun onAcceptButtonClicked() {

    }

    fun onCancelButtonClicked() {
        view.finish()
    }
}