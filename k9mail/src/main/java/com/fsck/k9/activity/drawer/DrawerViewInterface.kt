package com.fsck.k9.activity.drawer

import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.search.SearchAccount

interface DrawerViewInterface {
    fun showLoadingMessages()
    fun updateMessagesForSpecificInbox(account: SearchAccount?)
    fun initDrawerListenerAfterAccountChanged(fromView: View, accountClicked: Account)
    fun createAccountsMenu()
    fun createFoldersMenu()
    fun configureAccountClicked()
    fun addAccountClicked()
    fun resetDrawerListener()
    fun changeFolder(folder: LocalFolder)
    fun onAccountClick(account: Account)
    fun onBackPressed()
    fun setUpToolbarHomeIcon()
}