package com.fsck.k9.activity.drawer

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.Preferences
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

class DrawerLayoutPresenter @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private val preferences: Preferences,
        private var accountUtils: AccountUtils,
) {

    private var showingAccountsMenu = false
    var account: Account? = null
    private lateinit var unifiedInboxAccount: SearchAccount
    private lateinit var allMessagesAccount: SearchAccount
    private var menuFolders: List<LocalFolder>? = null
    private lateinit var drawerView: DrawerView

    private var layoutClicked: Boolean = false

    fun init(drawerView: DrawerLayoutView) {
        this.drawerView = drawerView
    }

    fun loadNavigationView() {
        account?.let {
            setupNavigationHeader()
            setFoldersAdapter()
            createFoldersMenu()
            drawerView.setNavigationViewInsets()
        }
    }

    fun changeAccount(accountClicked: Account) {
        account = accountClicked
        setupNavigationHeader()
        val search = createSearchFolder(accountClicked)
        drawerView.refreshMessages(search)
    }

    fun onAccountClicked(account: Account) {
        val search = createSearchFolder(account)
        drawerView.refreshMessages(search)
        setupNavigationHeader()
        createFoldersMenu()
    }

    fun createFoldersMenu() {
        showingAccountsMenu = false
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)
        drawerView.setFoldersDrawerVisible()
        populateDrawerGroup()
    }

    fun populateDrawerGroup() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context)
        allMessagesAccount = SearchAccount.createAllMessagesAccount(context)

        val menuFoldersNotEmpty = menuFolders?.isNotEmpty() ?: false
        val isSameUid = if (menuFoldersNotEmpty && account != null) {
            menuFolders?.get(0)?.accountUuid == account?.uuid
        } else {
            false
        }
        when {
            isSameUid -> setupFolders(true)
            account != null -> getFolders()
        }
    }

    fun createAccountsMenu() {
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

    private fun setFoldersAdapter() {
        val collection = ListAdapteeCollection<FolderModel>(emptyList())
        drawerView.setFolderAdapter(collection)
    }

    private fun setupNavigationHeader() {
        account?.let { acc ->
            drawerView.setUpMainAccountView(acc)
        }
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)

        val accounts: MutableList<Account> = ArrayList(preferences.accounts).apply {
            remove(account)
        }
        drawerView.setupAccountsListeners(account!!, accounts)
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

    private fun setupFolders(force: Boolean = false) {
        drawerView.populateFolders(account!!, menuFolders!!, force)
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

    private fun createSearchFolder(account: Account): LocalSearch {
        val folder = account.autoExpandFolderName
        val search = LocalSearch(folder)
        search.addAccountUuid(account.uuid)
        search.addAllowedFolder(folder)
        return search
    }

    fun layoutClick(): Boolean {
        return if (!layoutClicked) {
            layoutClicked = true
            false
        } else
            layoutClicked
    }

    fun resetLayoutClick() {
        layoutClicked = false
    }

}