package com.fsck.k9.mailstore

import com.fsck.k9.Account
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryManager @Inject constructor() {
    fun getFolderRepository(account: Account) = FolderRepository(account)
}
