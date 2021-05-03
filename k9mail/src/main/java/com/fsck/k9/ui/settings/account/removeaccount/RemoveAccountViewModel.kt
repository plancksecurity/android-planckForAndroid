package com.fsck.k9.ui.settings.account.removeaccount

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account

class RemoveAccountViewModel : ViewModel(), RemoveAccountModel {
    override var step: RemoveAccountStep = RemoveAccountStep.INITIAL
    override lateinit var account: Account
}