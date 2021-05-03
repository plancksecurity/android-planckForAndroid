package com.fsck.k9.ui.settings.account.removeaccount

import androidx.lifecycle.ViewModel

class RemoveAccountViewModel : ViewModel(), RemoveAccountModel {
    override var step: RemoveAccountStep = RemoveAccountStep.INITIAL
}