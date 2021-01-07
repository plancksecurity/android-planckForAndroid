package com.fsck.k9.activity.drawer

import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

interface DrawerViewInterface {
    fun showLoadingMessages()
    fun updateMessagesForSpecificInbox(account: SearchAccount?)
    fun createAccountsMenu()
    fun createFoldersMenu()
    fun configureAccountClicked()
    fun addAccountClicked()
    fun changeFolder(folder: LocalFolder)
    fun onAccountClick(account: Account)
    fun onBackPressed()
    fun setUpToolbarHomeIcon()
    fun changeAccount(accountClicked: Account)
    fun setupNavigationHeader()
    fun refreshMessages(search: LocalSearch)
    fun changeAccountsOrder()
    fun onDrawerClosed(folder: LocalFolder)
    fun onAccountClicked(account: Account)
}