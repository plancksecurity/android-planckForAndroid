package com.fsck.k9.ui.settings.account.removeaccount

interface RemoveAccountView {
    fun finish()

    fun accountDeleted()

    fun showLoading()

    fun hideLoading()
}