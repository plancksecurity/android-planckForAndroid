package com.fsck.k9.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnClickListener
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.fsck.k9.*
import com.fsck.k9.activity.misc.NonConfigurationInstance
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.SizeFormatter
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.pEp.PEpImporterActivity
import com.fsck.k9.pEp.ui.listeners.OnBaseAccountClickListener
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.pEp.ui.tools.NestedListView
import com.fsck.k9.preferences.SettingsExporter
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.SearchSpecification.Attribute
import com.fsck.k9.search.SearchSpecification.SearchField
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity
import com.fsck.k9.ui.settings.general.GeneralSettingsFragment
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.accounts.*
import kotlinx.coroutines.*
import security.pEp.permissions.PermissionChecker
import security.pEp.permissions.PermissionRequester
import security.pEp.ui.about.AboutActivity
import security.pEp.ui.intro.startWelcomeMessage
import security.pEp.ui.resources.ResourcesProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


class SettingsActivity : PEpImporterActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

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
    private var currentAccountDeleted = false

    /**
     * Contains information about objects that need to be retained on configuration changes.
     *
     * @see .onRetainCustomNonConfigurationInstance
     */
    private var nonConfigurationInstance: NonConfigurationInstance? = null
    private var accountsList: NestedListView? = null
    private var addAccountButton: View? = null

    @Inject
    lateinit var permissionRequester: PermissionRequester
    @Inject
    lateinit var permissionChecker: PermissionChecker
    @Inject
    lateinit var resourcesProvider: ResourcesProvider

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

    private val accounts = ArrayList<BaseAccount>()


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
        accountsList = findViewById<View>(R.id.accounts_list) as NestedListView
        if (!K9.isHideSpecialAccounts()) {
            createSpecialAccounts()
        }

        val accounts = Preferences.getPreferences(this).accounts
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
                && onOpenAccount(Preferences.getPreferences(this).defaultAccount)) {
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
            selectedContextAccount = Preferences.getPreferences(this).getAccount(accountUuid)
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
        addAccountButton!!.setOnClickListener { onAddNewAccount() }
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
        outState.putString(CURRENT_ACCOUNT, currentAccount)
        outState.putString(FPR, fpr)
        outState.putBoolean(SHOWING_IMPORT_DIALOG, showingImportDialog)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)

        exportGlobalSettings = state.getBoolean(STATE_EXPORT_GLOBAL_SETTINGS, false)
        exportAccountUuids = state.getStringArrayList(STATE_EXPORT_ACCOUNTS)
        currentAccount = state.getString(CURRENT_ACCOUNT)
        fpr = state.getString(FPR, "").replace(" ", "")
        if (state.getBoolean(SHOWING_IMPORT_DIALOG)) {
            onKeyImport()
        }
    }

    public override fun onResume() {
        super.onResume()

        refresh()
        StorageManager.getInstance(application).addListener(storageListener)
    }

    public override fun onPause() {
        super.onPause()
        StorageManager.getInstance(application).removeListener(storageListener)
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
        accounts.addAll(Preferences.getPreferences(this).accounts)

        if (accounts.size < 1) {
            AccountSetupBasics.actionNewAccount(this)
            finishAffinity()
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

        adapter = AccountListAdapter(accounts, object : OnFolderClickListener {
            override fun onClick(folder: LocalFolder) {

            }

            override fun onClick(position: Int?) {
                val account = accountsList!!.getItemAtPosition(position!!) as BaseAccount
                onEditAccount(account as Account)
            }
        }, OnBaseAccountClickListener { baseAccount -> AccountSettingsActivity.start(this@SettingsActivity, baseAccount.uuid) })
        accountsList!!.adapter = adapter

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
        if (account is SearchAccount) {
            val searchAccount = account as SearchAccount?
            MessageList.actionDisplaySearch(this, searchAccount!!.relatedSearch, false, false)
        } else {
            val realAccount = account as Account?
            if (!realAccount!!.isEnabled) {
                onActivateAccount(realAccount)
                return false
            } else if (!realAccount.isAvailable(this)) {
                val toastText = getString(R.string.account_unavailable, account!!.description)
                FeedbackTools.showShortFeedback(accountsList, toastText)
                Timber.i("refusing to open account that is not available")
                return false
            }
            if (K9.FOLDER_NONE == realAccount.autoExpandFolderName) {
                FolderList.actionHandleAccount(this, realAccount)
            } else {
                val search = LocalSearch(realAccount.autoExpandFolderName)
                search.addAllowedFolder(realAccount.autoExpandFolderName)
                search.addAccountUuid(realAccount.uuid)
                MessageList.actionDisplaySearch(this, search, false, true)
            }
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

    private fun onDeleteAccount(account: Account) {
        selectedContextAccount = account
        showDialog(DIALOG_REMOVE_ACCOUNT)
    }

    private fun onEditAccount(account: Account) {
        AccountSettingsActivity.start(this, account.uuid)
        selectedContextAccount = null
    }

    public override fun onCreateDialog(id: Int): Dialog? {
        // Android recreates our dialogs on configuration changes even when they have been
        // dismissed. Make sure we have all information necessary before creating a new dialog.
        when (id) {
            DIALOG_REMOVE_ACCOUNT -> {
                return if (selectedContextAccount == null) {
                    null
                } else ConfirmationDialog.create(
                        this, id, R.string.account_delete_dlg_title,
                        getString(R.string.account_delete_dlg_instructions_fmt,
                        selectedContextAccount!!.description),
                        R.string.okay_action,
                        R.string.cancel_action, this@SettingsActivity::deleteAccountWork)

            }
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
                    val uri = Uri.parse(ANDROID_MARKET_URL)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    selectedContextAccount = null
                }
            }
        }

        return super.onCreateDialog(id)
    }

    private fun deleteAccountWork() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {

            if (selectedContextAccount is Account) {
                val realAccount = selectedContextAccount as Account?
                try {
                    realAccount!!.localStore.delete()
                } catch (e: Exception) {
                    // Ignore, this may lead to localStores on sd-cards that
                    // are currently not inserted to be left
                }

                MessagingController.getInstance(application)
                        .deleteAccount(realAccount)
                Preferences.getPreferences(this@SettingsActivity)
                        .deleteAccount(realAccount)
                K9.setServicesEnabled(this@SettingsActivity)

                if(K9.startIntegratedInbox() ||
                        Preferences.getPreferences(this@SettingsActivity).accounts.indexOf(realAccount) == 0) {
                    currentAccountDeleted = true
                }
                refresh()

            }
            selectedContextAccount = null
        }
    }

    public override fun onPrepareDialog(id: Int, d: Dialog) {
        val alert = d as AlertDialog
        when (id) {
            DIALOG_REMOVE_ACCOUNT -> {
                alert.setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                        selectedContextAccount!!.description))
            }
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
            selectedContextAccount = accountsList!!.getItemAtPosition(menuInfo.position) as BaseAccount
        }
        if (selectedContextAccount is Account) {
            val realAccount = selectedContextAccount as Account?
            realAccount?.let {
                when (item.itemId) {
                    R.id.delete_account -> onDeleteAccount(it)
                    R.id.account_settings -> onEditAccount(it)
                    R.id.activate -> onActivateAccount(it)
                    R.id.clear_pending -> onClearCommands(it)
                    R.id.empty_trash -> onEmptyTrash(it)
                    R.id.clear -> onClear(it)
                    R.id.recreate -> onRecreate(it)
                    R.id.export -> onExport(false, it)
                    R.id.move_up -> onMove(it, true)
                    R.id.move_down -> onMove(it, false)
                    R.id.import_PGP_key_from_SD -> onImportPGPKeyFromFileSystem(it)
                }
            }

        }
        return true
    }

    private fun onImportPGPKeyFromFileSystem(realAccount: Account) {
        currentAccount = realAccount.email
        onKeyImport()
    }

    private fun onClear(account: Account) {
        showDialog(DIALOG_CLEAR_ACCOUNT)

    }

    private fun onRecreate(account: Account) {
        showDialog(DIALOG_RECREATE_ACCOUNT)
    }


    private suspend fun moveAccount(account: Account, up: Boolean) = withContext(Dispatchers.IO) {
        launch(Dispatchers.IO) {
            account.move(Preferences.getPreferences(applicationContext), up)
        }
        delay(600)
    }


    private fun onMove(account: Account, up: Boolean) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())


        uiScope.launch {
            // Show loading
            loading?.visibility = View.VISIBLE
            accounts_list?.alpha = 0.2f
            //Move account
            moveAccount(account, up)
            refresh()

            //Hide loading
            loading?.visibility = View.GONE
            accounts_list?.alpha = 1f

        }
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.i("onActivityResult requestCode = %d, resultCode = %s, data = %s", requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return
        if (data == null) {
            return
        }
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_SETTINGS_FILE -> onImport(data.data)
            ACTIVITY_REQUEST_SAVE_SETTINGS_FILE -> onExport(data)
            ACTIVITY_REQUEST_PICK_KEY_FILE -> onKeyImport(data.data, currentAccount)
        }
    }


    fun onKeyImport(uri: Uri?, currentAccount: String) {
        val asyncTask = ListImportContentsAsyncTask(this, uri, currentAccount, true, fpr)
        setNonConfigurationInstance(asyncTask)
        asyncTask.execute()
    }

    override fun onImport(uri: Uri?) {
        val asyncTask = ListImportContentsAsyncTask(this, uri, currentAccount, false, null)
        setNonConfigurationInstance(asyncTask)
        asyncTask.execute()
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

    private inner class AccountClickListener internal constructor(internal val search: LocalSearch) : OnClickListener {

        override fun onClick(v: View) {
            MessageList.actionDisplaySearch(this@SettingsActivity, search, true, false)
        }

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


    internal inner class AccountListAdapter(
            accounts: List<BaseAccount>,
            private val onFolderClickListener: OnFolderClickListener,
            private val onBaseAccountClickListener: OnBaseAccountClickListener
    ) : ArrayAdapter<BaseAccount>(this@SettingsActivity, 0, accounts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position)
            val view: View = if (convertView != null) {
                convertView
            } else {
                layoutInflater.inflate(R.layout.accounts_item, parent, false)
            }
            view.isLongClickable = true
            var holder: AccountViewHolder? = view.tag as AccountViewHolder?
            if (holder == null) {
                holder = AccountViewHolder()
                holder.description = view.findViewById<View>(R.id.description) as TextView
                holder.descriptionUnreadMessages = view.findViewById<View>(R.id.description_unread_messages) as TextView
                holder.email = view.findViewById<View>(R.id.email) as TextView
                holder.newMessageCount = view.findViewById<View>(R.id.new_message_count) as TextView
                holder.flaggedMessageCount = view.findViewById<View>(R.id.flagged_message_count) as TextView
                holder.newMessageCountWrapper = view.findViewById(R.id.new_message_count_wrapper)
                holder.flaggedMessageCountWrapper = view.findViewById(R.id.flagged_message_count_wrapper)
                holder.newMessageCountIcon = view.findViewById(R.id.new_message_count_icon)
                holder.flaggedMessageCountIcon = view.findViewById(R.id.flagged_message_count_icon)
                holder.activeIcons = view.findViewById<View>(R.id.active_icons) as RelativeLayout

                holder.folders = view.findViewById<View>(R.id.folders) as ImageButton
                holder.settings = view.findViewById<View>(R.id.account_settings) as ImageButton
                holder.accountsItemLayout = view.findViewById<View>(R.id.accounts_item_layout) as LinearLayout
                val accountsDescriptionLayout = view.findViewById<View>(R.id.accounts_description_layout) as LinearLayout

                view.tag = holder

                holder.accountsItemLayout!!.setOnClickListener { onFolderClickListener.onClick(position) }
            }
            val stats = accountStats[account!!.uuid]

            if (stats != null && account is Account && stats.size >= 0) {
                holder.email!!.text = SizeFormatter.formatSize(this@SettingsActivity, stats.size)
                holder.email!!.visibility = View.VISIBLE
            } else {
                if (account.email == account.description) {
                    holder.email!!.visibility = View.GONE
                } else {
                    holder.email!!.visibility = View.VISIBLE
                    holder.email!!.text = account.email
                }
            }

            var description: String? = account.description
            if (description == null || description.isEmpty()) {
                description = account.email
            }

            holder.description!!.text = description

            var unreadMessageCount: Int? = null
            if (stats != null) {
                unreadMessageCount = stats.unreadMessageCount
                holder.descriptionUnreadMessages!!.text = String.format("%d", unreadMessageCount)
                holder.newMessageCount!!.text = String.format("%d", unreadMessageCount)

                holder.flaggedMessageCount!!.text = String.format("%d", stats.flaggedMessageCount)
                holder.flaggedMessageCountWrapper!!.visibility = if (K9.messageListStars() && stats.flaggedMessageCount > 0) {
                    View.VISIBLE
                } else View.GONE

                holder.flaggedMessageCountWrapper!!.setOnClickListener(createFlaggedSearchListener(account))
                holder.newMessageCountWrapper!!.setOnClickListener(createUnreadSearchListener(account))

                holder.activeIcons!!.setOnClickListener {
                    FeedbackTools.showShortFeedback(accountsList, getString(R.string.tap_hint))
                }

            } else {
                holder.newMessageCountWrapper!!.visibility = View.GONE
                holder.flaggedMessageCountWrapper!!.visibility = View.GONE
            }

            holder.flaggedMessageCountIcon!!.setBackgroundResource(resourcesProvider.getAttributeResource(R.attr.iconFlagButton));


            fontSizes.setViewTextSize(holder.description, fontSizes.accountName)
            fontSizes.setViewTextSize(holder.email, fontSizes.accountDescription)

            if (account is SearchAccount) {
                holder.folders!!.visibility = View.GONE
            } else {
                holder.folders?.let {
                    it.visibility = View.VISIBLE
                    it.drawable.alpha = 255
                    it.setOnClickListener { FolderList.actionHandleAccount(this@SettingsActivity, account as Account) }
                }

                holder.settings?.let {
                    it.drawable.alpha = 255
                    it.setOnClickListener { onEditAccount(account as Account) }
                }
            }

            return view
        }


        private fun createFlaggedSearchListener(account: BaseAccount): OnClickListener {
            val searchTitle = getString(R.string.search_title, account.description,
                    getString(R.string.flagged_modifier))

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

            search.and(SearchField.FLAGGED, "1", Attribute.EQUALS)

            return AccountClickListener(search)
        }

        private fun createUnreadSearchListener(account: BaseAccount): OnClickListener {
            val search = createUnreadSearch(this@SettingsActivity, account)
            return AccountClickListener(search)
        }

        internal inner class AccountViewHolder {
            var description: TextView? = null
            var email: TextView? = null
            var newMessageCount: TextView? = null
            var flaggedMessageCount: TextView? = null
            var newMessageCountIcon: View? = null
            var flaggedMessageCountIcon: View? = null
            var newMessageCountWrapper: View? = null
            var flaggedMessageCountWrapper: View? = null
            var activeIcons: RelativeLayout? = null
            var chip: View? = null
            var folders: ImageButton? = null
            var settings: ImageButton? = null
            var accountsItemLayout: LinearLayout? = null
            var descriptionUnreadMessages: TextView? = null
        }
    }

    companion object {

        /**
         * URL used to open Android Market application
         */
        private const val ANDROID_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager"

        /**
         * Number of special accounts ('Unified Inbox' and 'All Messages')
         */
        private const val SPECIAL_ACCOUNTS_COUNT = 2

        private const val DIALOG_REMOVE_ACCOUNT = 1
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
        if(currentAccountDeleted) {
            if (K9.startIntegratedInbox() && !K9.isHideSpecialAccounts()) {
                if(onOpenAccount(unifiedInboxAccount)) {
                    currentAccountDeleted = false
                    finish()
                }
            } else if (onOpenAccount(Preferences.getPreferences(this@SettingsActivity).defaultAccount)) {
                currentAccountDeleted = false
                finish()
            }
        }
        else {
            super.onBackPressed()
        }
    }

}
