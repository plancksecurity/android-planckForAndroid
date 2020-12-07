package com.fsck.k9.activity.drawer

import android.app.Activity
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.material.navigation.NavigationView
import com.pedrogomez.renderers.ListAdapteeCollection
import com.pedrogomez.renderers.RVRendererAdapter
import com.pedrogomez.renderers.RendererBuilder
import kotlinx.android.synthetic.main.toolbar.*
import security.pEp.ui.PEpUIUtils.accountNameSummary
import security.pEp.ui.nav_view.NavFolderAccountButton
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class DrawerLayoutManager @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private var drawerFolderPopulator: DrawerFolderPopulator,
        private val preferences: Preferences,
        private var accountUtils: AccountUtils) : DrawerLocker, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var foldersDrawerLayout: View
    private lateinit var accountsDrawerLayout: View
    private lateinit var addAccountContainer: View
    private lateinit var configureAccountContainer: View

    private lateinit var navigationFolders: RecyclerView
    private lateinit var navigationAccounts: RecyclerView

    private lateinit var firstAccountText: TextView
    private lateinit var secondAccountText: TextView
    private lateinit var firstAccountLayout: View
    private lateinit var secondAccountLayout: View
    private lateinit var mainAccountLayout: View
    private lateinit var mainAccountText: TextView
    private lateinit var mainAccountName: TextView
    private lateinit var mainAccountEmail: TextView

    private lateinit var navFoldersAccountsButton: NavFolderAccountButton
    private lateinit var navigationView: NavigationView

    private lateinit var menuHeader: View

    private lateinit var toggle: ActionBarDrawerToggle

    private var showingAccountsMenu = false

    private var rendererFolderBuilder: RendererBuilder<FolderModel>? = null
    private var folderAdapter: RVRendererAdapter<FolderModel>? = null
    private var rendererAccountBuilder: RendererBuilder<Account>? = null
    private var accountAdapter: RVRendererAdapter<Account>? = null

    private lateinit var drawerCloseListener: DrawerListener

    private lateinit var drawerLayoutInterface: DrawerLayoutInterface

    var account: Account? = null

    private var unifiedInboxAccount: SearchAccount? = null
    private var allMessagesAccount: SearchAccount? = null
    private var menuFolders: List<LocalFolder>? = null

    fun initView(drawerLayout: DrawerLayout , drawerLayoutInterface: DrawerLayoutInterface) {
        this.drawerLayout = drawerLayout
        this.drawerLayoutInterface = drawerLayoutInterface
        findViewsById()
    }

    fun initializeDrawerToggle(toggle: ActionBarDrawerToggle) {
        this.toggle = toggle
        drawerLayout.removeDrawerListener(toggle)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    fun loadNavigationView() {
        account?.let {
            navigationView.setNavigationItemSelectedListener(this)

            setupNavigationHeader()
            setupNavigationFoldersList()
            createFoldersMenu()
            setNavigationViewInsets()
        }
    }

    private fun findViewsById() {
        foldersDrawerLayout = drawerLayout.findViewById(R.id.navigation_bar_folders_layout)
        accountsDrawerLayout = drawerLayout.findViewById(R.id.navigation_bar_accounts_layout)
        navigationView = drawerLayout.findViewById(R.id.nav_view)
        mainAccountName = drawerLayout.findViewById(R.id.nav_header_name)
        mainAccountEmail = drawerLayout.findViewById(R.id.nav_header_email)
        mainAccountText = drawerLayout.findViewById(R.id.nav_header_contact_text)
        mainAccountLayout = drawerLayout.findViewById(R.id.nav_header_image_container)
        firstAccountText = drawerLayout.findViewById(R.id.first_account)
        firstAccountLayout = drawerLayout.findViewById(R.id.first_account_container)
        secondAccountText = drawerLayout.findViewById(R.id.second_account)
        secondAccountLayout = drawerLayout.findViewById(R.id.second_account_container)
        navigationFolders = drawerLayout.findViewById(R.id.navigation_folders)
        navFoldersAccountsButton = drawerLayout.findViewById(R.id.navFoldersAccountsButton)
        navigationAccounts = drawerLayout.findViewById(R.id.navigation_accounts)
        menuHeader = drawerLayout.findViewById(R.id.menu_header)
        addAccountContainer = drawerLayout.findViewById(R.id.add_account_container)
    }

    private fun setNavigationViewInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(navigationView) { _, insets ->
            val view: View = navigationView.findViewById(R.id.menu_header)
            view.setPadding(
                    view.paddingLeft,
                    insets.systemWindowInsetTop,
                    view.paddingRight,
                    view.paddingBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }

    private fun setupNavigationHeader() {
        account?.let { acc ->
            mainAccountText.text = accountNameSummary(acc.name)
            mainAccountName.text = acc.name
            mainAccountEmail.text = acc.email
        }

        setupNavigationHeaderListeners()
        setupAccountsListeners()
    }

    private fun setupNavigationHeaderListeners() {
        menuHeader.setOnClickListener {
            if (!showingAccountsMenu) {
                navFoldersAccountsButton.showFolders()
                createAccountsMenu()
            } else {
                navFoldersAccountsButton.showAccounts()
                createFoldersMenu()
            }
        }
    }

    private fun createAccountsMenu() {
        navigationAccounts.layoutManager = getDrawerLayoutManager()

        showingAccountsMenu = true
        foldersDrawerLayout.visibility = View.GONE
        accountsDrawerLayout.visibility = View.VISIBLE
        val accounts: MutableList<Account> = ArrayList(preferences.accounts)
        accounts.remove(account)
        val accountRenderer = AccountRenderer()
        rendererAccountBuilder = RendererBuilder(accountRenderer)
        accountRenderer.setOnAccountClickListenerListener { account ->
            drawerLayoutInterface.showLoadingMessages()
            drawerLayoutInterface.updateAccount(account)
            drawerLayoutInterface.updateLastUsedAccount()
            drawerCloseListener = object : DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                override fun onDrawerOpened(drawerView: View) {}
                override fun onDrawerClosed(drawerView: View) {
                    val folder = account.autoExpandFolderName
                    val search = LocalSearch(folder)
                    search.addAccountUuid(this@DrawerLayoutManager.account?.uuid)
                    search.addAllowedFolder(folder)
                    drawerLayoutInterface._refreshMessages(search)
                    setupNavigationHeader()
                    createFoldersMenu()
                    navFoldersAccountsButton.showAccounts()
                    drawerLayout.removeDrawerListener(drawerCloseListener)
                    drawerLayoutInterface.changeAccountsOrder()
                }

                override fun onDrawerStateChanged(newState: Int) {}
            }
            drawerLayout.addDrawerListener(drawerCloseListener)
            drawerLayout.closeDrawers()
            //                onOpenFolder(account.getAutoExpandFolderName());
        }
        val adapteeCollection = ListAdapteeCollection(accounts)
        accountAdapter = RVRendererAdapter(rendererAccountBuilder, adapteeCollection)
        navigationAccounts.adapter = accountAdapter
        setupCreateAccountListener()
        configureAccountContainer = drawerLayout.findViewById(R.id.configure_account_container)
        configureAccountContainer.setOnClickListener {
            drawerLayout.closeDrawers()
            drawerLayoutInterface._onEditSettings()
        }
    }

    private fun setupCreateAccountListener() {
        addAccountContainer.setOnClickListener {
            drawerLayout.closeDrawers()
            AccountSetupBasics.actionNewAccount(context)
        }
    }

    private fun setupNavigationFoldersList() {
        navigationFolders.layoutManager = getDrawerLayoutManager()

        val folderRenderer = FolderRenderer()
        rendererFolderBuilder = RendererBuilder(folderRenderer)
        folderRenderer.setFolderClickListener(object : OnFolderClickListener {
            override fun onClick(folder: LocalFolder) {
                changeFolder(folder)
            }

            override fun onClick(position: Int) {}
        })
        val adapteeCollection = ListAdapteeCollection<FolderModel>(emptyList())
        folderAdapter = RVRendererAdapter(rendererFolderBuilder, adapteeCollection)

        navigationFolders.adapter = folderAdapter
    }

    private fun getDrawerLayoutManager(): LinearLayoutManager {
        val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        return linearLayoutManager
    }

    private fun createFoldersMenu() {
        showingAccountsMenu = false
        foldersDrawerLayout.visibility = View.VISIBLE
        accountsDrawerLayout.visibility = View.GONE
        setupMainFolders()
        populateDrawerGroup()
    }

    fun populateDrawerGroup() {
        val menuFoldersNotEmpty = menuFolders?.isNotEmpty() ?: false
        val isSameUid = menuFolders?.get(0)?.accountUuid == account?.uuid
        if (menuFoldersNotEmpty && isSameUid) {
            populateFolders(menuFolders!!)
        } else if (account != null) {
            val instance = MessagingController.getInstance(context)
            instance.listFolders(account, false, object : SimpleMessagingListener() {
                override fun listFolders(account: Account, folders: List<LocalFolder>) {
                    menuFolders = folders
                    populateFolders(menuFolders!!)
                }
            })
        }
    }

    private fun populateFolders(folders: List<LocalFolder>) {
        val foldersFiltered: List<LocalFolder> = filterLocalFolders(folders)
        (context as Activity).runOnUiThread {
            drawerFolderPopulator.populateFoldersIfNeeded(folderAdapter!!, foldersFiltered, account!!)
            setupMainFolders()
        }
    }

    private fun filterLocalFolders(folders: List<LocalFolder>): List<LocalFolder> {
        val allMessagesFolderName: String = context.getString(R.string.search_all_messages_title)
        val unifiedFolderName: String = context.getString(R.string.integrated_inbox_title)
        return folders.filter { folder -> folder.name != allMessagesFolderName && folder.name != unifiedFolderName }
    }

    private fun setupMainFolders() {
        setupMainFoldersUnreadMessages()
        setupMainFoldersListeners()
    }

    private fun setupMainFoldersListeners() {
        val unifiedInbox: View = drawerLayout.findViewById(R.id.unified_inbox)
        val allMessagesContainer: View = drawerLayout.findViewById(R.id.all_messages_container)
        unifiedInbox.setOnClickListener { drawerLayoutInterface._updateMessagesForSpecificInbox(unifiedInboxAccount) }
        allMessagesContainer.setOnClickListener { drawerLayoutInterface._updateMessagesForSpecificInbox(allMessagesAccount) }
    }

    fun setupMainFoldersUnreadMessages() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context)
        allMessagesAccount = SearchAccount.createAllMessagesAccount(context)
        accountUtils.loadSearchAccountStats(context, unifiedInboxAccount) { _, stats: AccountStats ->
            val unifiedInboxMessages = drawerLayout.findViewById(R.id.unified_inbox_new_messages) as TextView
            setNewInboxMessages(stats, unifiedInboxMessages)
        }
        accountUtils.loadSearchAccountStats(context, allMessagesAccount) { _, stats: AccountStats ->
            val allMessages = drawerLayout.findViewById(R.id.all_messages_new_messages) as TextView
            setNewInboxMessages(stats, allMessages)
        }
    }

    private fun setNewInboxMessages(stats: AccountStats, unreadMessagesView: TextView) {
        if (stats.unreadMessageCount > 0) {
            unreadMessagesView.visibility = View.VISIBLE
            unreadMessagesView.text = stats.unreadMessageCount.toString()
        } else {
            unreadMessagesView.visibility = View.GONE
        }
    }

    private fun setupAccountsListeners() {
        val accounts: MutableList<Account> = ArrayList(preferences.accounts).apply {
            remove(account)
        }
        when {
            accounts.size > 1 -> setupThreeAcountsListeners(accounts)
            accounts.isNotEmpty() -> setupTwoAccountsListeners(accounts)
            else -> {
                firstAccountLayout.visibility = View.GONE
                secondAccountLayout.visibility = View.GONE
            }
        }
    }

    private fun setupTwoAccountsListeners(accounts: List<Account>) {
        val firstAccount = accounts[accounts.size - 1]
        firstAccountLayout.visibility = View.VISIBLE
        secondAccountLayout.visibility = View.GONE
        firstAccountText.text = accountNameSummary(firstAccount.name)
        firstAccountLayout.setOnClickListener {
            drawerLayoutInterface.showLoadingMessages()
            mainAccountText.text = accountNameSummary(firstAccount.name)
            firstAccountText.text = accountNameSummary(account!!.name)
            mainAccountEmail.text = firstAccount.email
            mainAccountName.text = firstAccount.name
            changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount)
        }
    }

    private fun setupThreeAcountsListeners(accounts: List<Account>) {
        val firstAccount = accounts[accounts.size - 1]
        val lastAccount = accounts[accounts.size - 2]
        firstAccountLayout.visibility = View.VISIBLE
        secondAccountLayout.visibility = View.VISIBLE
        firstAccountText.text = accountNameSummary(firstAccount.name)
        secondAccountText.text = accountNameSummary(lastAccount.name)
        firstAccountLayout.setOnClickListener {
            drawerLayoutInterface.showLoadingMessages()
            mainAccountText.text = accountNameSummary(firstAccount.name)
            mainAccountEmail.text = firstAccount.email
            mainAccountName.text = firstAccount.name
            firstAccountText.text = accountNameSummary(lastAccount.name)
            secondAccountText.text = accountNameSummary(account!!.name)
            changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount)
        }
        secondAccountLayout.setOnClickListener {
            drawerLayoutInterface.showLoadingMessages()
            mainAccountText.text = accountNameSummary(lastAccount.name)
            mainAccountEmail.text = lastAccount.email
            mainAccountName.text = lastAccount.name
            secondAccountText.text = accountNameSummary(account!!.name)
            changeAccountAnimation(mainAccountLayout, secondAccountLayout, lastAccount)
        }
    }

    private fun changeAccountAnimation(goToView: View, fromView: View, accountClicked: Account) {
        val firstAccountLayoutPosition = IntArray(2)
        fromView.getLocationOnScreen(firstAccountLayoutPosition)
        val mainAccountLayoutPosition = IntArray(2)
        goToView.getLocationOnScreen(mainAccountLayoutPosition)
        val anim = TranslateAnimation(0F, goToView.x + goToView.width / 2 - firstAccountLayoutPosition[0],
                0F, goToView.y + goToView.height / 2 - firstAccountLayoutPosition[1])
        anim.duration = 500
        fromView.startAnimation(anim)
        initializeDrawerListener(fromView, accountClicked)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                val disappearAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                disappearAnimation.duration = 500
                goToView.startAnimation(disappearAnimation)
                drawerLayout.addDrawerListener(drawerCloseListener)
            }

            override fun onAnimationEnd(animation: Animation) {
                fromView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up))
                drawerLayout.closeDrawers()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun initializeDrawerListener(fromView: View, accountClicked: Account) {
        drawerCloseListener = object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                changeAccount(fromView, accountClicked)
                drawerLayout.removeDrawerListener(drawerCloseListener)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        }
    }

    private fun changeFolder(folder: LocalFolder) {
        drawerLayoutInterface.updateFolderName(folder.name)
        drawerLayoutInterface.showLoadingMessages()
        drawerCloseListener = object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                drawerLayoutInterface.onDrawerClosed(folder)
                drawerLayout.removeDrawerListener(drawerCloseListener)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        }
        drawerLayout.addDrawerListener(drawerCloseListener)
        drawerLayout.closeDrawers()
    }

    private fun changeAccount(fromView: View, accountClicked: Account) {
        fromView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up))
        drawerLayout.closeDrawers()
        drawerLayoutInterface.updateAccount(accountClicked)
        setupNavigationHeader()
        val folder = accountClicked.autoExpandFolderName
        val search = LocalSearch(folder)
        search.addAccountUuid(accountClicked.uuid)
        search.addAllowedFolder(folder)
        drawerLayoutInterface._refreshMessages(search)
        drawerLayoutInterface.changeAccountsOrder()
    }

    fun closeDrawers() = drawerLayout.closeDrawers()

    override fun setDrawerEnabled(enabled: Boolean) {
        val lockMode = if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        drawerLayout.setDrawerLockMode(lockMode)
        toggle.isDrawerIndicatorEnabled = enabled
        if (!enabled) {
            toggle.setToolbarNavigationClickListener { drawerLayoutInterface.onBackPressed() }
            drawerLayoutInterface.setUpToolbarHomeIcon()

        }
    }

    fun clearFolders() {
        drawerFolderPopulator.clearFolders()
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

    fun drawerWasClosed(): Boolean {
        return if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        } else {
            false
        }
    }
}