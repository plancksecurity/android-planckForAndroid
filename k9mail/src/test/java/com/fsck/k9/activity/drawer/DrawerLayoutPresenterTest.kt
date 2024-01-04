package com.fsck.k9.activity.drawer

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.Preferences
import com.fsck.k9.activity.ActivityListener
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.planck.AccountStatsCallback
import com.fsck.k9.planck.AccountUtils
import com.fsck.k9.search.SearchAccount
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val ACCOUNT1_UUID = "uuid1"
private const val ACCOUNT2_UUID = "uuid2"
private const val MAIL1 = "test1@test.ch"
private const val MAIL2 = "test2@test.ch"
private const val FOLDER_NAME = "Inbox"

class DrawerLayoutPresenterTest {
    private val context: Context = mockk()
    private val remoteStore: RemoteStore = mockk {
        every { pathDelimiter }.returns(DEFAULT_SEPARATOR)
    }
    private val account1: Account = mockk {
        every { uuid }.returns(ACCOUNT1_UUID)
        every { email }.returns(MAIL1)
        every { remoteStore }.returns(this@DrawerLayoutPresenterTest.remoteStore)
        every { autoExpandFolderName }.returns(FOLDER_NAME)
    }
    private val account2: Account = mockk {
        every { uuid }.returns(ACCOUNT2_UUID)
        every { email }.returns(MAIL2)
        every { remoteStore }.returns(this@DrawerLayoutPresenterTest.remoteStore)
        every { autoExpandFolderName }.returns(FOLDER_NAME)
    }
    private val unifiedInboxAccount: SearchAccount = mockk()
    private val allMessagesAccount: SearchAccount = mockk()
    private val preferences: Preferences = mockk {
        every { availableAccounts }.answers { listOf(account1, account2) }
    }
    private val folder1: LocalFolder = mockk {
        every { name }.returns(FOLDER_NAME)
        every { accountUuid }.returns(ACCOUNT1_UUID)
    }
    private val controller: MessagingController = mockk {
        val callbackSlot = slot<SimpleMessagingListener>()
        val accountSlot = slot<Account>()
        every { listFolders(capture(accountSlot), any(), capture(callbackSlot)) }.answers {
            callbackSlot.captured.listFolders(accountSlot.captured, listOf(folder1))
        }
    }
    private val accountStats = AccountStats()
    private val accountUtils: AccountUtils = mockk {
        val callbackSlot = slot<AccountStatsCallback>()
        val searchAccountSlot = slot<SearchAccount>()
        every {
            loadSearchAccountStats(
                any(),
                capture(searchAccountSlot),
                capture(callbackSlot)
            )
        }.answers {
            callbackSlot.captured.accountStatusChanged(searchAccountSlot.captured, accountStats)
        }
    }
    private val view: DrawerLayoutView = mockk(relaxed = true)
    private val presenter = DrawerLayoutPresenter(
        context,
        preferences,
        controller,
        accountUtils
    )

    private lateinit var activityListener: ActivityListener

    @Before
    fun setUp() {
        mockkStatic(SearchAccount::class)
        every { SearchAccount.createAllMessagesAccount(any()) }.returns(allMessagesAccount)
        every { SearchAccount.createUnifiedInboxAccount(any()) }.returns(unifiedInboxAccount)
        presenter.init(view)
        presenter.updateAccount(account1)
        val listenerSlot = slot<ActivityListener>()
        every { controller.addListener(capture(listenerSlot)) }.answers {
            activityListener = listenerSlot.captured
        }
        every { controller.removeListener(any()) }.just(runs)
    }

    @After
    fun tearDown() {
        unmockkStatic(SearchAccount::class)
    }

    @Test
    fun `loadNavigationView() sets up the main account view and listeners`() {
        presenter.loadNavigationView()


        verify { view.setUpMainAccountView(account1) }
        verify { view.setupNavigationHeaderListeners(false) }
        verify { view.setupAccountsListeners(account1, arrayListOf(account2)) }
    }

    @Test
    fun `loadNavigationView() sets folders adapter`() {
        presenter.loadNavigationView()


        verify { account1.remoteStore }
        verify { remoteStore.pathDelimiter }
        verify { view.setFolderAdapter(any()) }
    }

    @Test
    fun `loadNavigationView() creates folders menu`() {
        presenter.loadNavigationView()


        verify { view.setupNavigationHeaderListeners(false) }
        verify { view.setFoldersDrawerVisible() }
        verify { view.setFolderAdapter(any()) }
        verify { SearchAccount.createAllMessagesAccount(context) }
        verify { SearchAccount.createUnifiedInboxAccount(context) }
    }

    @Test
    fun `loadNavigationView() uses MessagingController to list folders if they are not set yet`() {
        presenter.loadNavigationView()


        verify { controller.listFolders(account1, false, any()) }
        verify { view.populateFolders(account1, listOf(folder1), true) }
        verify { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify { accountUtils.loadSearchAccountStats(context, unifiedInboxAccount, any()) }
        verify { accountUtils.loadSearchAccountStats(context, allMessagesAccount, any()) }
    }

    @Test
    fun `loadNavigationView() sets navigation view insets`() {
        presenter.loadNavigationView()


        verify { view.setNavigationViewInsets() }
    }

    @Test
    fun `changeAccount() sets up the navigation header with the new account`() {
        presenter.changeAccount(account2)


        verify { view.setUpMainAccountView(account2) }
        verify { view.setupNavigationHeaderListeners(false) }
        verify { view.setupAccountsListeners(account2, arrayListOf(account1)) }
    }

    @Test
    fun `changeAccount() refreshes messages for the account auto-expand folder name`() {
        presenter.changeAccount(account2)


        verify { account2.autoExpandFolderName }
        verify { account2.uuid }
        verify { view.refreshMessages(any()) }
    }

    @Test
    fun `onAccountClicked() refreshes messages for the account auto-expand folder name`() {
        presenter.onAccountClicked(account2)


        verify { account2.autoExpandFolderName }
        verify { account2.uuid }
        verify { view.refreshMessages(any()) }
    }

    @Test
    fun `onAccountClicked() sets up the navigation header with the new account`() {
        presenter.onAccountClicked(account2)


        verify { view.setUpMainAccountView(account2) }
        verify { view.setupNavigationHeaderListeners(false) }
        verify { view.setupAccountsListeners(account2, arrayListOf(account1)) }
    }

    @Test
    fun `onAccountClicked() creates folders menu`() {
        presenter.onAccountClicked(account2)


        verify { view.setupNavigationHeaderListeners(false) }
        verify { view.setFoldersDrawerVisible() }
        verify { SearchAccount.createAllMessagesAccount(context) }
        verify { SearchAccount.createUnifiedInboxAccount(context) }
    }

    @Test
    fun `onAccountClicked() uses MessagingController to list folders if they are not set yet`() {
        presenter.onAccountClicked(account2)


        verify { controller.listFolders(account2, false, any()) }
        verify { view.populateFolders(account2, listOf(folder1), true) }
        verify { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify { accountUtils.loadSearchAccountStats(context, unifiedInboxAccount, any()) }
        verify { accountUtils.loadSearchAccountStats(context, allMessagesAccount, any()) }
    }

    @Test
    fun `createFoldersMenu() uses MessagingController to list folders if they are not set yet`() {
        presenter.updateAccount(account2)
        presenter.createFoldersMenu()


        verify { controller.listFolders(account2, false, any()) }
        verify { view.populateFolders(account2, listOf(folder1), true) }
        verify { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify { accountUtils.loadSearchAccountStats(context, unifiedInboxAccount, any()) }
        verify { accountUtils.loadSearchAccountStats(context, allMessagesAccount, any()) }
    }

    @Test
    fun `createFoldersMenu() forces set current folders to the view if they were already set`() {
        every { folder1.accountUuid }.returns(ACCOUNT2_UUID)


        presenter.updateAccount(account2)
        presenter.createFoldersMenu()
        // here MessagingController list folders and they are assigned
        verify(exactly = 1) { controller.listFolders(account2, false, any()) }


        presenter.createFoldersMenu()


        verify(exactly = 2) { view.populateFolders(account2, listOf(folder1), true) }
        verify(exactly = 2) { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                unifiedInboxAccount,
                any()
            )
        }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                allMessagesAccount,
                any()
            )
        }
    }

    @Test
    fun `startListeningToFolderChanges() adds the listener to MessagingController`() {
        presenter.startListeningToFolderChanges()


        verify { controller.addListener(any()) }
    }

    @Test
    fun `startListeningToFolderChanges() uses MessagingController to list folders if they are not set yet`() {
        presenter.startListeningToFolderChanges()


        verify { controller.addListener(activityListener) }
        verify { controller.listFolders(account1, false, any()) }
        verify { view.populateFolders(account1, listOf(folder1), true) }
        verify { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify { accountUtils.loadSearchAccountStats(context, unifiedInboxAccount, any()) }
        verify { accountUtils.loadSearchAccountStats(context, allMessagesAccount, any()) }
    }

    @Test
    fun `stopListeningToFolderChanges() removes the listener from MessagingController`() {
        presenter.startListeningToFolderChanges()
        presenter.stopListeningToFolderChanges()


        verify { controller.addListener(activityListener) }
        verify { controller.removeListener(activityListener) }
    }

    @Test
    fun `ActivityListener_informUserOfStatus() uses MessagingController to list folders if the account was changed`() {
        presenter.startListeningToFolderChanges()


        // here MessagingController list folders and they are assigned
        verify { controller.addListener(activityListener) }
        verify(exactly = 1) { controller.listFolders(account1, false, any()) }
        verify(exactly = 1) { view.populateFolders(account1, listOf(folder1), true) }


        presenter.updateAccount(account2)
        activityListener.informUserOfStatus()


        verify(exactly = 1) { controller.listFolders(account2, false, any()) }
        verify(exactly = 1) { view.populateFolders(account2, listOf(folder1), true) }

        verify(exactly = 2) { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                unifiedInboxAccount,
                any()
            )
        }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                allMessagesAccount,
                any()
            )
        }
    }

    @Test
    fun `ActivityListener_informUserOfStatus() just populatews folders if the account was not changed and the folders are already set`() {
        presenter.startListeningToFolderChanges()


        // here MessagingController list folders and they are assigned
        verify { controller.addListener(activityListener) }
        verify(exactly = 1) { controller.listFolders(account1, false, any()) }
        verify(exactly = 1) { view.populateFolders(account1, listOf(folder1), true) }


        activityListener.informUserOfStatus()


        verify(exactly = 0) { controller.listFolders(account2, false, any()) }
        verify(exactly = 1) { view.populateFolders(account1, listOf(folder1), false) }

        verify(exactly = 2) { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                unifiedInboxAccount,
                any()
            )
        }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                allMessagesAccount,
                any()
            )
        }
    }


    @Test
    fun `createAccountsMenu() sets navigation header listeners`() {
        presenter.createAccountsMenu()


        verify { view.setupNavigationHeaderListeners(true) }
        verify { view.setAccountsDrawerVisible() }
    }

    @Test
    fun `createAccountsMenu() sets account adapter`() {
        presenter.createAccountsMenu()


        verify { preferences.availableAccounts }
        verify { view.setAccountsAdapter(listOf(account2)) }
    }

    @Test
    fun `layoutClick() returns false if layoutClick() was not called`() {
        assertFalse(presenter.layoutClick())
    }

    @Test
    fun `layoutClick() returns true if layoutClick() was already called`() {
        assertFalse(presenter.layoutClick())
        assertTrue(presenter.layoutClick())
    }

    @Test
    fun `layoutClick() returns false if layoutClick() was already called and reset`() {
        assertFalse(presenter.layoutClick())
        presenter.resetLayoutClick()
        assertFalse(presenter.layoutClick())
    }

    @Test
    fun `refreshFolders() uses MessagingController to list folders every time`() {
        presenter.refreshFolders()
        presenter.refreshFolders()


        verify(exactly = 2) { controller.listFolders(account1, false, any()) }
        verify(exactly = 2) { view.populateFolders(account1, listOf(folder1), true) }
        verify(exactly = 2) { view.setupMainFolders(unifiedInboxAccount, allMessagesAccount) }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                unifiedInboxAccount,
                any()
            )
        }
        verify(exactly = 2) {
            accountUtils.loadSearchAccountStats(
                context,
                allMessagesAccount,
                any()
            )
        }
    }
}