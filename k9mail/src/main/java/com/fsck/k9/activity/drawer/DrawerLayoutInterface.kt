package com.fsck.k9.activity.drawer

import com.fsck.k9.Account
import com.fsck.k9.fragment.MessageListFragment
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

interface DrawerLayoutInterface {
    fun _refreshMessages(search: LocalSearch)
    fun editAccount()
    fun _addMessageListFragment(fragment: MessageListFragment, isHomeScreen: Boolean)
    fun showLoadingMessages()
    fun onBackPressed()
    fun setUpToolbarHomeIcon()
    fun _updateMessagesForSpecificInbox(account: SearchAccount?)
    fun onDrawerClosed(folder: LocalFolder)
    fun updateAccount(account: Account)
    fun updateFolderName(folderName: String)
    fun changeAccountsOrder()
    fun updateLastUsedAccount()
}