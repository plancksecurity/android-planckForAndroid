package com.fsck.k9.activity.drawer

import com.fsck.k9.Account
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.planck.models.FolderModel
import kotlinx.coroutines.*
import security.planck.foldable.folders.adapters.BaseLevelListRVRendererAdapter
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class DrawerFolderPopulator @Inject constructor() {
    private var lastUnreadCounts: IntArray = intArrayOf()
    private lateinit var lastFolders: List<LocalFolder>

    fun populateFoldersIfNeeded(
        folderAdapter: BaseLevelListRVRendererAdapter<FolderModel>,
        newFolders: List<LocalFolder>,
        account: Account,
        force: Boolean
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            val newFoldersAreDifferent = areFolderListDifferent(newFolders)
            val newUnreadCounts = calculateNewUnread(newFolders)
            val unreadCountIsDifferent = !newUnreadCounts.contentEquals(lastUnreadCounts)
            if (force || newFoldersAreDifferent || unreadCountIsDifferent) {
                lastFolders = newFolders
                lastUnreadCounts = newUnreadCounts
                populateFolders(
                    folderAdapter,
                    account,
                    ArrayList(newFolders),
                    newUnreadCounts.copyOf()
                )
            }
        }
    }

    private suspend fun populateFolders(
        folderAdapter: BaseLevelListRVRendererAdapter<FolderModel>,
        account: Account,
        lastFolders: List<LocalFolder>,
        lastUnreadCounts: IntArray
    ) {
        var folderModels: MutableList<FolderModel> = ArrayList(lastFolders.size)
        withContext(Dispatchers.Default) {
            lastFolders.forEachIndexed { index, localFolder ->
                val folderModel = FolderModel()
                folderModel.account = account
                folderModel.localFolder = localFolder
                folderModel.unreadCount = lastUnreadCounts[index]
                folderModels.add(folderModel)
            }
        }
        folderAdapter.changePlainList(folderModels)
    }

    private fun areFolderListDifferent(newFolders: List<LocalFolder>): Boolean {
        return if (!::lastFolders.isInitialized) true
        else newFolders != lastFolders
    }

    private suspend fun calculateNewUnread(newFolders: List<LocalFolder>): IntArray {
        return withContext(Dispatchers.IO) {
            val newUnreadCounts = IntArray(newFolders.size) { index ->
                if (lastUnreadCounts.size == newFolders.size) lastUnreadCounts[index] else 0
            }

            newFolders.forEachIndexed { index, localFolder ->
                try {
                    newUnreadCounts[index] = localFolder.unreadMessageCount
                } catch (e: MessagingException) {
                    Timber.e(e)
                }
            }
            newUnreadCounts
        }
    }
}