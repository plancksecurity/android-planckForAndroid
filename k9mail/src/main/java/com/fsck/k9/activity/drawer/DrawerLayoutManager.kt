package com.fsck.k9.activity.drawer

import android.content.Context
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.AccountUtils
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.pedrogomez.renderers.ListAdapteeCollection
import javax.inject.Inject
import javax.inject.Named

class DrawerLayoutManager @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private val drawerView: DrawerView,
        private val preferences: Preferences,
        private var accountUtils: AccountUtils,
) : DrawerViewInterface {

    private var showingAccountsMenu = false

    private lateinit var drawerLayoutInterface: DrawerLayoutInterface

    var account: Account? = null

    private lateinit var unifiedInboxAccount: SearchAccount
    private lateinit var allMessagesAccount: SearchAccount
    private var menuFolders: List<LocalFolder>? = null

    fun initView(drawerLayout: DrawerLayout, drawerLayoutInterface: DrawerLayoutInterface) {
        this.drawerLayoutInterface = drawerLayoutInterface
        drawerView.setUpDrawerView(drawerLayout, this)
    }

    fun initializeDrawerToggle(toggle: ActionBarDrawerToggle) {
        drawerView.initToggle(toggle)
        drawerView.setDrawerListener(toggle)
        toggle.syncState()
    }

    fun loadNavigationView() {
        account?.let {

            setupNavigationHeader()
            setFoldersAdapter()
            createFoldersMenu()
            drawerView.setNavigationViewInsets()
        }
    }

    override fun setupNavigationHeader() {
        account?.let { acc ->
            drawerView.setUpMainAccountView(acc)
        }
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)

        val accounts: MutableList<Account> = ArrayList(preferences.accounts).apply {
            remove(account)
        }
        drawerView.setupAccountsListeners(account!!, accounts)
    }

    override fun refreshMessages(search: LocalSearch) {
        drawerLayoutInterface.refreshMessages(search)
    }

    override fun changeAccountsOrder() {
        drawerLayoutInterface.changeAccountsOrder()
    }

    override fun onDrawerClosed(folder: LocalFolder) {
        drawerLayoutInterface.onDrawerClosed(folder)
    }

    override fun onAccountClicked(account: Account) {
        val search = createSearchFolder(account)
        refreshMessages(search)
        setupNavigationHeader()
        createFoldersMenu()
    }

    private fun createSearchFolder(account: Account): LocalSearch {
        val folder = account.autoExpandFolderName
        val search = LocalSearch(folder)
        search.addAccountUuid(account.uuid)
        search.addAllowedFolder(folder)
        return search
    }

    override fun showLoadingMessages() {
        drawerLayoutInterface.showLoadingMessages()
    }

    override fun updateMessagesForSpecificInbox(account: SearchAccount?) {
        drawerLayoutInterface.updateMessagesForSpecificInbox(account)
    }

    override fun createAccountsMenu() {
        showingAccountsMenu = true
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)
        drawerView.setAccountsDrawerVisible()
        setAccountAdapter()
    }

    private fun setAccountAdapter() {
        val accounts: MutableList<Account> = ArrayList(preferences.accounts)
        accounts.remove(account)
        val collection = ListAdapteeCollection(accounts)
        drawerView.setAccountsAdapter(collection)
    }

    override fun onAccountClick(account: Account) {
        this.account = account
        drawerLayoutInterface.showLoadingMessages()
        drawerLayoutInterface.updateAccount(account)
        drawerLayoutInterface.updateLastUsedAccount()
        drawerView.initDrawerListenerOnAccountChanged(account)
    }

    override fun onBackPressed() {
        drawerLayoutInterface.onBackPressed()
    }

    override fun setUpToolbarHomeIcon() {
        drawerLayoutInterface.setUpToolbarHomeIcon()
    }

    private fun setFoldersAdapter() {
        val collection = ListAdapteeCollection<FolderModel>(emptyList())
        drawerView.setFolderAdapter(collection)
    }

    override fun createFoldersMenu() {
        showingAccountsMenu = false
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)
        drawerView.setFoldersDrawerVisible()
        populateDrawerGroup()
    }

    override fun configureAccountClicked() {
        drawerLayoutInterface.editAccount()
    }

    override fun addAccountClicked() {
        AccountSetupBasics.actionNewAccount(context)
    }

    fun populateDrawerGroup() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context)
        allMessagesAccount = SearchAccount.createAllMessagesAccount(context)

        val menuFoldersNotEmpty = menuFolders?.isNotEmpty() ?: false
        val isSameUid = menuFolders?.get(0)?.accountUuid == account?.uuid
        when {
            menuFoldersNotEmpty && isSameUid -> setupFolders()
            account != null -> getFolders()
        }
    }

    private fun getFolders() {
        MessagingController.getInstance(context)
                .listFolders(account, false, object : SimpleMessagingListener() {
                    override fun listFolders(account: Account, folders: List<LocalFolder>) {
                        menuFolders = folders
                        setupFolders()
                    }
                })
    }

    private fun setupFolders() {
        drawerView.populateFolders(account!!, menuFolders!!)
        drawerView.setupMainFolders(unifiedInboxAccount, allMessagesAccount)
        loadSearchAccountStats()
    }

    private fun loadSearchAccountStats() {
        accountUtils.loadSearchAccountStats(context, unifiedInboxAccount) { _, stats: AccountStats ->
            drawerView.setupUnifiedInboxUnreadMessages(stats)
        }
        accountUtils.loadSearchAccountStats(context, allMessagesAccount) { _, stats: AccountStats ->
            drawerView.setupAllMessagesUnreadMessages(stats)
        }
    }

    override fun changeFolder(folder: LocalFolder) {
        drawerLayoutInterface.updateFolderName(folder.name)
        drawerLayoutInterface.showLoadingMessages()
        drawerView.initDrawerListenerOnFolderChanged(folder)
    }

    override fun changeAccount(accountClicked: Account) {
        drawerView.closeDrawers()
        account = accountClicked
        drawerLayoutInterface.updateAccount(accountClicked)
        setupNavigationHeader()
        val folder = accountClicked.autoExpandFolderName
        val search = LocalSearch(folder)
        search.addAccountUuid(accountClicked.uuid)
        search.addAllowedFolder(folder)
        drawerLayoutInterface.refreshMessages(search)
        drawerLayoutInterface.changeAccountsOrder()
    }

    fun setDrawerEnabled(enabled: Boolean) = drawerView.setDrawerEnabled(enabled)

    fun closeDrawers() = drawerView.closeDrawers()

    fun drawerWasClosed(): Boolean = drawerView.drawerWasClosed()

    fun clearFolders() = drawerView.clearFolders()

}