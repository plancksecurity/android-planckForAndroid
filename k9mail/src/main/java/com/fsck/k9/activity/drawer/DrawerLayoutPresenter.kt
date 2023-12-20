package com.fsck.k9.activity.drawer

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.planck.AccountUtils
import com.fsck.k9.planck.models.FolderModel
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.pedrogomez.renderers.ListAdapteeCollection
import dagger.hilt.android.qualifiers.ActivityContext
import security.planck.foldable.folders.model.LevelListItem
import security.planck.foldable.folders.util.Constants
import security.planck.foldable.folders.util.LevelListBuilderImpl
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

const val DEFAULT_PATH_DEPTH = 4
const val DEFAULT_SEPARATOR = "."

class DrawerLayoutPresenter @Inject constructor(
        @ActivityContext private val context: Context,
        private val preferences: Preferences,
        private val controller: MessagingController,
        private var accountUtils: AccountUtils,
) {

    private var showingAccountsMenu = false
    var account: Account? = null
    private lateinit var unifiedInboxAccount: SearchAccount
    private lateinit var allMessagesAccount: SearchAccount
    private var menuFolders: List<LocalFolder>? = null
    private lateinit var drawerView: DrawerView

    private var layoutClicked: Boolean = false
    private var gettingFolders = AtomicBoolean(false)

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
        populateDrawerGroup(force = ForceMode.FORCE_SET_CURRENT_FOLDERS)
    }

    fun populateDrawerGroup(force: ForceMode = ForceMode.NONE) {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context)
        allMessagesAccount = SearchAccount.createAllMessagesAccount(context)

        account?.let { account ->
            // either account is different or menu folders are null or empty
            val needsNewFolders = menuFolders?.firstOrNull()?.accountUuid != account.uuid
            when {
                force == ForceMode.FORCE_GET_FOLDERS || needsNewFolders -> getFolders()
                else -> {
                    setupFolders(force = force != ForceMode.NONE)
                }
            }
        }
    }

    fun createAccountsMenu() {
        showingAccountsMenu = true
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)
        drawerView.setAccountsDrawerVisible()
        setAccountAdapter()
    }

    private fun setAccountAdapter() {
        val accounts: MutableList<Account> = ArrayList(preferences.availableAccounts)
        accounts.remove(account)
        val collection = ListAdapteeCollection(accounts)
        drawerView.setAccountsAdapter(collection)
    }

    fun setFoldersAdapter() {
        val account = account?: return
        val separator = account.remoteStore?.pathDelimiter ?: DEFAULT_SEPARATOR
        val filter: (LevelListItem<FolderModel>) -> Int = { levelItem ->
            when {
                levelItem.item.itemName == account.inboxFolderName -> 0
                levelItem.item.itemName == account.draftsFolderName -> 1
                levelItem.item.itemName == account.sentFolderName -> 2
                levelItem.item.itemName == account.outboxFolderName -> 3
                levelItem.item.itemName == account.spamFolderName -> 4
                levelItem.item.itemName == account.trashFolderName -> 5
                account.isSpecialFolder(levelItem.item.itemName) -> 900
                else -> Constants.DEFAULT_SHOW_ON_TOP_PRIO
            }
        }
        val levelListBuilder = LevelListBuilderImpl<FolderModel>(
            separator = separator,
            depthLimit = DEFAULT_PATH_DEPTH,
            showOnTopFilter = filter,
            unfoldedPathFilter = { account.isSpecialFolder(it.item.itemName) }
        )
        drawerView.setFolderAdapter(levelListBuilder)
    }

    private fun setupNavigationHeader() {
        account?.let { acc ->
            drawerView.setUpMainAccountView(acc)
        }
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)

        val accounts: MutableList<Account> = ArrayList(preferences.availableAccounts).apply {
            remove(account)
        }
        drawerView.setupAccountsListeners(account!!, accounts)
    }

    private fun getFolders() {
        if (gettingFolders.get()) return
        gettingFolders.set(true)
        controller.listFolders(account, false, object : SimpleMessagingListener() {
            override fun listFolders(account: Account, folders: List<LocalFolder>) {
                menuFolders = folders
                gettingFolders.set(false)
                setupFolders(true)
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

    enum class ForceMode {
        NONE,
        FORCE_GET_FOLDERS,
        FORCE_SET_CURRENT_FOLDERS
    }

}