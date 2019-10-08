package com.fsck.k9.activity;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.misc.ExtendedAsyncTask;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.pEp.PEpImporterActivity;
import com.fsck.k9.pEp.ui.AboutActivity;
import com.fsck.k9.pEp.ui.listeners.OnBaseAccountClickListener;
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.NestedListView;
import com.fsck.k9.preferences.SettingsExporter;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.ui.settings.SettingsActivity;
import com.fsck.k9.ui.settings.account.AccountSettingsActivity;
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity;
import com.karumi.dexter.listener.single.CompositePermissionListener;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;


public class AccountsLegacy extends PEpImporterActivity {

    /**
     * URL used to open Android Market application
     */
    private static final String ANDROID_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager";

    /**
     * Number of special accounts ('Unified Inbox' and 'All Messages')
     */
    private static final int SPECIAL_ACCOUNTS_COUNT = 2;

    private static final int DIALOG_REMOVE_ACCOUNT = 1;
    private static final int DIALOG_CLEAR_ACCOUNT = 2;
    private static final int DIALOG_RECREATE_ACCOUNT = 3;
    private static final int DIALOG_NO_FILE_MANAGER = 4;

    private MessagingController controller;

    /*
     * Must be serializable hence implementation class used for declaration.
     */
    private ConcurrentHashMap<String, AccountStats> accountStats = new ConcurrentHashMap<String, AccountStats>();

    private ConcurrentMap<BaseAccount, String> pendingWork = new ConcurrentHashMap<BaseAccount, String>();

    private BaseAccount selectedContextAccount;

    private AccountsHandler handler = new AccountsHandler();
    private FoldersAdapter adapter;
    private FoldersAdapter foldersAdapter;
    private SearchAccount allMessagesAccount = null;
    private SearchAccount unifiedInboxAccount = null;
    private FontSizes fontSizes = K9.getFontSizes();

    private MenuItem refreshMenuItem;
    //private ActionBar actionBar;

    private boolean exportGlobalSettings;
    private ArrayList<String> exportAccountUuids;

    /**
     * Contains information about objects that need to be retained on configuration changes.
     *
     * @see #onRetainNonConfigurationInstance()
     */
    private NonConfigurationInstance nonConfigurationInstance;


    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    private static final int ACTIVITY_REQUEST_SAVE_SETTINGS_FILE = 2;
    private NestedListView accountsList;
    private View addAccountButton;
    private NestedListView foldersList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CompositePermissionListener storagePermissionListener;

    class AccountsHandler extends Handler {
        private void setViewTitle() {
            getToolbar().setTitle(R.string.accounts_title);

            String operation = mListener.getOperation(AccountsLegacy.this);
            operation = operation.trim();
            if (operation.length() < 1) {
                getToolbar().setSubtitle(null);
            } else {
                getToolbar().setSubtitle(operation);
            }
        }
        public void refreshTitle() {
            runOnUiThread(new Runnable() {
                public void run() {
                    setViewTitle();
                }
            });
        }

        public void dataChanged() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    if (foldersAdapter != null) {
                        foldersAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        public void workingAccount(final Account account, final int res) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(res, account.getDescription());
                    FeedbackTools.showShortFeedback(accountsList, toastText);
                }
            });
        }

        public void accountSizeChanged(final Account account, final long oldSize, final long newSize) {
            runOnUiThread(new Runnable() {
                public void run() {
                    AccountStats stats = accountStats.get(account.getUuid());
                    if (newSize != -1 && stats != null && K9.measureAccounts()) {
                        stats.size = newSize;
                    }
                    String toastText = getString(R.string.account_size_changed, account.getDescription(),
                                                 SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));
                    FeedbackTools.showLongFeedback(accountsList, toastText);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
        public void progress(final boolean progress) {
            // Make sure we don't try this before the menu is initialized
            // this could happen while the activity is initialized.
            if (refreshMenuItem == null) {
                return;
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    if (progress) {
                        refreshMenuItem.setActionView(R.layout.actionbar_indeterminate_progress_actionview);
                    } else {
                        refreshMenuItem.setActionView(null);
                    }
                }
            });

        }
        public void progress(final int progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
                }
            });
        }
    }

    public void setProgress(boolean progress) {
        handler.progress(progress);
    }

    ActivityListener mListener = new ActivityListener() {
        @Override
        public void informUserOfStatus() {
            handler.refreshTitle();
        }

        @Override
        public void folderStatusChanged(Account account, String folderServerId, int unreadMessageCount) {
            /*try {
                //TODO: FIX;
                AccountStats stats = controller.getAccountStats(account);
                if (stats == null) {
                    Timber.w("Unable to get account stats");
                } else {
                    accountStatusChanged(account, stats);
                }
            } catch (Exception e) {
                Timber.e(e, "Unable to get account stats");
            }*/
        }
        @Override
        public void accountStatusChanged(BaseAccount account, AccountStats stats) {
            AccountStats oldStats = accountStats.get(account.getUuid());
            int oldUnreadMessageCount = 0;
            if (oldStats != null) {
                oldUnreadMessageCount = oldStats.unreadMessageCount;
            }
            if (stats == null) {
                stats = new AccountStats(); // empty stats for unavailable accounts
                stats.available = false;
            }
            accountStats.put(account.getUuid(), stats);
            handler.dataChanged();
            pendingWork.remove(account);

            if (pendingWork.isEmpty()) {
                handler.progress(Window.PROGRESS_END);
                handler.refreshTitle();
            } else {
                int level = (Window.PROGRESS_END / adapter.getCount()) * (adapter.getCount() - pendingWork.size()) ;
                handler.progress(level);
            }
        }

        @Override
        public void accountSizeChanged(Account account, long oldSize, long newSize) {
            handler.accountSizeChanged(account, oldSize, newSize);
        }

        @Override
        public void synchronizeMailboxFinished(
            Account account,
            String folderServerId,
            int totalMessagesInMailbox,
        int numNewMessages) {
            MessagingController.getInstance(getApplication()).getAccountStats(AccountsLegacy.this, account, mListener);
            super.synchronizeMailboxFinished(account, folderServerId, totalMessagesInMailbox, numNewMessages);

            handler.progress(false);

        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folderName) {
            super.synchronizeMailboxStarted(account, folderName);
            handler.progress(true);
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folderServerId,
        String message) {
            super.synchronizeMailboxFailed(account, folderServerId, message);
            handler.progress(false);

        }

    };

    private static final String ACCOUNT_STATS = "accountStats";
    private static final String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";
    private static final String STATE_EXPORT_GLOBAL_SETTINGS = "exportGlobalSettings";
    private static final String STATE_EXPORT_ACCOUNTS = "exportAccountUuids";
    public static final String EXTRA_STARTUP = "startup";


    public static final String ACTION_IMPORT_SETTINGS = "importSettings";

    public static void listAccountsOnStartup(Context context) {
        Intent intent = new Intent(context, AccountsLegacy.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_STARTUP, true);
        context.startActivity(intent);
    }

    public static void listAccounts(Context context) {
        Intent intent = new Intent(context, AccountsLegacy.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_STARTUP, false);
        context.startActivity(intent);
    }

    public static void importSettings(Context context) {
        Intent intent = new Intent(context, AccountsLegacy.class);
        intent.setAction(ACTION_IMPORT_SETTINGS);
        context.startActivity(intent);
    }

    public static LocalSearch createUnreadSearch(Context context, BaseAccount account) {
        String searchTitle = context.getString(R.string.search_title, account.getDescription(),
                context.getString(R.string.unread_modifier));

        LocalSearch search;
        if (account instanceof SearchAccount) {
            search = ((SearchAccount) account).getRelatedSearch().clone();
            search.setName(searchTitle);
        } else {
            search = new LocalSearch(searchTitle);
            search.addAccountUuid(account.getUuid());

            Account realAccount = (Account) account;
            realAccount.excludeSpecialFolders(search);
            realAccount.limitToDisplayableFolders(search);
        }

        search.and(SearchField.READ, "1", Attribute.NOT_EQUALS);

        return search;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        controller = MessagingController.getInstance(getApplicationContext());

        bindViews(R.layout.accounts);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.pep_green),
                getResources().getColor(R.color.pep_yellow),
                getResources().getColor(R.color.pep_red));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onCheckMail(null);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        accountsList = (NestedListView) findViewById(R.id.accounts_list);
        foldersList = (NestedListView) findViewById(R.id.folders_list);
        if (!K9.isHideSpecialAccounts()) {
            createSpecialAccounts();
        }

        List<Account> accounts = Preferences.getPreferences(this).getAccounts();
        Intent intent = getIntent();
        //onNewIntent(intent);

        // see if we should show the welcome message
        if (ACTION_IMPORT_SETTINGS.equals(intent.getAction())) {
            onSettingsImport();
        } else if (accounts.size() < 1) {
            WelcomeMessage.showWelcomeMessage(this);
            finish();
            return;
        }

        if (UpgradeDatabases.actionUpgradeDatabases(this, intent)) {
            finish();
            return;
        }

        boolean startup = intent.getBooleanExtra(EXTRA_STARTUP, true);
        if (startup && K9.startIntegratedInbox() && !K9.isHideSpecialAccounts()) {
            onOpenAccount(unifiedInboxAccount);
            finish();
            return;
        } else if (startup && accounts.size() > 0 && onOpenAccount(accounts.get(0))) {
            finish();
            return;
        }
// TODO: 28/9/16 is this really needed?
//        requestWindowFeature(Window.FEATURE_PROGRESS);
        initializeActionBar();
        registerForContextMenu(accountsList);

        if (icicle != null && icicle.containsKey(SELECTED_CONTEXT_ACCOUNT)) {
            String accountUuid = icicle.getString("selectedContextAccount");
            selectedContextAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        restoreAccountStats(icicle);
        handler.setViewTitle();

        // Handle activity restarts because of a configuration change (e.g. rotating the screen)
        nonConfigurationInstance = (NonConfigurationInstance) getLastCustomNonConfigurationInstance();
        if (nonConfigurationInstance != null) {
            nonConfigurationInstance.restore(this);
        }

        setupAddAccountButton();
        setupSettingsButton();
        askForBatteryOptimizationWhiteListing();
    }

    @Override
    public void search(String query) {
        triggerSearch(query, null);
    }

    @Override
    public void showPermissionGranted(String permissionName) {

    }

    @Override
    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
        String permissionDenied = getResources().getString(R.string.download_snackbar_permission_permanently_denied);
        FeedbackTools.showLongFeedback(getRootView(),  permissionDenied);
    }

    @Override
    public void inject() {

    }

    private void setupSettingsButton() {
        View settingsButton = findViewById(R.id.settings_container);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditSettings();
            }
        });
    }

    private void setupAddAccountButton() {
        addAccountButton = findViewById(R.id.add_account_container);
        addAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddNewAccount();
            }
        });
    }

    private void initializeActionBar() {
        setUpToolbar(false);
        //View customView = getToolbar().findViewById(R.id.actionbar_custom);
        setStatusBarPepColor(Rating.pEpRatingFullyAnonymous);
    }

    /**
     * Creates and initializes the special accounts ('Unified Inbox' and 'All Messages')
     */
    private void createSpecialAccounts() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
        allMessagesAccount = SearchAccount.createAllMessagesAccount(this);
    }

    @SuppressWarnings("unchecked")
    private void restoreAccountStats(Bundle icicle) {
        if (icicle != null) {
            Map<String, AccountStats> oldStats = (Map<String, AccountStats>)icicle.get(ACCOUNT_STATS);
            if (oldStats != null) {
                accountStats.putAll(oldStats);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedContextAccount != null) {
            outState.putString(SELECTED_CONTEXT_ACCOUNT, selectedContextAccount.getUuid());
        }
        outState.putSerializable(ACCOUNT_STATS, accountStats);

        outState.putBoolean(STATE_EXPORT_GLOBAL_SETTINGS, exportGlobalSettings);
        outState.putStringArrayList(STATE_EXPORT_ACCOUNTS, exportAccountUuids);
        outState.putString(CURRENT_ACCOUNT, currentAccount);
        outState.putString(FPR, fpr);
        outState.putBoolean(SHOWING_IMPORT_DIALOG, showingImportDialog);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        exportGlobalSettings = state.getBoolean(STATE_EXPORT_GLOBAL_SETTINGS, false);
        exportAccountUuids = state.getStringArrayList(STATE_EXPORT_ACCOUNTS);
        currentAccount = state.getString(CURRENT_ACCOUNT);
        fpr = state.getString(FPR, "").replace(" ", "");
        if (state.getBoolean(SHOWING_IMPORT_DIALOG)) {
            onKeyImport();
        }
    }

    private StorageManager.StorageListener storageListener = new StorageManager.StorageListener() {

        @Override
        public void onUnmount(String providerId) {
            refresh();
        }

        @Override
        public void onMount(String providerId) {
            refresh();
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        refresh();
        MessagingController.getInstance(getApplication()).addListener(mListener);
        StorageManager.getInstance(getApplication()).addListener(storageListener);
        mListener.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
        StorageManager.getInstance(getApplication()).removeListener(storageListener);
        mListener.onPause(this);
    }

    /**
     * Save the reference to a currently displayed dialog or a running AsyncTask (if available).
     */
    // TODO: 28/9/16 Fix this
//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        Object retain = null;
//        if (nonConfigurationInstance != null && nonConfigurationInstance.retain()) {
//            retain = nonConfigurationInstance;
//        }
//        return retain;
//    }

    private List<BaseAccount> accounts = new ArrayList<BaseAccount>();
    private enum ACCOUNT_LOCATION {
        TOP, MIDDLE, BOTTOM;
    }
    private EnumSet<ACCOUNT_LOCATION> accountLocation(BaseAccount account) {
        EnumSet<ACCOUNT_LOCATION> accountLocation = EnumSet.of(ACCOUNT_LOCATION.MIDDLE);
        if (accounts.size() > 0) {
            if (accounts.get(0).equals(account)) {
                accountLocation.remove(ACCOUNT_LOCATION.MIDDLE);
                accountLocation.add(ACCOUNT_LOCATION.TOP);
            }
            if (accounts.get(accounts.size() - 1).equals(account)) {
                accountLocation.remove(ACCOUNT_LOCATION.MIDDLE);
                accountLocation.add(ACCOUNT_LOCATION.BOTTOM);
            }
        }
        return accountLocation;
    }

    @Override
    public void refresh() {
        accounts.clear();
        accounts.addAll(Preferences.getPreferences(this).getAccounts());

        // see if we should show the welcome message
//        if (accounts.length < 1) {
//            WelcomeMessage.showWelcomeMessage(this);
//            finish();
//        }

        List<BaseAccount> newAccounts;
        if (!K9.isHideSpecialAccounts() && accounts.size() > 0) {
            if (unifiedInboxAccount == null || allMessagesAccount == null) {
                createSpecialAccounts();
            }

            newAccounts = new ArrayList<BaseAccount>(accounts.size() +
                    SPECIAL_ACCOUNTS_COUNT);
            newAccounts.add(unifiedInboxAccount);
            newAccounts.add(allMessagesAccount);
        } else {
            newAccounts = new ArrayList<BaseAccount>(accounts.size());
        }

        newAccounts.addAll(accounts);

        adapter = new FoldersAdapter(accounts, new OnFolderClickListener() {
            @Override
            public void onClick(LocalFolder folder) {

            }

            @Override
            public void onClick(Integer position) {
                BaseAccount account = (BaseAccount)accountsList.getItemAtPosition(position);
                onOpenAccount(account);
            }
        }, new OnBaseAccountClickListener() {
            @Override
            public void onClick(BaseAccount baseAccount) {
                AccountSettingsActivity.start(AccountsLegacy.this, baseAccount.getUuid());
            }
        });
        accountsList.setAdapter(adapter);

        List<BaseAccount> folders = new ArrayList<>(SPECIAL_ACCOUNTS_COUNT);

        if (!K9.isHideSpecialAccounts() && accounts.size() > 0) {
            folders.add(unifiedInboxAccount);
            folders.add(allMessagesAccount);
        }

        foldersAdapter = new FoldersAdapter(folders, new OnFolderClickListener() {
            @Override
            public void onClick(LocalFolder folder) {

            }

            @Override
            public void onClick(Integer position) {
                BaseAccount account = foldersAdapter.getItem(position);
                onOpenAccount(account);
            }
        }, new OnBaseAccountClickListener() {
            @Override
            public void onClick(BaseAccount baseAccount) {
               GeneralSettingsActivity.Companion.start(AccountsLegacy.this);
            }
        });
        foldersList.setAdapter(foldersAdapter);
        if (!newAccounts.isEmpty()) {
            handler.progress(Window.PROGRESS_START);
        }
        pendingWork.clear();
        handler.refreshTitle();

        MessagingController controller = MessagingController.getInstance(getApplication());

        for (BaseAccount account : newAccounts) {
            pendingWork.put(account, "true");

            if (account instanceof Account) {
                Account realAccount = (Account) account;
                controller.getAccountStats(this, realAccount, mListener);
            } else if (K9.countSearchMessages() && account instanceof SearchAccount) {
                final SearchAccount searchAccount = (SearchAccount) account;
                controller.getSearchAccountStats(searchAccount, mListener);
            }
        }

        foldersAdapter.notifyDataSetChanged();
    }

    private void onAddNewAccount() {
        AccountSetupBasics.actionNewAccount(this);
    }

    private void onEditSettings() {
        SettingsActivity.launch(this);
    }


    /*
     * This method is called with 'null' for the argument 'account' if
     * all accounts are to be checked. This is handled accordingly in
     * MessagingController.checkMail().
     */
    private void onCheckMail(Account account) {
        MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, null);
        if (account == null) {
            MessagingController.getInstance(getApplication()).sendPendingMessages(null);
        } else {
            MessagingController.getInstance(getApplication()).sendPendingMessages(account, null);
        }

    }

    private void onClearCommands(Account account) {
        MessagingController.getInstance(getApplication()).clearAllPending(account);
    }

    private void onEmptyTrash(Account account) {
        MessagingController.getInstance(getApplication()).emptyTrash(account, null);
    }


    private void onCompose() {
        Account defaultAccount = Preferences.getPreferences(this).getDefaultAccount();
        if (defaultAccount != null) {
            MessageActions.actionCompose(this, defaultAccount);
        } else {
            onAddNewAccount();
        }
    }

    /**
     * Show that account's inbox or folder-list
     * or return false if the account is not available.
     * @param account the account to open ({@link SearchAccount} or {@link Account})
     * @return false if unsuccessful
     */
    private boolean onOpenAccount(BaseAccount account) {
        if (account instanceof SearchAccount) {
            SearchAccount searchAccount = (SearchAccount)account;
            MessageList.actionDisplaySearch(this, searchAccount.getRelatedSearch(), false, false);
        } else {
            Account realAccount = (Account)account;
            if (!realAccount.isEnabled()) {
                onActivateAccount(realAccount);
                return false;
            } else if (!realAccount.isAvailable(this)) {
                String toastText = getString(R.string.account_unavailable, account.getDescription());
                FeedbackTools.showShortFeedback(accountsList, toastText);
                Timber.i("refusing to open account that is not available");
                return false;
            }
            if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName())) {
                FolderList.actionHandleAccount(this, realAccount);
            } else {
                LocalSearch search = new LocalSearch(realAccount.getAutoExpandFolderName());
                search.addAllowedFolder(realAccount.getAutoExpandFolderName());
                search.addAccountUuid(realAccount.getUuid());
                MessageList.actionDisplaySearch(this, search, false, true);}
        }
        return true;
    }

    private void onActivateAccount(Account account) {
        List<Account> disabledAccounts = new ArrayList<Account>();
        disabledAccounts.add(account);
        promptForServerPasswords(disabledAccounts);
    }

    /**
     * Ask the user to enter the server passwords for disabled accounts.
     *
     * @param disabledAccounts
     *         A non-empty list of {@link Account}s to ask the user for passwords. Never
     *         {@code null}.
     *         <p><strong>Note:</strong> Calling this method will modify the supplied list.</p>
     */
    private void promptForServerPasswords(final List<Account> disabledAccounts) {
        Account account = disabledAccounts.remove(0);
        PasswordPromptDialog dialog = new PasswordPromptDialog(account, disabledAccounts);
        setNonConfigurationInstance(dialog);
        dialog.show(this);
    }

    @Override
    protected void onImportFinished() {

    }

    private void onDeleteAccount(Account account) {
        selectedContextAccount = account;
        showDialog(DIALOG_REMOVE_ACCOUNT);
    }

    private void onEditAccount(Account account) {
        AccountSettingsActivity.start(this, account.getUuid());
    }

    @Override
    public Dialog onCreateDialog(int id) {
        // Android recreates our dialogs on configuration changes even when they have been
        // dismissed. Make sure we have all information necessary before creating a new dialog.
        switch (id) {
        case DIALOG_REMOVE_ACCOUNT: {
            if (selectedContextAccount == null) {
                return null;
            }

            return ConfirmationDialog.create(this, id,
                                             R.string.account_delete_dlg_title,
                                             getString(R.string.account_delete_dlg_instructions_fmt,
                                                     selectedContextAccount.getDescription()),
                                             R.string.okay_action,
                                             R.string.cancel_action,
            new Runnable() {
                @Override
                public void run() {
                    if (selectedContextAccount instanceof Account) {
                        Account realAccount = (Account) selectedContextAccount;
                        try {
                            realAccount.getLocalStore().delete();
                        } catch (Exception e) {
                            // Ignore, this may lead to localStores on sd-cards that
                            // are currently not inserted to be left
                        }
                        MessagingController.getInstance(getApplication())
                        .deleteAccount(realAccount);
                        Preferences.getPreferences(AccountsLegacy.this)
                        .deleteAccount(realAccount);
                        K9.setServicesEnabled(AccountsLegacy.this);
                        refresh();

                    }
                }
            });
        }
        case DIALOG_CLEAR_ACCOUNT: {
            if (selectedContextAccount == null) {
                return null;
            }

            return ConfirmationDialog.create(this, id,
                                             R.string.account_clear_dlg_title,
                                             getString(R.string.account_clear_dlg_instructions_fmt,
                                                     selectedContextAccount.getDescription()),
                                             R.string.okay_action,
                                             R.string.cancel_action,
            new Runnable() {
                @Override
                public void run() {
                    if (selectedContextAccount instanceof Account) {
                        Account realAccount = (Account) selectedContextAccount;
                        handler.workingAccount(realAccount,
                                                R.string.clearing_account);
                        MessagingController.getInstance(getApplication())
                        .clear(realAccount, null);
                    }
                }
            });
        }
        case DIALOG_RECREATE_ACCOUNT: {
            if (selectedContextAccount == null) {
                return null;
            }

            return ConfirmationDialog.create(this, id,
                                             R.string.account_recreate_dlg_title,
                                             getString(R.string.account_recreate_dlg_instructions_fmt,
                                                     selectedContextAccount.getDescription()),
                                             R.string.okay_action,
                                             R.string.cancel_action,
            new Runnable() {
                @Override
                public void run() {
                    if (selectedContextAccount instanceof Account) {
                        Account realAccount = (Account) selectedContextAccount;
                        handler.workingAccount(realAccount,
                                                R.string.recreating_account);
                        MessagingController.getInstance(getApplication())
                        .recreate(realAccount, null);
                    }
                }
            });
        }
        case DIALOG_NO_FILE_MANAGER: {
            return ConfirmationDialog.create(this, id,
                                             R.string.import_dialog_error_title,
                                             getString(R.string.import_dialog_error_message),
                                             R.string.open_market,
                                             R.string.close,
            new Runnable() {
                @Override
                public void run() {
                    Uri uri = Uri.parse(ANDROID_MARKET_URL);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            });
        }
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog d) {
        AlertDialog alert = (AlertDialog) d;
        switch (id) {
        case DIALOG_REMOVE_ACCOUNT: {
            alert.setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                                       selectedContextAccount.getDescription()));
            break;
        }
        case DIALOG_CLEAR_ACCOUNT: {
            alert.setMessage(getString(R.string.account_clear_dlg_instructions_fmt,
                                       selectedContextAccount.getDescription()));
            break;
        }
        case DIALOG_RECREATE_ACCOUNT: {
            alert.setMessage(getString(R.string.account_recreate_dlg_instructions_fmt,
                                       selectedContextAccount.getDescription()));
            break;
        }
        }

        super.onPrepareDialog(id, d);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        // submenus don't actually set the menuInfo, so the "advanced"
        // submenu wouldn't work.
        if (menuInfo != null) {
            selectedContextAccount = (BaseAccount)accountsList.getItemAtPosition(menuInfo.position);
        }
        if (selectedContextAccount instanceof Account) {
            Account realAccount = (Account) selectedContextAccount;
            switch (item.getItemId()) {
                case R.id.delete_account:
                    onDeleteAccount(realAccount);
                    break;
                case R.id.account_settings:
                    onEditAccount(realAccount);
                    break;
                case R.id.activate:
                    onActivateAccount(realAccount);
                    break;
                case R.id.clear_pending:
                    onClearCommands(realAccount);
                    break;
                case R.id.empty_trash:
                    onEmptyTrash(realAccount);
                    break;
                case R.id.clear:
                    onClear(realAccount);
                    break;
                case R.id.recreate:
                    onRecreate(realAccount);
                    break;
                case R.id.export:
                    onExport(false, realAccount);
                    break;
                case R.id.move_up:
                    onMove(realAccount, true);
                    break;
                case R.id.move_down:
                    onMove(realAccount, false);
                    break;
                case R.id.import_PGP_key_from_SD:
                    onImportPGPKeyFromFileSystem(realAccount);
                    break;

            }
        }
        return true;
    }

    private void onImportPGPKeyFromFileSystem(Account realAccount) {
        currentAccount = realAccount.getEmail();
        onKeyImport();
    }

    private void onClear(Account account) {
        showDialog(DIALOG_CLEAR_ACCOUNT);

    }
    private void onRecreate(Account account) {
        showDialog(DIALOG_RECREATE_ACCOUNT);
    }
    private void onMove(final Account account, final boolean up) {
        MoveAccountAsyncTask asyncTask = new MoveAccountAsyncTask(this, account, up);
        setNonConfigurationInstance(asyncTask);
        asyncTask.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
            onEditSettings();
            break;
        case R.id.check_mail:
            onCheckMail(null);
            break;
        case R.id.compose:
            onCompose();
            break;
        case R.id.about:
            onAbout();
            break;
        case R.id.search:
            showSearchView();
            break;
        case R.id.export_all:
            onExport(true, null);
            break;
        case R.id.import_settings:
            onSettingsImport();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onAbout() {
        AboutActivity.Companion.onAbout(this);
    }


    /**
     * Get current version number.
     *
     * @return String version
     */
    private String getVersionNumber() {
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //Log.e(TAG, "Package name not found", e);
        }
        return version;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.accounts_option, menu);
        refreshMenuItem = menu.findItem(R.id.check_mail);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Timber.d("onCreateContextMenu", "true");
        menu.setHeaderTitle(R.string.accounts_context_menu_title);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        BaseAccount account =  adapter.getItem(info.position);

        if ((account instanceof Account) && !((Account) account).isEnabled()) {
            getMenuInflater().inflate(R.menu.disabled_accounts_context, menu);
        } else {
            getMenuInflater().inflate(R.menu.accounts_context, menu);
        }

        if (account instanceof SearchAccount) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                    item.setVisible(false);
            }
        }
        else {
            EnumSet<ACCOUNT_LOCATION> accountLocation = accountLocation(account);
            if (accountLocation.contains(ACCOUNT_LOCATION.TOP)) {
                menu.findItem(R.id.move_up).setEnabled(false);
            }
            else {
                menu.findItem(R.id.move_up).setEnabled(true);
            }
            if (accountLocation.contains(ACCOUNT_LOCATION.BOTTOM)) {
                menu.findItem(R.id.move_down).setEnabled(false);
            }
            else {
                menu.findItem(R.id.move_down).setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.i("onActivityResult requestCode = %d, resultCode = %s, data = %s", requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        if (data == null) {
            return;
        }
        switch (requestCode) {
            case ACTIVITY_REQUEST_PICK_SETTINGS_FILE:
                onImport(data.getData());
                break;
            case ACTIVITY_REQUEST_SAVE_SETTINGS_FILE:
                onExport(data);
                break;
            case ACTIVITY_REQUEST_PICK_KEY_FILE:
                onKeyImport(data.getData(), currentAccount);
                break;
        }
    }


    public void onKeyImport(Uri uri, String currentAccount) {
        ListImportContentsAsyncTask asyncTask = new ListImportContentsAsyncTask(this, uri, currentAccount, true, fpr);
        setNonConfigurationInstance(asyncTask);
        asyncTask.execute();
    }
    @Override
    public void onImport(Uri uri) {
        ListImportContentsAsyncTask asyncTask = new ListImportContentsAsyncTask(this, uri, currentAccount, false, null);
        setNonConfigurationInstance(asyncTask);
        asyncTask.execute();
    }


    /**
     * Set the {@code NonConfigurationInstance} this activity should retain on configuration
     * changes.
     *
     * @param inst
     *         The {@link NonConfigurationInstance} that should be retained when
     *         {@link AccountsLegacy#onRetainNonConfigurationInstance()} is called.
     */
    public void setNonConfigurationInstance(NonConfigurationInstance inst) {
        nonConfigurationInstance = inst;
    }

    private class AccountClickListener implements OnClickListener {

        final LocalSearch search;

        AccountClickListener(LocalSearch search) {
            this.search = search;
        }

        @Override
        public void onClick(View v) {
            MessageList.actionDisplaySearch(AccountsLegacy.this, search, true, false);
        }

    }

    public void onExport(final boolean includeGlobals, final Account account) {

        createStoragePermissionListeners();
        if (hasWriteExternalPermission()) {        // TODO, prompt to allow a user to choose which accounts to export
            ArrayList<String> accountUuids = null;
            if (account != null) {
                accountUuids = new ArrayList<>();
                accountUuids.add(account.getUuid());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                exportGlobalSettings = includeGlobals;
                exportAccountUuids = accountUuids;

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, SettingsExporter.generateDatedExportFileName());

                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, ACTIVITY_REQUEST_SAVE_SETTINGS_FILE);

            } else {
                //Pre-Kitkat
                startExport(includeGlobals, accountUuids, null);
            }
        }
    }

    public void onExport(Intent intent) {
        Uri documentsUri = intent.getData();
        startExport(exportGlobalSettings, exportAccountUuids, documentsUri);
    }

    private void startExport(boolean exportGlobalSettings, ArrayList<String> exportAccountUuids, Uri documentsUri) {
      //  ExportAsyncTask asyncTask = new ExportAsyncTask(this, exportGlobalSettings, exportAccountUuids, documentsUri);
      //  setNonConfigurationInstance(asyncTask);
      //  asyncTask.execute();
    }

    private boolean hasWriteExternalPermission() {
        int res = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private static class MoveAccountAsyncTask extends ExtendedAsyncTask<Void, Void, Void> {
        private Account mAccount;
        private boolean mUp;

        protected MoveAccountAsyncTask(Activity activity, Account account, boolean up) {
            super(activity);
            mAccount = account;
            mUp = up;
        }

        @Override
        protected void showProgressDialog() {
            String message = mActivity.getString(R.string.manage_accounts_moving_message);
            mProgressDialog = ProgressDialog.show(mActivity, null, message, true);
        }

        @Override
        protected Void doInBackground(Void... args) {
            mAccount.move(Preferences.getPreferences(mContext), mUp);
            return null;
        }

        @Override
        protected void onPostExecute(Void arg) {
            AccountsLegacy activity = (AccountsLegacy) mActivity;

            // Let the activity know that the background task is complete
            activity.setNonConfigurationInstance(null);

            activity.refresh();
            removeProgressDialog();
        }
    }

    class FoldersAdapter extends ArrayAdapter<BaseAccount> {
        private final OnFolderClickListener onFolderClickListener;
        private final OnBaseAccountClickListener onBaseAccountClickListener;

        public FoldersAdapter(List<BaseAccount> accounts, OnFolderClickListener onFolderClickListener, OnBaseAccountClickListener onBaseAccountClickListener) {
            super(AccountsLegacy.this, 0, accounts);
            this.onFolderClickListener = onFolderClickListener;
            this.onBaseAccountClickListener = onBaseAccountClickListener;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final BaseAccount account = getItem(position);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
            }
            view.setLongClickable(true);
            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null) {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.descriptionUnreadMessages = (TextView) view.findViewById(R.id.description_unread_messages);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
                holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
                holder.newMessageCountWrapper = view.findViewById(R.id.new_message_count_wrapper);
                holder.flaggedMessageCountWrapper = view.findViewById(R.id.flagged_message_count_wrapper);
                holder.newMessageCountIcon = view.findViewById(R.id.new_message_count_icon);
                holder.flaggedMessageCountIcon = view.findViewById(R.id.flagged_message_count_icon);
                holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);

                holder.folders = (ImageButton) view.findViewById(R.id.folders);
                holder.accountsItemLayout = (LinearLayout)view.findViewById(R.id.accounts_item_layout);
                LinearLayout accountsDescriptionLayout = (LinearLayout)view.findViewById(R.id.accounts_description_layout);

                view.setTag(holder);

                holder.accountsItemLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onFolderClickListener.onClick(position);
                    }
                });
            }
            AccountStats stats = accountStats.get(account.getUuid());

            if (stats != null && account instanceof Account && stats.size >= 0) {
                holder.email.setText(SizeFormatter.formatSize(AccountsLegacy.this, stats.size));
                holder.email.setVisibility(View.VISIBLE);
            } else {
                if (account.getEmail().equals(account.getDescription())) {
                    holder.email.setVisibility(View.GONE);
                } else {
                    holder.email.setVisibility(View.VISIBLE);
                    holder.email.setText(account.getEmail());
                }
            }

            String description = account.getDescription();
            if (description == null || description.isEmpty()) {
                description = account.getEmail();
            }

            holder.description.setText(description);

            Integer unreadMessageCount = null;
            if (stats != null) {
                unreadMessageCount = stats.unreadMessageCount;
                holder.descriptionUnreadMessages.setText(String.format("%d", unreadMessageCount));
                holder.newMessageCount.setText(String.format("%d", unreadMessageCount));

                holder.flaggedMessageCount.setText(String.format("%d", stats.flaggedMessageCount));
                holder.flaggedMessageCountWrapper.setVisibility(K9.messageListStars() && stats.flaggedMessageCount > 0 ? View.VISIBLE : View.GONE);

                holder.flaggedMessageCountWrapper.setOnClickListener(createFlaggedSearchListener(account));
                holder.newMessageCountWrapper.setOnClickListener(createUnreadSearchListener(account));

                holder.activeIcons.setOnClickListener(new OnClickListener() {
                                                          public void onClick(View v) {
                                                              FeedbackTools.showShortFeedback(accountsList, getString(R.string.tap_hint));
                                                          }
                                                      }
                );

            } else {
                holder.newMessageCountWrapper.setVisibility(View.GONE);
                holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
            }
            if (account instanceof Account) {
                Account realAccount = (Account)account;
                holder.flaggedMessageCountIcon.setBackgroundDrawable(ContextCompat.getDrawable(AccountsLegacy.this, R.drawable.ic_unread_toggle_star));
            } else {
                holder.flaggedMessageCountIcon.setBackgroundDrawable(ContextCompat.getDrawable(AccountsLegacy.this,
                        R.drawable.ic_unread_toggle_star));
            }



            fontSizes.setViewTextSize(holder.description, fontSizes.getAccountName());
            fontSizes.setViewTextSize(holder.email, fontSizes.getAccountDescription());

            if (account instanceof SearchAccount) {
                holder.folders.setVisibility(View.GONE);
            } else {
                holder.folders.setVisibility(View.VISIBLE);
                holder.folders.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        FolderList.actionHandleAccount(AccountsLegacy.this, (Account)account);

                    }
                });
            }

            return view;
        }


        private OnClickListener createFlaggedSearchListener(BaseAccount account) {
            String searchTitle = getString(R.string.search_title, account.getDescription(),
                    getString(R.string.flagged_modifier));

            LocalSearch search;
            if (account instanceof SearchAccount) {
                search = ((SearchAccount) account).getRelatedSearch().clone();
                search.setName(searchTitle);
            } else {
                search = new LocalSearch(searchTitle);
                search.addAccountUuid(account.getUuid());

                Account realAccount = (Account) account;
                realAccount.excludeSpecialFolders(search);
                realAccount.limitToDisplayableFolders(search);
            }

            search.and(SearchField.FLAGGED, "1", Attribute.EQUALS);

            return new AccountClickListener(search);
        }

        private OnClickListener createUnreadSearchListener(BaseAccount account) {
            LocalSearch search = createUnreadSearch(AccountsLegacy.this, account);
            return new AccountClickListener(search);
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
            public TextView flaggedMessageCount;
            public View newMessageCountIcon;
            public View flaggedMessageCountIcon;
            public View newMessageCountWrapper;
            public View flaggedMessageCountWrapper;
            public RelativeLayout activeIcons;
            public View chip;
            public ImageButton folders;
            public LinearLayout accountsItemLayout;
            public TextView descriptionUnreadMessages;
        }
    }
}
