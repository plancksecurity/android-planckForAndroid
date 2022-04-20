package com.fsck.k9.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.*
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.activity.accountlist.AccountListAdapter
import com.fsck.k9.activity.accountlist.DragAndDropProvider
import com.fsck.k9.activity.accountlist.ItemReleasedListener
import com.fsck.k9.activity.misc.NonConfigurationInstance
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.SizeFormatter
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.pEp.PEpImporterActivity
import com.fsck.k9.pEp.ui.listeners.indexedFolderClickListener
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.preferences.SettingsExporter
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.SearchSpecification.Attribute
import com.fsck.k9.search.SearchSpecification.SearchField
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.account.AccountSettingsActivity.Companion.ACTIVITY_REQUEST_ACCOUNT_SETTINGS
import com.fsck.k9.ui.settings.account.AccountSettingsActivity.Companion.EXTRA_ACCOUNT_DELETED
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity
import com.fsck.k9.ui.settings.general.GeneralSettingsFragment
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.*
import security.pEp.permissions.PermissionChecker
import security.pEp.permissions.PermissionRequester
import security.pEp.ui.about.AboutActivity
import security.pEp.ui.intro.startWelcomeMessage
import security.pEp.ui.keyimport.KeyImportActivity.Companion.ANDROID_FILE_MANAGER_MARKET_URL
import security.pEp.ui.keyimport.KeyImportActivity.Companion.showImportKeyDialog
import security.pEp.ui.resources.ResourcesProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


class SettingsActivity : PEpImporterActivity(),
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    ItemReleasedListener {

    private var controller: MessagingController? = null

    /*
     * Must be serializable hence implementation class used for declaration.
     */
    private val accountStats = ConcurrentHashMap<String, AccountStats>()

    private val pendingWork = ConcurrentHashMap<BaseAccount, String>()

    private var selectedContextAccount: BaseAccount? = null

    private val handler = AccountsHandler()
    private var adapter: AccountListAdapter? = null
    private var allMessagesAccount: SearchAccount? = null
    private var unifiedInboxAccount: SearchAccount? = null
    private val fontSizes = K9.getFontSizes()

    private var refreshMenuItem: MenuItem? = null

    private var exportGlobalSettings: Boolean = false
    private var exportAccountUuids: ArrayList<String>? = null

    /**
     * This flag is true when messages of the deleted account were being diplayed in MessageList. (Wether it was done as Inbox or Unified Inbox).
     */
    private var anyAccountWasDeleted = false

    /**
     * Contains information about objects that need to be retained on configuration changes.
     *
     * @see .onRetainCustomNonConfigurationInstance
     */
    private var nonConfigurationInstance: NonConfigurationInstance? = null
    private lateinit var accountsList: RecyclerView
    private lateinit var addAccountButton: View

    @Inject
    lateinit var permissionRequester: PermissionRequester

    @Inject
    lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var resourcesProvider: ResourcesProvider

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var dragAndDropProvider: DragAndDropProvider


    private val storageListener = object : StorageManager.StorageListener {

        override fun onUnmount(providerId: String) {
            refresh()
        }

        override fun onMount(providerId: String) {
            refresh()
        }
    }

    /**
     * Save the reference to a currently displayed dialog or a running AsyncTask (if available).
     */

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        var retain: Any? = null
        if (nonConfigurationInstance?.retain() == true) {
            retain = nonConfigurationInstance
        }
        return retain
    }

    private val accounts = ArrayList<Account>()

    internal inner class AccountsHandler : Handler() {
        internal fun setViewTitle() {
            toolbar.setTitle(R.string.action_settings)
        }

        fun refreshTitle() {
            runOnUiThread { setViewTitle() }
        }

        fun dataChanged() {
            runOnUiThread {
                if (adapter != null) {
                    adapter!!.notifyDataSetChanged()
                }
            }
        }

        fun workingAccount(account: Account, res: Int) {
            runOnUiThread {
                val toastText = getString(res, account.description)
                FeedbackTools.showShortFeedback(accountsList, toastText)
            }
        }

        @SuppressLint("StringFormatMatches")
        fun accountSizeChanged(account: Account, oldSize: Long, newSize: Long) {
            runOnUiThread {
                val stats = accountStats[account.uuid]
                if (newSize != (-1).toLong() && stats != null && K9.measureAccounts()) {
                    stats.size = newSize
                }
                val toastText = getString(R.string.account_size_changed, account.description,
                        SizeFormatter.formatSize(application, oldSize), SizeFormatter.formatSize(application, newSize))
                FeedbackTools.showLongFeedback(accountsList, toastText)
                if (adapter != null) {
                    adapter!!.notifyDataSetChanged()
                }
            }
        }

        fun progress(progress: Boolean) {
            // Make sure we don't try this before the menu is initialized
            // this could happen while the activity is initialized.
            if (refreshMenuItem == null) {
                return
            }

            runOnUiThread {
                if (progress) {
                    refreshMenuItem!!.setActionView(R.layout.actionbar_indeterminate_progress_actionview)
                } else {
                    refreshMenuItem!!.actionView = null
                }
            }

        }

        fun progress(progress: Int) {
            runOnUiThread { window.setFeatureInt(Window.FEATURE_PROGRESS, progress) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = MessagingController.getInstance(applicationContext)

        bindViews(R.layout.accounts)
        accountsList = findViewById(R.id.accounts_list)
        if (!K9.isHideSpecialAccounts()) {
            createSpecialAccounts()
        }

        val accounts = preferences.accounts
        if(accounts.size > 0) {
            createComposeDynamicShortcut()
        }

        // TODO: 04/08/2020 Relocate, it is here because it does not work on SplashActivity
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val app = application as K9
            app.pEpInitSyncEnvironment()
        }

        val intent = intent
        //onNewIntent(intent);

        // see if we should show the welcome message
        if (ACTION_IMPORT_SETTINGS == intent.action) {
            onSettingsImport()
        } else if (accounts.size < 1) {

            startWelcomeMessage()
            finish()
            return
        }

        if (UpgradeDatabases.actionUpgradeDatabases(this, intent)) {
            finish()
            return
        }


        val startup = intent.getBooleanExtra(EXTRA_STARTUP, true)
        if (startup && K9.startIntegratedInbox() && !K9.isHideSpecialAccounts()) {
            onOpenAccount(unifiedInboxAccount)
            finish()
            return
        } else if (startup && accounts.size > 0
                && onOpenAccount(preferences.defaultAccount)) {
            finish()
            return
        }


        initializeActionBar()

        if (savedInstanceState == null) {
            fragmentTransaction {
                add(R.id.generalSettingsContainer, GeneralSettingsFragment.create())
            }
        }

        registerForContextMenu(accountsList)

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_CONTEXT_ACCOUNT)) {
            val accountUuid = savedInstanceState.getString("selectedContextAccount")
            selectedContextAccount = preferences.getAccount(accountUuid)
        }

        restoreAccountStats(savedInstanceState)
        handler.setViewTitle()

        // Handle activity restarts because of a configuration change (e.g. rotating the screen)
        nonConfigurationInstance = lastCustomNonConfigurationInstance as NonConfigurationInstance?
        if (nonConfigurationInstance != null) {
            nonConfigurationInstance!!.restore(this)
        }

        setupAddAccountButton()
    }

    override fun search(query: String) {
        triggerSearch(query, null)
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private fun setupAddAccountButton() {
        addAccountButton = findViewById(R.id.add_account_container)
        addAccountButton.setOnClickListener { onAddNewAccount() }
    }

    private fun initializeActionBar() {
        //setUpToolbar(false)
        //View customView = getToolbar().findViewById(R.id.actionbar_custom);
        //setStatusBarPepColor(Rating.pEpRatingFullyAnonymous)

        setUpToolbar(true)
        val actionBar = supportActionBar ?: throw RuntimeException("getSupportActionBar() == null")
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Creates and initializes the special accounts ('Unified Inbox' and 'All Messages')
     */
    private fun createSpecialAccounts() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this)
        allMessagesAccount = SearchAccount.createAllMessagesAccount(this)
    }

    private fun restoreAccountStats(icicle: Bundle?) {
        if (icicle != null) {
            val oldStats = icicle.get(ACCOUNT_STATS) as Map<String, AccountStats>
            if (oldStats != null) {
                accountStats.putAll(oldStats)
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (selectedContextAccount != null) {
            outState.putString(SELECTED_CONTEXT_ACCOUNT, selectedContextAccount!!.uuid)
        }
        outState.putSerializable(ACCOUNT_STATS, accountStats)

        outState.putBoolean(STATE_EXPORT_GLOBAL_SETTINGS, exportGlobalSettings)
        outState.putStringArrayList(STATE_EXPORT_ACCOUNTS, exportAccountUuids)
        outState.putString(CURRENT_ACCOUNT_UUID, currentAccountUuid)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)

        exportGlobalSettings = state.getBoolean(STATE_EXPORT_GLOBAL_SETTINGS, false)
        exportAccountUuids = state.getStringArrayList(STATE_EXPORT_ACCOUNTS)
        currentAccountUuid = state.getString(CURRENT_ACCOUNT_UUID)
    }

    public override fun onResume() {
        super.onResume()
        refresh()
        StorageManager.getInstance(application).addListener(storageListener)
    }

    public override fun onPause() {
        super.onPause()
        StorageManager.getInstance(application).removeListener(storageListener)
        dragAndDropProvider.onPause()
    }

    private enum class ACCOUNT_LOCATION {
        TOP, MIDDLE, BOTTOM
    }

    private fun accountLocation(account: BaseAccount?): EnumSet<ACCOUNT_LOCATION> {
        val accountLocation = EnumSet.of(ACCOUNT_LOCATION.MIDDLE)
        if (accounts.size > 0) {
            if (accounts[0] == account) {
                accountLocation.remove(ACCOUNT_LOCATION.MIDDLE)
                accountLocation.add(ACCOUNT_LOCATION.TOP)
            }
            if (accounts[accounts.size - 1] == account) {
                accountLocation.remove(ACCOUNT_LOCATION.MIDDLE)
                accountLocation.add(ACCOUNT_LOCATION.BOTTOM)
            }
        }
        return accountLocation
    }

    public override fun refresh() {
        accounts.clear()
        accounts.addAll(preferences.accounts)

        if (accounts.size < 1) {
            removeComposeDynamicShortcut()
            AccountSetupBasics.actionNewAccount(this)
            finishAffinity()
            return
        }

        val newAccounts: MutableList<BaseAccount>
        if (!K9.isHideSpecialAccounts() && accounts.size > 0) {
            if (unifiedInboxAccount == null || allMessagesAccount == null) {
                createSpecialAccounts()
            }

            newAccounts = ArrayList(accounts.size + SPECIAL_ACCOUNTS_COUNT)
            unifiedInboxAccount?.let { newAccounts.add(it) }
            allMessagesAccount?.let { newAccounts.add(it) }
        } else {
            newAccounts = ArrayList(accounts.size)
        }

        newAccounts.addAll(accounts)

        initList();

        val folders = ArrayList<BaseAccount>(SPECIAL_ACCOUNTS_COUNT)

        if (!K9.isHideSpecialAccounts() && accounts.size > 0) {
            unifiedInboxAccount?.let { folders.add(it) }
            allMessagesAccount?.let { folders.add(it) }
        }

        if (newAccounts.isNotEmpty()) {
            handler.progress(Window.PROGRESS_START)
        }
        pendingWork.clear()
        handler.refreshTitle()

        for (account in newAccounts) {
            pendingWork[account] = "true"

        }

    }

    private fun initList() {
        adapter = AccountListAdapter(accounts,
                indexedFolderClickListener { position ->
                    val account = adapter?.getItem(position)
                    onEditAccount(account as Account)
                },
                resourcesProvider,
                fontSizes,
                accountStats
        )
        val layoutManager = LinearLayoutManager(this);
        accountsList.layoutManager = layoutManager;
        accountsList.adapter = adapter
        dragAndDropProvider.initialize(accountsList, accounts, this)
    }

    private fun onAddNewAccount() {
        AccountSetupBasics.actionNewAccount(this)
    }

    private fun onClearCommands(account: Account) {
        MessagingController.getInstance(application).clearAllPending(account)
        selectedContextAccount = null
    }

    private fun onEmptyTrash(account: Account) {
        MessagingController.getInstance(application).emptyTrash(account, null)
        selectedContextAccount = null
    }

    /**
     * Show that account's inbox or folder-list
     * or return false if the account is not available.
     *
     * @param account the account to open ([SearchAccount] or [Account])
     * @return false if unsuccessful
     */
    private fun onOpenAccount(account: BaseAccount?): Boolean {
        account?: return false
        if (account is SearchAccount) {
            openSearchAccount(account)
        } else {
            val realAccount = account as Account
            if(!accountWasOpenable(realAccount)) return false
            openAccount(realAccount)
        }
        return true
    }

    private fun openAccount(realAccount: Account) {
        if (K9.FOLDER_NONE == realAccount.autoExpandFolderName) {
            FolderList.actionHandleAccount(this, realAccount)
        } else {
            val search = LocalSearch(realAccount.autoExpandFolderName)
            search.addAllowedFolder(realAccount.autoExpandFolderName)
            search.addAccountUuid(realAccount.uuid)
            MessageList.actionDisplaySearch(this, search, false, true)
        }
    }

    private fun openSearchAccount(searchAccount: SearchAccount?) {
        MessageList.actionDisplaySearch(this, searchAccount!!.relatedSearch, false, false)
    }

    private fun accountWasOpenable(realAccount: Account): Boolean {
        if (!realAccount.isEnabled) {
            onActivateAccount(realAccount)
            return false
        } else if (!realAccount.isAvailable(this)) {
            val toastText = getString(R.string.account_unavailable, realAccount.description)
            FeedbackTools.showShortFeedback(accountsList, toastText)
            Timber.i("refusing to open account that is not available")
            return false
        }
        return true
    }


    private fun onActivateAccount(account: Account) {
        val disabledAccounts = ArrayList<Account>()
        disabledAccounts.add(account)
        promptForServerPasswords(disabledAccounts)
        selectedContextAccount = null
    }

    /**
     * Ask the user to enter the server passwords for disabled accounts.
     *
     * @param disabledAccounts A non-empty list of [Account]s to ask the user for passwords. Never
     * `null`.
     *
     * **Note:** Calling this method will modify the supplied list.
     */
    private fun promptForServerPasswords(disabledAccounts: MutableList<Account>) {
        val account = disabledAccounts.removeAt(0)
        val dialog = PasswordPromptDialog(account, disabledAccounts)
        setNonConfigurationInstance(dialog)
        dialog.show(this)
    }

    override fun onImportFinished() {

    }

    private fun onEditAccount(account: Account) {
        openSettingActivity( account.uuid)
        selectedContextAccount = null
    }

    private fun openSettingActivity(uuid: String) {
        AccountSettingsActivity.start(this@SettingsActivity, uuid)
    }

    @SuppressLint("StringFormatMatches")
    public override fun onCreateDialog(id: Int): Dialog? {
        // Android recreates our dialogs on configuration changes even when they have been
        // dismissed. Make sure we have all information necessary before creating a new dialog.
        when (id) {
            DIALOG_CLEAR_ACCOUNT -> {
                return if (selectedContextAccount == null) {
                    null
                } else ConfirmationDialog.create(this, id,
                        R.string.account_clear_dlg_title,
                        getString(R.string.account_clear_dlg_instructions_fmt,
                                selectedContextAccount!!.description),
                        R.string.okay_action,
                        R.string.cancel_action
                ) {
                    if (selectedContextAccount is Account) {
                        val realAccount = selectedContextAccount as Account?
                        if (realAccount != null) {
                            handler.workingAccount(realAccount,
                                    R.string.clearing_account)

                            MessagingController.getInstance(application)
                                    .clear(realAccount, null)
                        }
                    }
                    selectedContextAccount = null
                }

            }
            DIALOG_RECREATE_ACCOUNT -> {
                return if (selectedContextAccount == null) {
                    null
                } else ConfirmationDialog.create(this, id,
                        R.string.account_recreate_dlg_title,
                        getString(R.string.account_recreate_dlg_instructions_fmt,
                                selectedContextAccount!!.description),
                        R.string.okay_action,
                        R.string.cancel_action
                ) {
                    if (selectedContextAccount is Account) {
                        val realAccount = selectedContextAccount as Account?
                        if (realAccount != null) {
                            handler.workingAccount(realAccount,
                                    R.string.recreating_account)
                            MessagingController.getInstance(application)
                                    .recreate(realAccount, null)
                        }
                    }
                    selectedContextAccount = null
                }

            }
            DIALOG_NO_FILE_MANAGER -> {
                return ConfirmationDialog.create(this, id,
                        R.string.import_dialog_error_title,
                        getString(R.string.import_dialog_error_message),
                        R.string.open_market,
                        R.string.close
                ) {
                    val uri = Uri.parse(ANDROID_FILE_MANAGER_MARKET_URL)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    selectedContextAccount = null
                }
            }
        }

        return super.onCreateDialog(id)
    }

    @SuppressLint("StringFormatMatches")
    public override fun onPrepareDialog(id: Int, d: Dialog) {
        val alert = d as AlertDialog
        when (id) {
            DIALOG_CLEAR_ACCOUNT -> {
                alert.setMessage(getString(R.string.account_clear_dlg_instructions_fmt,
                        selectedContextAccount!!.description))
            }
            DIALOG_RECREATE_ACCOUNT -> {
                alert.setMessage(getString(R.string.account_recreate_dlg_instructions_fmt,
                        selectedContextAccount!!.description))
            }
            else -> {
            }
        }

        super.onPrepareDialog(id, d)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterContextMenuInfo?
        // submenus don't actually set the menuInfo, so the "advanced"
        // submenu wouldn't work.
        if (menuInfo != null) {
            selectedContextAccount = adapter?.getItem(menuInfo.position)
        }
        if (selectedContextAccount is Account) {
            val realAccount = selectedContextAccount as Account?
            realAccount?.let {
                when (item.itemId) {
                    R.id.account_settings -> onEditAccount(it)
                    R.id.activate -> onActivateAccount(it)
                    R.id.clear_pending -> onClearCommands(it)
                    R.id.empty_trash -> onEmptyTrash(it)
                    R.id.clear -> onClear(it)
                    R.id.recreate -> onRecreate(it)
                    R.id.export -> onExport(false, it)
                    R.id.import_PGP_key_from_SD -> onImportPGPKeyFromFileSystem(it)
                }
            }

        }
        return true
    }

    private fun onImportPGPKeyFromFileSystem(realAccount: Account) {
        currentAccountUuid = realAccount.uuid
        showImportKeyDialog(this, currentAccountUuid)

    }

    private fun onClear(account: Account) {
        showDialog(DIALOG_CLEAR_ACCOUNT)

    }

    private fun onRecreate(account: Account) {
        showDialog(DIALOG_RECREATE_ACCOUNT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.about -> onAbout()
            R.id.export_all -> onExport(true, null)
            R.id.import_settings -> onSettingsImport()
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun onAbout() {
        AboutActivity.onAbout(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.accounts_option, menu)
        refreshMenuItem = menu.findItem(R.id.check_mail)
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        Timber.d("onCreateContextMenu", "true")
        menu.setHeaderTitle(R.string.accounts_context_menu_title)

        val info = menuInfo as AdapterContextMenuInfo
        val account = adapter!!.getItem(info.position)

        if (account is Account && !account.isEnabled) {
            menuInflater.inflate(R.menu.disabled_accounts_context, menu)
        } else {
            menuInflater.inflate(R.menu.accounts_context, menu)
        }

        if (account is SearchAccount) {
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                item.isVisible = false
            }
        } else {
            val accountLocation = accountLocation(account)
            menu.findItem(R.id.move_up).isEnabled = !accountLocation.contains(ACCOUNT_LOCATION.TOP)
            menu.findItem(R.id.move_down).isEnabled = !accountLocation.contains(ACCOUNT_LOCATION.BOTTOM)
        }
        val app: K9 = applicationContext as K9
        if (app.isGrouped) {
            menu.findItem(R.id.import_PGP_key_from_SD)?.isEnabled = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return
        if (data == null) {
            return
        }
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_SETTINGS_FILE -> onImport(data.data)
            ACTIVITY_REQUEST_SAVE_SETTINGS_FILE -> onExport(data)
            ACTIVITY_REQUEST_ACCOUNT_SETTINGS ->
                anyAccountWasDeleted = data.getBooleanExtra(EXTRA_ACCOUNT_DELETED,false)
        }
    }

    override fun onImport(uri: Uri?) {
        val asyncTask = ListImportContentsAsyncTask(this, uri)
        setNonConfigurationInstance(asyncTask)
        asyncTask.execute()
    }

    override fun itemReleased() {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch {
            reorderAccounts()
        }
    }

    private suspend fun reorderAccounts() = withContext(Dispatchers.IO) {
        preferences.reorderAccounts(accounts)
    }

    /**
     * Set the `NonConfigurationInstance` this activity should retain on configuration
     * changes.
     *
     * @param inst The [NonConfigurationInstance] that should be retained when
     * [SettingsActivity.onRetainCustomNonConfigurationInstance] is called.
     */
    override fun setNonConfigurationInstance(inst: NonConfigurationInstance?) {
        nonConfigurationInstance = inst
    }

    fun onExport(includeGlobals: Boolean, account: Account?) {

        permissionRequester.requestStoragePermission(
                rootView,
                object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        val permissionDenied = resources.getString(R.string.download_snackbar_permission_permanently_denied)
                        FeedbackTools.showLongFeedback(rootView, permissionDenied)
                    }

                }
        )

        if (permissionChecker.hasWriteExternalPermission()) {
            // TODO, prompt to allow a user to choose which accounts to export
            var accountUuids: ArrayList<String>? = null
            if (account != null) {
                accountUuids = ArrayList()
                accountUuids.add(account.uuid)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                exportGlobalSettings = includeGlobals
                exportAccountUuids = accountUuids

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

                intent.type = "application/octet-stream"
                intent.putExtra(Intent.EXTRA_TITLE, SettingsExporter.generateDatedExportFileName())

                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, ACTIVITY_REQUEST_SAVE_SETTINGS_FILE)

            } else {
                //Pre-Kitkat
                startExport(includeGlobals, accountUuids, null)
            }
        }
    }

    fun onExport(intent: Intent) {
        val documentsUri = intent.data
        startExport(exportGlobalSettings, exportAccountUuids, documentsUri)
    }

    private fun startExport(exportGlobalSettings: Boolean, exportAccountUuids: ArrayList<String>?, documentsUri: Uri?) {
        val asyncTask = ExportAsyncTask(this, exportGlobalSettings, exportAccountUuids, documentsUri)
        setNonConfigurationInstance(asyncTask)
        asyncTask.execute()
    }

    companion object {

        /**
         * URL used to open Android Market application
         */

        /**
         * Number of special accounts ('Unified Inbox' and 'All Messages')
         */
        private const val SPECIAL_ACCOUNTS_COUNT = 2

        private const val DIALOG_CLEAR_ACCOUNT = 2
        private const val DIALOG_RECREATE_ACCOUNT = 3
        private const val DIALOG_NO_FILE_MANAGER = 4


        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1
        private const val ACTIVITY_REQUEST_SAVE_SETTINGS_FILE = 2

        private const val ACCOUNT_STATS = "accountStats"
        private const val SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount"
        private const val STATE_EXPORT_GLOBAL_SETTINGS = "exportGlobalSettings"
        private const val STATE_EXPORT_ACCOUNTS = "exportAccountUuids"
        const val EXTRA_STARTUP = "startup"


        const val ACTION_IMPORT_SETTINGS = "importSettings"

        fun listAccountsOnStartup(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(EXTRA_STARTUP, true)
            context.startActivity(intent)
        }

        fun listAccounts(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(EXTRA_STARTUP, false)
            context.startActivity(intent)
        }

        fun importSettings(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.action = ACTION_IMPORT_SETTINGS
            context.startActivity(intent)
        }

        @SuppressLint("StringFormatInvalid")
        fun createUnreadSearch(context: Context, account: BaseAccount): LocalSearch {
            val searchTitle = context.getString(R.string.search_title, account.description,
                    context.getString(R.string.unread_modifier))

            val search: LocalSearch
            if (account is SearchAccount) {
                search = account.relatedSearch.clone()
                search.name = searchTitle
            } else {
                search = LocalSearch(searchTitle)
                search.addAccountUuid(account.uuid)

                val realAccount = account as Account
                realAccount.excludeSpecialFolders(search)
                realAccount.limitToDisplayableFolders(search)
            }

            search.and(SearchField.READ, "1", Attribute.NOT_EQUALS)

            return search
        }

        @JvmStatic
        fun launch(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            intent.putExtra(EXTRA_STARTUP, false)
            activity.startActivity(intent)

        }

        fun actionBasicStart(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        GeneralSettingsActivity.start(this, preferenceScreen.key)

        return true
    }

    override fun onBackPressed() {
        if(anyAccountWasDeleted) {
            if (K9.startIntegratedInbox() && !K9.isHideSpecialAccounts()) {
                finishAffinity()
                openSearchAccount(unifiedInboxAccount)
            } else {
                val defaultAccount = preferences.defaultAccount
                if(accountWasOpenable(defaultAccount)) {
                    finishAffinity()
                    openAccount(defaultAccount)
                }
            }
            anyAccountWasDeleted = false
        } else {
            super.onBackPressed()
        }
    }

    private fun createComposeDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val composeIntent = MessageActions.getDefaultComposeShortcutIntent(this)
            val composeShortcut = ShortcutInfo.Builder(this, MessageCompose.SHORTCUT_COMPOSE)
                .setShortLabel(resources.getString(R.string.compose_action))
                .setLongLabel(resources.getString(R.string.compose_action))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_compose))
                .setIntent(composeIntent)
                .build()
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            shortcutManager.dynamicShortcuts = listOf(composeShortcut)
        }
    }

    private fun removeComposeDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            shortcutManager.removeDynamicShortcuts(listOf(MessageCompose.SHORTCUT_COMPOSE))
        }
    }
}

