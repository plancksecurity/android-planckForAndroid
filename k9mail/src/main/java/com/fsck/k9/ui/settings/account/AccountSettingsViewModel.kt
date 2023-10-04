package com.fsck.k9.ui.settings.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderRepositoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.sync.SyncRepository
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val preferences: Preferences,
    private val folderRepositoryManager: FolderRepositoryManager,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val accountLiveData = MutableLiveData<Account>()
    private val foldersLiveData = MutableLiveData<List<Folder>>()

    val isGrouped: Boolean
        get() = syncRepository.isGrouped

    fun getAccount(accountUuid: String): LiveData<Account> {
        if (accountLiveData.value == null) {
            GlobalScope.launch(Dispatchers.Main) {
                val account = withContext(Dispatchers.Default) {
                    loadAccount(accountUuid)
                }

                accountLiveData.value = account
            }
        }

        return accountLiveData
    }

    /**
     * Returns the cached [Account] if possible. Otherwise does a blocking load because `PreferenceFragmentCompat`
     * doesn't support asynchronous preference loading.
     */
    fun getAccountBlocking(accountUuid: String): Account {
        return accountLiveData.value ?: loadAccount(accountUuid).also {
            accountLiveData.value = it
        }
    }

    private fun loadAccount(accountUuid: String) = preferences.getAccount(accountUuid)

    fun getFolders(account: Account): LiveData<List<Folder>> {
        if (foldersLiveData.value == null) {
            loadFolders(account)
        }

        return foldersLiveData
    }

    private fun loadFolders(account: Account) {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        GlobalScope.launch(Dispatchers.Main) {
            val folders = withContext(Dispatchers.Default) {
                folderRepository.getRemoteFolders()
            }

            foldersLiveData.value = folders
        }
    }
}
