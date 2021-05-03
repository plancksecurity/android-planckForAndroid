package com.fsck.k9.ui.settings.account.remove

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account

class RemoveAccountViewModel : ViewModel(), RemoveAccountModel {
    override var step: RemoveAccountStep = RemoveAccountStep.INITIAL
    override lateinit var account: Account
    private set

    override fun isStarted(): Boolean = step != RemoveAccountStep.INITIAL

    override fun initialize(account: Account) {
        this.account = account
    }
}