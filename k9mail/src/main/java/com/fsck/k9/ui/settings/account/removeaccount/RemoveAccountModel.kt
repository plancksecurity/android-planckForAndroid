package com.fsck.k9.ui.settings.account.removeaccount

import com.fsck.k9.Account

interface RemoveAccountModel {
    var step: RemoveAccountStep
    val account: Account
    fun isStarted(): Boolean
    fun initialize(account: Account)
}