package com.fsck.k9.activity.drawer

import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.pedrogomez.renderers.ListAdapteeCollection
import security.pEp.foldable.folders.util.LevelListBuilder

interface DrawerView {
    fun refreshMessages(search: LocalSearch)
    fun setupNavigationHeaderListeners(showingAccountsMenu: Boolean)
    fun setAccountsDrawerVisible()
    fun setAccountsAdapter(collection: ListAdapteeCollection<Account>)
    fun setFolderAdapter(levelListBuilder: LevelListBuilder<FolderModel>)
    fun setFoldersDrawerVisible()
    fun populateFolders(account: Account, menuFolders: List<LocalFolder>, force: Boolean)
    fun setupMainFolders(unifiedInboxAccount: SearchAccount, allMessagesAccount: SearchAccount)
    fun setupUnifiedInboxUnreadMessages(stats: AccountStats)
    fun setupAllMessagesUnreadMessages(stats: AccountStats)
    fun setupAccountsListeners(account: Account, accounts: MutableList<Account>)
    fun setUpMainAccountView(account: Account)
    fun setNavigationViewInsets()
    fun removeActivityListener()
    fun addActivityListener()
    fun populateDrawerGroup()

}