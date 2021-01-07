package com.fsck.k9.activity.drawer

import com.fsck.k9.Account
import com.fsck.k9.fragment.MessageListFragment
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

interface MessageListView {
    fun showLoadingMessages()
    fun onBackPressed()
    fun refreshMessages(search: LocalSearch)
    fun setUpToolbarHomeIcon()
    fun updateMessagesForSpecificInbox(account: SearchAccount?)
    fun onDrawerClosed(folder: LocalFolder)
    fun changeAccountsOrder()

    fun editAccount()
    fun addMessageListFragment(fragment: MessageListFragment, isHomeScreen: Boolean)
    fun updateAccount(account: Account)
    fun updateFolderName(folderName: String)
    fun updateLastUsedAccount()
}