package com.fsck.k9.activity.folderlist;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.UpgradeDatabases;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.job.K9JobManager;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.KeyboardUtils;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.fsck.k9.activity.MessageList.EXTRA_SEARCH_ACCOUNT;

/**
 * FolderList is the primary user interface for the program. This
 * Activity shows list of the Account's folders
 */

public class FolderList extends K9ListActivity {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_FROM_SHORTCUT = "fromShortcut";

    private static final boolean REFRESH_REMOTE = true;

    private ListView listView;

    private FolderListAdapter adapter;

    private Account account;

    private FolderListHandler handler = new FolderListHandler();

    private Context context;

    private MenuItem mRefreshMenuItem;
    private View actionBarProgressView;

    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;
    private View searchLayout;
    private EditText searchInput;
    private View clearSearchIcon;

    private final K9JobManager jobManager = K9.jobManager;

    class FolderListHandler extends Handler {

        public void refreshTitle() {
            runOnUiThread(() -> {
                getToolbar().setTitle(R.string.folders_title);

                String operation = adapter.getListener().getOperation(FolderList.this);
                if (operation.length() < 1) {
                    getToolbar().setSubtitle(account.getEmail());
                    getToolbar().setTitle(getString(R.string.folders_title));
                } else {
                    getToolbar().setSubtitle(operation);
                    getToolbar().setTitle(getString(R.string.folders_title) + " (" + account.getEmail() + ")");
                }
            });
        }


        public void newFolders(final List<FolderInfoHolder> newFolders) {
            runOnUiThread(() -> {
                adapter.getFolders().clear();
                adapter.getFolders().addAll(newFolders);
                adapter.setFilteredFolders(adapter.getFolders());
                handler.dataChanged();
            });
        }

        public void workingAccount(final int res) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(res, account.getDescription());
                    FeedbackTools.showShortFeedback(getListView(), toastText);
                }
            });
        }

        public void accountSizeChanged(final long oldSize, final long newSize) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(R.string.account_size_changed, account.getDescription(), SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));

                    FeedbackTools.showLongFeedback(getListView(), toastText);
                }
            });
        }

        public void folderLoading(final String folder, final boolean loading) {
            runOnUiThread(new Runnable() {
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    public void run() {
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        FolderInfoHolder folderHolder = adapter.getFolder(folder);


                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        if (folderHolder != null) {
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            folderHolder.loading = loading;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        }

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    }
            });
        }

        public void progress(final boolean progress) {
            // Make sure we don't try this before the menu is initialized
            // this could happen while the activity is initialized.
            if (mRefreshMenuItem == null) {
                return;
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    if (progress) {
                        mRefreshMenuItem.setActionView(actionBarProgressView);
                    } else {
                        mRefreshMenuItem.setActionView(null);
                    }
                }
            });

        }

        public void dataChanged() {
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
    * This class is responsible for reloading the list of local messages for a
    * given folder, notifying the adapter that the message have been loaded and
    * queueing up a remote update of the folder.
     */

    private void checkMail(FolderInfoHolder folder) {
        TracingPowerManager pm = TracingPowerManager.getPowerManager(this);
        final TracingWakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FolderList checkMail");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(K9.WAKE_LOCK_TIMEOUT);
        MessagingListener listener = new SimpleMessagingListener() {
            @Override
            public void synchronizeMailboxFinished(Account account, String folder, int totalMessagesInMailbox, int numNewMessages) {
                if (!account.equals(FolderList.this.account)) {
                    return;
                }
                wakeLock.release();
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folder,
            String message) {
                if (!account.equals(FolderList.this.account)) {
                    return;
                }
                wakeLock.release();
            }
        };
        MessagingController.getInstance(getApplication()).synchronizeMailbox(account, folder.name, listener, null);
        sendMail(account);
    }

    public static Intent actionHandleAccountIntent(Context context, Account account, boolean fromShortcut) {
        Intent intent = new Intent(context, FolderList.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (fromShortcut) {
            intent.putExtra(EXTRA_FROM_SHORTCUT, true);
        }

        return intent;
    }

    public static void actionHandleAccount(Context context, Account account) {
        Intent intent = actionHandleAccountIntent(context, account, false);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        actionBarProgressView = getActionBarProgressView();
        setContentView(R.layout.folder_list);
        initializeActionBar();
        listView = getListView();
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setLongClickable(true);
        listView.setFastScrollEnabled(true);
        listView.setScrollingCacheEnabled(false);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onOpenFolder(((FolderInfoHolder) adapter.getItem(position)).name);
            }
        });
        registerForContextMenu(listView);

        listView.setSaveEnabled(true);

        context = this;

        onNewIntent(getIntent());
        if (isFinishing()) {
            /*
             * onNewIntent() may call finish(), but execution will still continue here.
             * We return now because we don't want to display the changelog which can
             * result in a leaked window error.
             */
            return;
        }

    }

    @SuppressLint("InflateParams")
    private View getActionBarProgressView() {
        return getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
    }

    private void initializeActionBar() {
        setUpToolbar(true);
        initializeSearchBar();
    }

    private void initializeSearchBar() {
        searchLayout = findViewById(R.id.toolbar_search_container);
        searchInput = (EditText) findViewById(R.id.search_input);
        clearSearchIcon = findViewById(R.id.search_clear);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence query, int start, int before, int count) {
                if (query.toString().isEmpty()) {
                    clearSearchIcon.setVisibility(View.GONE);
                } else {
                    clearSearchIcon.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    if (searchInput != null && !searchInput.getText().toString().isEmpty()) {
                        search(searchInput.getText().toString());
                    }
                    else {
                        searchInput.setError(getString(R.string.search_empty_error));
                    }
                }
                return true;
            }
        });

        clearSearchIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSearchView();
            }
        });
    }

    private void search(String query) {
        if (account != null && query != null) {
            final Bundle appData = new Bundle();
            appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
            triggerSearch(query, appData);
        }
    }

    public void showSearchView() {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            onSearchRequested();
        } else {
            if (searchLayout != null) {
                getToolbar().setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                searchInput.setEnabled(true);
                setFocusOnKeyboard();
                searchInput.setError(null);
            }
        }
    }

    public void hideSearchView() {
        if (searchLayout != null) {
            searchLayout.setVisibility(View.GONE);
            getToolbar().setVisibility(View.VISIBLE);
            searchInput.setEnabled(false);
            searchInput.setText(null);
            KeyboardUtils.hideKeyboard(searchInput);
        }
    }

    private void setFocusOnKeyboard() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        if (account == null) {
            /*
             * This can happen when a launcher shortcut is created for an
             * account, and then the account is deleted or data is wiped, and
             * then the shortcut is used.
             */
            finish();
            return;
        }

        if (intent.getBooleanExtra(EXTRA_FROM_SHORTCUT, false) &&
                   !K9.FOLDER_NONE.equals(account.getAutoExpandFolderName())) {
            onOpenFolder(account.getAutoExpandFolderName());
        } else {
            initializeActivityView();
        }
    }

    private void initializeActivityView() {
        adapter = new FolderListAdapter(this, account);
        restorePreviousData();

        setListAdapter(adapter);
        getListView().setTextFilterEnabled(adapter.getFilter() != null); // should never be false but better safe then sorry
    }

    @SuppressWarnings("unchecked")
    private void restorePreviousData() {
        final Object previousData = getLastCustomNonConfigurationInstance();
        if (previousData != null) {
            adapter.restorePreviousData((ArrayList<FolderInfoHolder>) previousData);
        }
    }

    @Override public Object onRetainCustomNonConfigurationInstance() {
        return (adapter == null) ? null : adapter.getFolders();
    }

    @Override public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(adapter.getListener());
        adapter.getListener().onPause(this);
    }

    /**
    * On resume we refresh the folder list (in the background) and we refresh the
    * messages for any folder that is currently open. This guarantees that things
    * like unread message count and read status are updated.
     */
    @Override public void onResume() {
        super.onResume();
        hideSearchView();
        if (!account.isAvailable(this)) {
            Timber.i("account unavaliabale, not showing folder-list but account-list");
            SettingsActivity.Companion.listAccounts(this);
            finish();
            return;
        }
        if (adapter == null)
            initializeActivityView();

        handler.refreshTitle();

        MessagingController.getInstance(getApplication()).addListener(adapter.getListener());
        //account.refresh(Preferences.getPreferences(this));
        MessagingController.getInstance(getApplication()).getAccountStats(this, account, adapter.getListener());
        onRefresh(!REFRESH_REMOTE);

        MessagingController.getInstance(getApplication()).cancelNotificationsForAccount(account);
        adapter.getListener().onResume(this);
    }

    public FolderListHandler getHandler() {
        return handler;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Shortcuts that work no matter what is selected
        switch (keyCode) {
            case KeyEvent.KEYCODE_Q: {
                onAccounts();
                return true;
            }

            case KeyEvent.KEYCODE_S: {
                onEditSettings();
                return true;
            }

            case KeyEvent.KEYCODE_H: {
                FeedbackTools.showLongFeedback(getListView(), getString(R.string.folder_list_help_key));
                return true;
            }

            case KeyEvent.KEYCODE_1: {
                setDisplayMode(FolderMode.FIRST_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_2: {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_3: {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_4: {
                setDisplayMode(FolderMode.ALL);
                return true;
            }
        }//switch


        return super.onKeyDown(keyCode, event);
    }//onKeyDown

    private void setDisplayMode(FolderMode newMode) {
        account.setFolderDisplayMode(newMode);
        account.save(Preferences.getPreferences(this));
        if (account.getFolderPushMode() != FolderMode.NONE) {
            jobManager.schedulePusherRefresh();
        }
        adapter.getFilter().filter(null);
        onRefresh(false);
    }

    private void onRefresh(final boolean forceRemote) {
        MessagingController.getInstance(getApplication()).listFolders(account, forceRemote, adapter.getListener());
    }

    private void onEditSettings() {
        SettingsActivity.launch(this);
    }

    private void onAccounts() {
        SettingsActivity.launch(this);
    }

    private void onEmptyTrash(final Account account) {
        handler.dataChanged();

        MessagingController.getInstance(getApplication()).emptyTrash(account, null);
    }

    private void onClearFolder(Account account, String folderName) {
        MessagingController.getInstance(getApplication()).clearFolder(account, folderName, adapter.getListener());
    }

    private void sendMail(Account account) {
        MessagingController.getInstance(getApplication()).sendPendingMessages(account, adapter.getListener());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();

            return true;

        case R.id.search:
            showSearchView();

            return true;

        case R.id.compose:
            MessageActions.actionCompose(this, account);

            return true;

        case R.id.check_mail:
            MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, adapter.getListener());

            return true;

        case R.id.send_messages:
            MessagingController.getInstance(getApplication()).sendPendingMessages(account, null);

            return true;

        case R.id.list_folders:
            onRefresh(REFRESH_REMOTE);

            return true;

        case R.id.settings:
            onEditSettings();
            return true;

        case R.id.empty_trash:
            onEmptyTrash(account);

            return true;

        case R.id.compact:
            onCompact(account);

            return true;

        case R.id.display_1st_class: {
            setDisplayMode(FolderMode.FIRST_CLASS);
            return true;
        }
        case R.id.display_1st_and_2nd_class: {
            setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
            return true;
        }
        case R.id.display_not_second_class: {
            setDisplayMode(FolderMode.NOT_SECOND_CLASS);
            return true;
        }
        case R.id.display_all: {
            setDisplayMode(FolderMode.ALL);
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested() {
         Bundle appData = new Bundle();
         appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
         startSearch(null, false, appData, false);
         return true;
     }

    private void onOpenFolder(String folder) {
        String allMessagesFolderName = context.getString(R.string.search_all_messages_title);
        String unifiedFolderName = context.getString(R.string.integrated_inbox_title);
        if (folder.equals(allMessagesFolderName)) {
            SearchAccount allMessagesAccount = SearchAccount.createAllMessagesAccount(this);
            MessageList.actionDisplaySearch(this, allMessagesAccount.getRelatedSearch(), false, false, true);
            return;
        }
        if (folder.equals(unifiedFolderName)) {
            SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
            MessageList.actionDisplaySearch(this, unifiedInboxAccount.getRelatedSearch(), false, false, true);
            return;
        }
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(account.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(this, search, false, false, true);
    }

    private void onCompact(Account account) {
        handler.workingAccount(R.string.compacting_account);
        MessagingController.getInstance(getApplication()).compact(account, null);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_list_option, menu);
        mRefreshMenuItem = menu.findItem(R.id.check_mail);
        configureFolderSearchView(menu);
        return true;
    }

    private void configureFolderSearchView(Menu menu) {
        final MenuItem folderMenuItem = menu.findItem(R.id.filter_folders);
        final SearchView folderSearchView = (SearchView) folderMenuItem.getActionView();
        folderSearchView.setQueryHint(getString(R.string.folder_list_filter_hint));
        folderSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                folderMenuItem.collapseActionView();
                getToolbar().setTitle(R.string.filter_folders_action);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        folderMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                menu.findItem(R.id.search).setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                getToolbar().setTitle(R.string.folders_title);
                menu.findItem(R.id.search).setVisible(true);
                return true;
            }
        });
    }

    @Override public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item .getMenuInfo();
        FolderInfoHolder folder = (FolderInfoHolder) adapter.getItem(info.position);

        switch (item.getItemId()) {
        case R.id.clear_local_folder:
            onClearFolder(account, folder.name);
            break;
        case R.id.refresh_folder:
            checkMail(folder);
            break;
        case R.id.folder_settings:
            FolderSettings.actionSettings(this, account, folder.name);
            break;
        }

        return super.onContextItemSelected(item);
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        getMenuInflater().inflate(R.menu.folder_context, menu);

        FolderInfoHolder folder = (FolderInfoHolder) adapter.getItem(info.position);

        menu.setHeaderTitle(folder.displayName);
    }

    @Override
    public void onBackPressed() {
        if(searchLayout != null && searchLayout.getVisibility() == View.VISIBLE) {
            hideSearchView();
        }
        else {
            super.onBackPressed();
        }
    }
}
