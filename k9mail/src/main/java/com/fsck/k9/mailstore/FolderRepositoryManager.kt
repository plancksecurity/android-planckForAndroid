package com.fsck.k9.mailstore

import com.fsck.k9.Account
import javax.inject.Inject

class FolderRepositoryManager @Inject constructor() {
    fun getFolderRepository(account: Account) = FolderRepository(account)
}
