package com.fsck.k9.ui.settings.account.removeaccount

import com.fsck.k9.Account

interface RemoveAccountModel {
    var step: RemoveAccountStep
    var account: Account
}