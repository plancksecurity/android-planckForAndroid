package com.fsck.k9.activity.drawer

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.pEp.ui.infrastructure.DrawerLocker
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener
import com.fsck.k9.pEp.ui.renderers.AccountRenderer
import com.fsck.k9.pEp.ui.renderers.FolderRenderer
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.google.android.material.navigation.NavigationView
import com.pedrogomez.renderers.ListAdapteeCollection
import com.pedrogomez.renderers.RendererBuilder
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class DrawerLayoutManager @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private val drawerView: DrawerView,
        private val preferences: Preferences) : DrawerLocker, NavigationView.OnNavigationItemSelectedListener, DrawerViewInterface {

    private lateinit var toggle: ActionBarDrawerToggle

    private var showingAccountsMenu = false

    private lateinit var rendererFolderBuilder: RendererBuilder<FolderModel>
    private lateinit var rendererAccountBuilder: RendererBuilder<Account>

    private lateinit var drawerCloseListener: DrawerListener

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
            drawerView.setNavigationItemSelectedListener(this)

            setupNavigationHeader()
            setupNavigationFoldersList()
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
        drawerLayoutInterface._updateMessagesForSpecificInbox(account)
    }

    override fun initializeDrawerListener(fromView: View, accountClicked: Account) {
        drawerCloseListener = object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(view: View) {}
            override fun onDrawerClosed(view: View) {
                changeAccount(fromView, accountClicked)
                drawerView.removeDrawerListener(drawerCloseListener)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        }
    }

    override fun createAccountsMenu() {
        showingAccountsMenu = true
        drawerView.setupNavigationHeaderListeners(showingAccountsMenu)

        drawerView.setFoldersDrawerVisibility(View.GONE)
        drawerView.setAccountsDrawerVisibility(View.VISIBLE)
        setAccountAdapter()
        drawerView.setupCreateConfigAccountListeners()
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
        drawerCloseListener = object : DrawerListener {
            override fun onDrawerSlide(view: View, slideOffset: Float) {}
            override fun onDrawerOpened(view: View) {}
            override fun onDrawerClosed(view: View) {
                val folder = account.autoExpandFolderName
                val search = LocalSearch(folder)
                search.addAccountUuid(this@DrawerLayoutManager.account?.uuid)
                search.addAllowedFolder(folder)
                drawerLayoutInterface._refreshMessages(search)
                setupNavigationHeader()
                createFoldersMenu()
                drawerView.showAccounts()
                drawerView.removeDrawerListener(drawerCloseListener)
                drawerLayoutInterface.changeAccountsOrder()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        }
        drawerView.addDrawerListener(drawerCloseListener)
        drawerView.closeDrawers()
    }

    private fun setupNavigationFoldersList() {
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

        drawerView.setFoldersDrawerVisibility(View.VISIBLE)
        drawerView.setAccountsDrawerVisibility(View.GONE)
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
        drawerView.setupMainFolders(unifiedInboxAccount, allMessagesAccount)

        val menuFoldersNotEmpty = menuFolders?.isNotEmpty() ?: false
        val isSameUid = menuFolders?.get(0)?.accountUuid == account?.uuid
        if (menuFoldersNotEmpty && isSameUid) {
            val foldersFiltered: List<LocalFolder> = filterLocalFolders(menuFolders!!)
            drawerView.populateFolders(account!!, foldersFiltered)
            drawerView.setupMainFolders(unifiedInboxAccount, allMessagesAccount)
        } else if (account != null) {
            val instance = MessagingController.getInstance(context)
            instance.listFolders(account, false, object : SimpleMessagingListener() {
                override fun listFolders(account: Account, folders: List<LocalFolder>) {
                    menuFolders = folders
                    val foldersFiltered: List<LocalFolder> = filterLocalFolders(menuFolders!!)
                    drawerView.populateFolders(account, foldersFiltered)
                    drawerView.setupMainFolders(unifiedInboxAccount, allMessagesAccount)
                }
            })
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
        drawerCloseListener = object : DrawerListener {
            override fun onDrawerSlide(view: View, slideOffset: Float) {}
            override fun onDrawerOpened(view: View) {}
            override fun onDrawerClosed(view: View) {
                drawerLayoutInterface.onDrawerClosed(folder)
                drawerView.removeDrawerListener(drawerCloseListener)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        }
        drawerView.addDrawerListener(drawerCloseListener)
        drawerView.closeDrawers()
    }

    private fun changeAccount(fromView: View, accountClicked: Account) {
        fromView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up))
        drawerView.closeDrawers()
        account = accountClicked
        drawerLayoutInterface.updateAccount(accountClicked)
        setupNavigationHeader()
        val folder = accountClicked.autoExpandFolderName
        val search = LocalSearch(folder)
        search.addAccountUuid(accountClicked.uuid)
        search.addAllowedFolder(folder)
        drawerLayoutInterface._refreshMessages(search)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.integrated_inbox -> {
                val unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context)
                drawerLayoutInterface._updateMessagesForSpecificInbox(unifiedInboxAccount)
                true
            }
            R.id.all_messages -> {
                val allMessagesAccount = SearchAccount.createAllMessagesAccount(context)
                drawerLayoutInterface._updateMessagesForSpecificInbox(allMessagesAccount)
                true
            }
            else -> false
        }
    }

    fun closeDrawers() = drawerView.closeDrawers()

    fun drawerWasClosed(): Boolean = drawerView.drawerWasClosed()

    fun clearFolders() = drawerView.clearFolders()

    fun setupMainFoldersUnreadMessages() = drawerView.setupMainFoldersUnreadMessages(unifiedInboxAccount, allMessagesAccount)

}