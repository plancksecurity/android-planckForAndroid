package com.fsck.k9.activity.drawer

import android.content.Context
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.AccountUtils
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.pEp.ui.infrastructure.DrawerLocker
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener
import com.fsck.k9.pEp.ui.renderers.AccountRenderer
import com.fsck.k9.pEp.ui.renderers.FolderRenderer
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.pedrogomez.renderers.ListAdapteeCollection
import com.pedrogomez.renderers.RendererBuilder
import javax.inject.Inject
import javax.inject.Named

class DrawerLayoutManager @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private val drawerView: DrawerView,
        private val preferences: Preferences,
        private var accountUtils: AccountUtils
) : DrawerLocker, DrawerViewInterface {

    private lateinit var toggle: ActionBarDrawerToggle

    private var showingAccountsMenu = false

    private lateinit var rendererFolderBuilder: RendererBuilder<FolderModel>
    private lateinit var rendererAccountBuilder: RendererBuilder<Account>

    private lateinit var drawerCloseListener: CloseDrawerListener

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
        this.toggle = toggle
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

    override fun showLoadingMessages() {
        drawerLayoutInterface.showLoadingMessages()
    }

    override fun updateMessagesForSpecificInbox(account: SearchAccount?) {
        drawerLayoutInterface.updateMessagesForSpecificInbox(account)
    }

    override fun initDrawerListenerAfterAccountChanged(fromView: View, accountClicked: Account) {
        drawerCloseListener = object : CloseDrawerListener() {
            override fun onDrawerClosed(view: View) {
                drawerView.startAnimation(fromView)
                changeAccount(accountClicked)
                drawerView.removeDrawerListener(drawerCloseListener)
            }
        }
    }

    private fun initDrawerListenerOnAccountChanged(account: Account) {
        drawerCloseListener = object : CloseDrawerListener() {
            override fun onDrawerClosed(view: View) {
                val folder = account.autoExpandFolderName
                val search = LocalSearch(folder)
                search.addAccountUuid(this@DrawerLayoutManager.account?.uuid)
                search.addAllowedFolder(folder)
                drawerLayoutInterface.refreshMessages(search)
                setupNavigationHeader()
                createFoldersMenu()
                drawerView.showAccounts()
                drawerView.removeDrawerListener(drawerCloseListener)
                drawerLayoutInterface.changeAccountsOrder()
            }

        }
    }

    private fun initDrawerListenerOnFolderChanged(folder: LocalFolder) {
        drawerCloseListener = object : CloseDrawerListener() {
            override fun onDrawerClosed(view: View) {
                drawerLayoutInterface.onDrawerClosed(folder)
                drawerView.removeDrawerListener(drawerCloseListener)
            }
        }
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
        val accountRenderer = AccountRenderer()
        rendererAccountBuilder = RendererBuilder(accountRenderer)
        accountRenderer.setOnAccountClickListenerListener { account ->
            onAccountClick(account)
        }
        val adapteeCollection = ListAdapteeCollection(accounts)
        drawerView.setAccountsAdapter(rendererAccountBuilder, adapteeCollection)
    }

    private fun onAccountClick(account: Account) {
        this.account = account
        drawerLayoutInterface.showLoadingMessages()
        drawerLayoutInterface.updateAccount(account)
        drawerLayoutInterface.updateLastUsedAccount()
        initDrawerListenerOnAccountChanged(account)
        drawerView.addDrawerListener(drawerCloseListener)
        drawerView.closeDrawers()
    }

    private fun setFoldersAdapter() {
        val folderRenderer = FolderRenderer()
        rendererFolderBuilder = RendererBuilder(folderRenderer)
        folderRenderer.setFolderClickListener(object : OnFolderClickListener {
            override fun onClick(folder: LocalFolder) {
                changeFolder(folder)
            }

            override fun onClick(position: Int) {}
        })
        val adapteeCollection = ListAdapteeCollection<FolderModel>(emptyList())
        drawerView.setFolderAdapter(rendererFolderBuilder, adapteeCollection)
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

    override fun resetDrawerListener() {
        drawerView.addDrawerListener(drawerCloseListener)
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
        val foldersFiltered: List<LocalFolder> = filterLocalFolders(menuFolders!!)
        drawerView.populateFolders(account!!, foldersFiltered)
        drawerView.setupMainFolders(unifiedInboxAccount, allMessagesAccount)
        loadSearchAccountStats(unifiedInboxAccount, allMessagesAccount)
    }

    private fun loadSearchAccountStats(unifiedInboxAccount: SearchAccount, allMessagesAccount: SearchAccount) {
        accountUtils.loadSearchAccountStats(context, unifiedInboxAccount) { _, stats: AccountStats ->
            drawerView.setupUnifiedInboxUnreadMessages(stats)
        }
        accountUtils.loadSearchAccountStats(context, allMessagesAccount) { _, stats: AccountStats ->
            drawerView.setupAllMessagessUnreadMessages(stats)

        }
    }

    private fun filterLocalFolders(folders: List<LocalFolder>): List<LocalFolder> {
        val allMessagesFolderName: String = context.getString(R.string.search_all_messages_title)
        val unifiedFolderName: String = context.getString(R.string.integrated_inbox_title)
        return folders.filter { folder -> folder.name != allMessagesFolderName && folder.name != unifiedFolderName }
    }

    private fun changeFolder(folder: LocalFolder) {
        drawerLayoutInterface.updateFolderName(folder.name)
        drawerLayoutInterface.showLoadingMessages()
        initDrawerListenerOnFolderChanged(folder)
        drawerView.addDrawerListener(drawerCloseListener)
        drawerView.closeDrawers()
    }

    private fun changeAccount(accountClicked: Account) {
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

    override fun setDrawerEnabled(enabled: Boolean) {
        val lockMode = if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        drawerView.setDrawerLockMode(lockMode)
        toggle.isDrawerIndicatorEnabled = enabled
        if (!enabled) {
            toggle.setToolbarNavigationClickListener { drawerLayoutInterface.onBackPressed() }
            drawerLayoutInterface.setUpToolbarHomeIcon()

        }
    }

    fun closeDrawers() = drawerView.closeDrawers()

    fun drawerWasClosed(): Boolean = drawerView.drawerWasClosed()

    fun clearFolders() = drawerView.clearFolders()

}