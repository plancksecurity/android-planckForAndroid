package com.fsck.k9.activity.accountlist

import com.fsck.k9.BaseAccount

interface AccountClickListener {

    fun flaggedClicked(account: BaseAccount)

    fun unreadClicked(account: BaseAccount)

    fun foldersClicked(account: BaseAccount)

    fun settingsClicked(account: BaseAccount)

}