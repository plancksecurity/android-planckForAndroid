package com.fsck.k9.activity.drawer

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.R
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener
import com.fsck.k9.pEp.ui.renderers.AccountRenderer
import com.fsck.k9.pEp.ui.renderers.FolderRenderer
import com.fsck.k9.search.SearchAccount
import com.google.android.material.navigation.NavigationView
import com.pedrogomez.renderers.ListAdapteeCollection
import com.pedrogomez.renderers.RVRendererAdapter
import com.pedrogomez.renderers.RendererBuilder
import security.pEp.ui.PEpUIUtils
import security.pEp.ui.nav_view.NavFolderAccountButton
import javax.inject.Inject
import javax.inject.Named

class DrawerView @Inject constructor(
        @Named("ActivityContext") private val context: Context,
        private var drawerFolderPopulator: DrawerFolderPopulator,
) {

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

    private lateinit var folderAdapter: RVRendererAdapter<FolderModel>
    private lateinit var accountAdapter: RVRendererAdapter<Account>

    private lateinit var drawerViewInterface: DrawerViewInterface

    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var drawerCloseListener: DrawerLayout.DrawerListener

    private val disappearAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_down).apply { duration = 500 }
    private val scaleUpAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_up)

    fun setUpDrawerView(drawerLayout: DrawerLayout, drawerViewInterface: DrawerViewInterface) {
        this.drawerLayout = drawerLayout
        this.drawerViewInterface = drawerViewInterface
        findViewsById()
        setupCreateConfigAccountListeners()
    }

    fun initToggle(toggle: ActionBarDrawerToggle) {
        this.toggle = toggle
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
        configureAccountContainer = drawerLayout.findViewById(R.id.configure_account_container)
    }

    fun setDrawerListener(toggle: ActionBarDrawerToggle) {
        drawerLayout.removeDrawerListener(toggle)
        drawerLayout.addDrawerListener(toggle)
    }

    fun setNavigationViewInsets() {
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

    fun clearFolders() = drawerFolderPopulator.clearFolders()

    fun setupNavigationHeaderListeners(showingAccountsMenu: Boolean) {
        menuHeader.setOnClickListener {
            if (!showingAccountsMenu) {
                navFoldersAccountsButton.showFolders()
                drawerViewInterface.createAccountsMenu()
            } else {
                navFoldersAccountsButton.showAccounts()
                drawerViewInterface.createFoldersMenu()
            }
        }
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

    fun setUpMainAccountView(account: Account) {

        mainAccountText.text = PEpUIUtils.accountNameSummary(account.name)
        mainAccountName.text = account.name
        mainAccountEmail.text = account.email
    }

    fun setFoldersDrawerVisible() {
        foldersDrawerLayout.visibility = View.VISIBLE
        accountsDrawerLayout.visibility = View.GONE
    }

    fun setAccountsDrawerVisible() {
        accountsDrawerLayout.visibility = View.VISIBLE
        foldersDrawerLayout.visibility = View.GONE
    }

    fun setupAccountsListeners(account: Account, accounts: MutableList<Account>) {
        when {
            accounts.size > 1 -> setupThreeAccountsListeners(account, accounts)
            accounts.isNotEmpty() -> setupTwoAccountsListeners(account, accounts)
            else -> {
                firstAccountLayout.visibility = View.GONE
                secondAccountLayout.visibility = View.GONE
            }
        }
    }

    private fun setupTwoAccountsListeners(account: Account, accounts: List<Account>) {
        val firstAccount = accounts[accounts.size - 1]
        firstAccountLayout.visibility = View.VISIBLE
        secondAccountLayout.visibility = View.GONE
        firstAccountText.text = PEpUIUtils.accountNameSummary(firstAccount.name)
        firstAccountLayout.setOnClickListener {
            drawerViewInterface.showLoadingMessages()
            mainAccountText.text = PEpUIUtils.accountNameSummary(firstAccount.name)
            firstAccountText.text = PEpUIUtils.accountNameSummary(account.name)
            mainAccountEmail.text = firstAccount.email
            mainAccountName.text = firstAccount.name
            changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount)
        }
    }

    private fun setupThreeAccountsListeners(account: Account, accounts: List<Account>) {
        val firstAccount = accounts[accounts.size - 1]
        val lastAccount = accounts[accounts.size - 2]
        firstAccountLayout.visibility = View.VISIBLE
        secondAccountLayout.visibility = View.VISIBLE
        firstAccountText.text = PEpUIUtils.accountNameSummary(firstAccount.name)
        secondAccountText.text = PEpUIUtils.accountNameSummary(lastAccount.name)
        firstAccountLayout.setOnClickListener {
            drawerViewInterface.showLoadingMessages()
            mainAccountText.text = PEpUIUtils.accountNameSummary(firstAccount.name)
            mainAccountEmail.text = firstAccount.email
            mainAccountName.text = firstAccount.name
            firstAccountText.text = PEpUIUtils.accountNameSummary(lastAccount.name)
            secondAccountText.text = PEpUIUtils.accountNameSummary(account.name)
            changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount)
        }
        secondAccountLayout.setOnClickListener {
            drawerViewInterface.showLoadingMessages()
            mainAccountText.text = PEpUIUtils.accountNameSummary(lastAccount.name)
            mainAccountEmail.text = lastAccount.email
            mainAccountName.text = lastAccount.name
            secondAccountText.text = PEpUIUtils.accountNameSummary(account.name)
            changeAccountAnimation(mainAccountLayout, secondAccountLayout, lastAccount)
        }
    }

    private fun changeAccountAnimation(goToView: View, fromView: View, accountClicked: Account) {
        val anim = createTranslateAnimation(fromView, goToView)
        fromView.startAnimation(anim)
        initDrawerListenerAfterAccountChanged(fromView, accountClicked)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                goToView.startAnimation(disappearAnimation)
                addDrawerListener(drawerCloseListener)
            }

            override fun onAnimationEnd(animation: Animation) {
                fromView.startAnimation(scaleUpAnimation)
                drawerLayout.closeDrawers()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun createTranslateAnimation(fromView: View, goToView: View): TranslateAnimation {
        val firstAccountLayoutPosition = IntArray(2)
        fromView.getLocationOnScreen(firstAccountLayoutPosition)
        val mainAccountLayoutPosition = IntArray(2)
        goToView.getLocationOnScreen(mainAccountLayoutPosition)
        return TranslateAnimation(
                0F, goToView.x + goToView.width / 2 - firstAccountLayoutPosition[0],
                0F, goToView.y + goToView.height / 2 - firstAccountLayoutPosition[1])
                .apply { duration = 500 }
    }

    fun populateFolders(account: Account, folders: List<LocalFolder>) {
        (context as Activity).runOnUiThread {
            val foldersFiltered: List<LocalFolder> = filterLocalFolders(folders)
            drawerFolderPopulator.populateFoldersIfNeeded(folderAdapter, foldersFiltered, account)
        }
    }

    fun setupMainFolders(unifiedInboxAccount: SearchAccount, allMessagesAccount: SearchAccount) {
        (context as Activity).runOnUiThread {
            val unifiedInbox = drawerLayout.findViewById<View>(R.id.unified_inbox)
            val allMessagesContainer = drawerLayout.findViewById<View>(R.id.all_messages_container)
            unifiedInbox.setOnClickListener { drawerViewInterface.updateMessagesForSpecificInbox(unifiedInboxAccount) }
            allMessagesContainer.setOnClickListener { drawerViewInterface.updateMessagesForSpecificInbox(allMessagesAccount) }
        }
    }

    fun setupUnifiedInboxUnreadMessages(unifiedInboxStats: AccountStats) {
        val unifiedInboxMessages = drawerLayout.findViewById(R.id.unified_inbox_new_messages) as TextView
        setNewInboxMessages(unifiedInboxStats, unifiedInboxMessages)
    }

    fun setupAllMessagesUnreadMessages(allMessagesStats: AccountStats) {
        val allMessages = drawerLayout.findViewById(R.id.all_messages_new_messages) as TextView
        setNewInboxMessages(allMessagesStats, allMessages)
    }

    private fun setNewInboxMessages(stats: AccountStats, unreadMessagesView: TextView) {
        if (stats.unreadMessageCount > 0) {
            unreadMessagesView.visibility = View.VISIBLE
            unreadMessagesView.text = stats.unreadMessageCount.toString()
        } else {
            unreadMessagesView.visibility = View.GONE
        }
    }

    fun closeDrawers() = drawerLayout.closeDrawers()

    fun drawerWasClosed(): Boolean {
        return if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        } else {
            false
        }
    }

    fun setFolderAdapter(collection: ListAdapteeCollection<FolderModel>) {
        val folderRenderer = FolderRenderer()
        val rendererFolderBuilder = RendererBuilder(folderRenderer)
        folderRenderer.setFolderClickListener(object : OnFolderClickListener {
            override fun onClick(folder: LocalFolder) {
                drawerViewInterface.changeFolder(folder)
            }

            override fun onClick(position: Int) {}
        })

        navigationFolders.layoutManager = getDrawerLayoutManager()
        folderAdapter = RVRendererAdapter(rendererFolderBuilder, collection)
        navigationFolders.adapter = folderAdapter
    }

    fun setAccountsAdapter(collection: ListAdapteeCollection<Account>) {
        val accountRenderer = AccountRenderer()
        val rendererAccountBuilder = RendererBuilder(accountRenderer)
        accountRenderer.setOnAccountClickListenerListener { account -> drawerViewInterface.onAccountClick(account) }

        navigationAccounts.layoutManager = getDrawerLayoutManager()
        accountAdapter = RVRendererAdapter(rendererAccountBuilder, collection)
        navigationAccounts.adapter = accountAdapter
    }

    fun addDrawerListener(drawerListener: DrawerLayout.DrawerListener) {
        drawerLayout.addDrawerListener(drawerListener)
    }

    private fun setupCreateConfigAccountListeners() {
        configureAccountContainer.setOnClickListener {
            closeDrawers()
            drawerViewInterface.configureAccountClicked()
        }
        addAccountContainer.setOnClickListener {
            closeDrawers()
            drawerViewInterface.addAccountClicked()
        }
    }

    fun setDrawerEnabled(enabled: Boolean) {
        val lockMode = if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        drawerLayout.setDrawerLockMode(lockMode)
        toggle.isDrawerIndicatorEnabled = enabled
        if (!enabled) {
            toggle.setToolbarNavigationClickListener { drawerViewInterface.onBackPressed() }
            drawerViewInterface.setUpToolbarHomeIcon()
        }
    }

    private fun filterLocalFolders(folders: List<LocalFolder>): List<LocalFolder> {
        val allMessagesFolderName: String = context.getString(R.string.search_all_messages_title)
        val unifiedFolderName: String = context.getString(R.string.integrated_inbox_title)
        return folders.filter { folder -> folder.name != allMessagesFolderName && folder.name != unifiedFolderName }
    }

    private fun initDrawerListenerAfterAccountChanged(fromView: View, accountClicked: Account) {
        drawerCloseListener = onDrawerClosed {
            fromView.startAnimation(scaleUpAnimation)
            drawerViewInterface.changeAccount(accountClicked)
            drawerLayout.removeDrawerListener(drawerCloseListener)
        }
    }

    fun initDrawerListenerOnAccountChanged(account: Account) {
        drawerCloseListener = onDrawerClosed {
            drawerViewInterface.onAccountClicked(account)
            navFoldersAccountsButton.showAccounts()
            drawerLayout.removeDrawerListener(drawerCloseListener)
            drawerViewInterface.changeAccountsOrder()
        }
        addDrawerListener(drawerCloseListener)
        closeDrawers()
    }

    fun initDrawerListenerOnFolderChanged(folder: LocalFolder) {
        drawerCloseListener = onDrawerClosed {
            drawerViewInterface.onDrawerClosed(folder)
            drawerLayout.removeDrawerListener(drawerCloseListener)
        }
        addDrawerListener(drawerCloseListener)
        closeDrawers()
    }
}
