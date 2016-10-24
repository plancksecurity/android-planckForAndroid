package com.fsck.k9.activity;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.PEpUIUtils;
import com.fsck.k9.pEp.ui.infrastructure.DrawerLocker;
import com.fsck.k9.pEp.ui.listeners.OnAccountClickListener;
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener;
import com.fsck.k9.pEp.ui.renderers.AccountRenderer;
import com.fsck.k9.pEp.ui.renderers.FolderRenderer;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.fsck.k9.ui.messageview.MessageViewFragment.MessageViewFragmentListener;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.MessageTitleView;
import com.fsck.k9.view.ViewSwitcher;
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9Activity implements MessageListFragmentListener,
        MessageViewFragmentListener, OnBackStackChangedListener, OnSwitchCompleteListener,
        NavigationView.OnNavigationItemSelectedListener, DrawerLocker {

    // for this activity
    private static final String EXTRA_SEARCH = "search";
    private static final String EXTRA_NO_THREADING = "no_threading";

    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    // used for remote search
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private static final String STATE_DISPLAY_MODE = "displayMode";
    private static final String STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed";

    // Used for navigating to next/previous message
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private TextView mActionBarPepStatus;

    public static final int REQUEST_MASK_PENDING_INTENT = 1 << 16;
    private View customView;
    private DrawerLayout drawerLayout;
    private List<LocalFolder> menuFolders;
    private TextView firstAccountText;
    private TextView secondAccountText;
    private View firstAccountLayout;
    private View secondAccountLayout;
    private View mainAccountLayout;
    private TextView mainAccountText;
    private ImageView navigationViewFolders;
    private ImageView navigationViewAccounts;
    private NavigationView navigationView;
    private TextView mainAccountName;
    private TextView mainAccountEmail;
    private boolean showingAccountsMenu;
    private RecyclerView navigationFolders;
    private RecyclerView navigationAccounts;
    private View foldersDrawerLayout;
    private View accountsDrawerLayout;
    private View addAccountContainer;
    private View configureAccountContainer;
    private ActionBarDrawerToggle toggle;

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask) {
        actionDisplaySearch(context, search, noThreading, newTask, true);
    }

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop) {
        context.startActivity(
                intentDisplaySearch(context, search, noThreading, newTask, clearTop));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_SEARCH, search);
        intent.putExtra(EXTRA_NO_THREADING, noThreading);

        if (clearTop) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        return intent;
    }

    public static Intent shortcutIntent(Context context, String specialFolder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(ACTION_SHORTCUT);
        intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public static Intent actionDisplayMessageIntent(Context context,
            MessageReference messageReference) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        return intent;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.integrated_inbox) {
            SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
            updateMessagesForSpecificInbox(unifiedInboxAccount);
            return true;
        } else if (id == R.id.all_messages) {
            SearchAccount allMessagesAccount = SearchAccount.createAllMessagesAccount(this);
            updateMessagesForSpecificInbox(allMessagesAccount);
            return true;
        } else {
            return false;
        }
    }

    private void updateMessagesForSpecificInbox(SearchAccount searchAccount) {
        MessageListFragment fragment = MessageListFragment.newInstance(searchAccount.getRelatedSearch(), false, false);
        addMessageListFragment(fragment, true);
        drawerLayout.closeDrawers();
    }

    @Override
    public void setDrawerEnabled(boolean enabled) {
        int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED :
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        drawerLayout.setDrawerLockMode(lockMode);
        toggle.setDrawerIndicatorEnabled(enabled);
        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    private enum DisplayMode {
        MESSAGE_LIST,
        MESSAGE_VIEW,
        SPLIT_VIEW
    }


    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private View mActionBarMessageList;
    private View mActionBarMessageView;
    private MessageTitleView mActionBarSubject;
    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;
    private Menu mMenu;

    private ViewGroup mMessageViewContainer;
    private View mMessageViewPlaceHolder;

    private MessageListFragment mMessageListFragment;
    private MessageViewFragment mMessageViewFragment;
    private int mFirstBackStackId = -1;

    private Account mAccount;
    private String mFolderName;
    private LocalSearch mSearch;
    private boolean mSingleFolderMode;
    private boolean mSingleAccountMode;

    private ProgressBar mActionBarProgress;
    private MenuItem mMenuButtonCheckMail;
    private View mActionButtonIndeterminateProgress;
    private int mLastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;

    /**
     * {@code true} if the message list should be displayed as flat list (i.e. no threading)
     * regardless whether or not message threading was enabled in the settings. This is used for
     * filtered views, e.g. when only displaying the unread messages in a folder.
     */
    private boolean mNoThreading;

    private DisplayMode mDisplayMode;
    private MessageReference mMessageReference;

    /**
     * {@code true} when the message list was displayed once. This is used in
     * {@link #onBackPressed()} to decide whether to go from the message view to the message list or
     * finish the activity.
     */
    private boolean mMessageListWasDisplayed = false;
    private ViewSwitcher mViewSwitcher;

    private RendererBuilder<LocalFolder> rendererFolderBuilder;
    private RVRendererAdapter<LocalFolder> folderAdapter;
    private RendererBuilder<Account> rendererAccountBuilder;
    private RVRendererAdapter<Account> accountAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        if (useSplitView()) {
            bindViewsForLayout(R.layout.split_message_list);
        } else {
            bindViewsForLayout(R.layout.message_list);
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.container);
            mViewSwitcher.setFirstInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            mViewSwitcher.setFirstOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            mViewSwitcher.setSecondInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
            mViewSwitcher.setSecondOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            mViewSwitcher.setOnSwitchCompleteListener(this);
        }
        initializeActionBar();

        if (!decodeExtras(getIntent())) {
            return;
        }

        findFragments();
        initializeDisplayMode(savedInstanceState);
        initializeLayout();
        initializeFragments();
        displayViews();
        if (mAccount != null && mAccount.ispEpPrivacyProtected()) initializePepStatus();
        initializeFabButton();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, getToolbar(), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        foldersDrawerLayout = findViewById(R.id.navigation_bar_folders_layout);
        accountsDrawerLayout = findViewById(R.id.navigation_bar_accounts_layout);
        if (mAccount != null) {
            loadNavigationView();
        }
    }

    private void loadNavigationView() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mainAccountName = (TextView)findViewById(R.id.nav_header_name);
        mainAccountEmail = (TextView)findViewById(R.id.nav_header_email);

        mainAccountText = (TextView)findViewById(R.id.nav_header_contact_text);
        mainAccountLayout = findViewById(R.id.nav_header_image_container);
        firstAccountText = (TextView)findViewById(R.id.first_account);
        firstAccountLayout = findViewById(R.id.first_account_container);
        secondAccountText = (TextView)findViewById(R.id.second_account);
        secondAccountLayout = findViewById(R.id.second_account_container);

        navigationFolders = (RecyclerView) findViewById(R.id.navigation_folders);
        navigationViewFolders = (ImageView) findViewById(R.id.nav_header_folders);
        navigationViewAccounts = (ImageView) findViewById(R.id.nav_header_accounts);
        navigationViewAccounts.setVisibility(View.VISIBLE);

        navigationAccounts = (RecyclerView) findViewById(R.id.navigation_accounts);
        setupNavigationHeader();

        createFoldersMenu();
    }

    private void setupNavigationHeader() {
        mainAccountText.setText(PEpUIUtils.firstLetterOf(mAccount.getName()));
        mainAccountName.setText(mAccount.getName());
        mainAccountEmail.setText(mAccount.getEmail());
        setupNavigationHeaderListeners();
    }

    private void setupNavigationHeaderListeners() {
        navigationViewAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationViewAccounts.setVisibility(View.GONE);
                navigationViewFolders.setVisibility(View.VISIBLE);
                createAccountsMenu();
            }
        });

        navigationViewFolders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationViewAccounts.setVisibility(View.VISIBLE);
                navigationViewFolders.setVisibility(View.GONE);
                createFoldersMenu();
            }
        });
        setupAccountsListeners();
    }

    private void setupAccountsListeners() {
        List<Account> accounts = new ArrayList<>(Preferences.getPreferences(this).getAccounts());
        accounts.remove(mAccount);
        if (accounts.size() > 1) {
            setupThreeAcountsListeners(accounts);
        } else if (accounts.size() > 0) {
            setupTwoAcountsListeners(accounts);
        } else {
            firstAccountLayout.setVisibility(View.GONE);
            secondAccountLayout.setVisibility(View.GONE);
        }

        setupMainAccountListener();
    }

    private void setupMainAccountListener() {
        mainAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!showingAccountsMenu) {
                    navigationViewAccounts.setVisibility(View.GONE);
                    navigationViewFolders.setVisibility(View.VISIBLE);
                    createAccountsMenu();
                } else {
                    createFoldersMenu();
                }
            }
        });
    }

    private void setupTwoAcountsListeners(List<Account> accounts) {
        final Account firstAccount = accounts.get(accounts.size() - 1);
        firstAccountLayout.setVisibility(View.VISIBLE);
        secondAccountLayout.setVisibility(View.GONE);
        firstAccountText.setText(PEpUIUtils.firstLetterOf(firstAccount.getName()));
        firstAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAccountText.setText(PEpUIUtils.firstLetterOf(firstAccount.getName()));
                firstAccountText.setText(PEpUIUtils.firstLetterOf(mAccount.getName()));
                mainAccountEmail.setText(firstAccount.getEmail());
                mainAccountName.setText(firstAccount.getName());
                mAccount = firstAccount;
                setupNavigationHeader();
                onOpenFolder(firstAccount.getAutoExpandFolderName());
                drawerLayout.closeDrawers();
            }
        });
    }

    private void setupThreeAcountsListeners(List<Account> accounts) {
        final Account firstAccount = accounts.get(accounts.size() - 1);
        final Account lastAccount = accounts.get(accounts.size() - 2);
        firstAccountLayout.setVisibility(View.VISIBLE);
        secondAccountLayout.setVisibility(View.VISIBLE);
        firstAccountText.setText(PEpUIUtils.firstLetterOf(firstAccount.getName()));
        secondAccountText.setText(PEpUIUtils.firstLetterOf(lastAccount.getName()));
        firstAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAccountText.setText(PEpUIUtils.firstLetterOf(firstAccount.getName()));
                mainAccountEmail.setText(firstAccount.getEmail());
                mainAccountName.setText(firstAccount.getName());
                firstAccountText.setText(PEpUIUtils.firstLetterOf(lastAccount.getName()));
                secondAccountText.setText(PEpUIUtils.firstLetterOf(mAccount.getName()));
                mAccount = firstAccount;
                setupNavigationHeader();
                onOpenFolder(firstAccount.getAutoExpandFolderName());
                drawerLayout.closeDrawers();
            }
        });
        secondAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAccountText.setText(PEpUIUtils.firstLetterOf(lastAccount.getName()));
                mainAccountEmail.setText(lastAccount.getEmail());
                mainAccountName.setText(lastAccount.getName());
                secondAccountText.setText(PEpUIUtils.firstLetterOf(mAccount.getName()));
                mAccount = lastAccount;
                setupNavigationHeader();
                onOpenFolder(lastAccount.getAutoExpandFolderName());
                drawerLayout.closeDrawers();
            }
        });
    }

    private void createFoldersMenu() {
        showingAccountsMenu = false;
        foldersDrawerLayout.setVisibility(View.VISIBLE);
        accountsDrawerLayout.setVisibility(View.GONE);
        setupMainFolders();
        populateDrawerGroup();
    }

    private void setupMainFolders() {
        final SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
        final SearchAccount allMessagesAccount = SearchAccount.createAllMessagesAccount(this);

        setupUnreadMessagesInMainFolders(unifiedInboxAccount, allMessagesAccount);

        setupMainFoldersListeners(unifiedInboxAccount, allMessagesAccount);
    }

    private void setupUnreadMessagesInMainFolders(SearchAccount unifiedInboxAccount, SearchAccount allMessagesAccount) {
        TextView unifiedInboxMessages = (TextView) findViewById(R.id.unified_inbox_new_messages);
        TextView allMessages = (TextView) findViewById(R.id.all_messages_new_messages);

        LocalFolder unifiedFolder = refreshFolder(mAccount, unifiedInboxAccount.getRelatedSearch().getName());
        LocalFolder allMessagesFolder = refreshFolder(mAccount, allMessagesAccount.getRelatedSearch().getName());

        try {
            Integer unifiedMessageCount = unifiedFolder.getUnreadMessageCount();
            Integer allMessageCount = allMessagesFolder.getUnreadMessageCount();
            if (unifiedMessageCount != null && allMessageCount != null) {
                unifiedInboxMessages.setText(String.valueOf(unifiedMessageCount));
                allMessages.setText(String.valueOf(allMessageCount));
            }
        } catch (MessagingException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setupMainFoldersListeners(final SearchAccount unifiedInboxAccount, final SearchAccount allMessagesAccount) {
        View unifiedInbox = findViewById(R.id.unified_inbox);
        View allMessagesContainer = findViewById(R.id.all_messages_container);
        unifiedInbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMessagesForSpecificInbox(unifiedInboxAccount);
            }
        });
        allMessagesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMessagesForSpecificInbox(allMessagesAccount);
            }
        });
    }

    private LocalFolder refreshFolder(Account account, String folderName) {
        // There has to be a cheaper way to get at the localFolder object than this
        LocalFolder localFolder = null;
        try {
            if (account != null && folderName != null) {
                if (!account.isAvailable(MessageList.this)) {
                    Log.i(K9.LOG_TAG, "not refreshing folder of unavailable account");
                    return null;
                }
                localFolder = account.getLocalStore().getFolder(folderName);
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception while populating folder", e);
        } finally {
            if (localFolder != null) {
                localFolder.close();
            }
        }
        return localFolder;
    }

    private void createAccountsMenu() {
        showingAccountsMenu = true;
        foldersDrawerLayout.setVisibility(View.GONE);
        accountsDrawerLayout.setVisibility(View.VISIBLE);

        List<Account> accounts = new ArrayList<>(Preferences.getPreferences(this).getAccounts());
        accounts.remove(mAccount);
        AccountRenderer accountRenderer = new AccountRenderer();
        rendererAccountBuilder = new RendererBuilder<>(accountRenderer);

        accountRenderer.setOnAccountClickListenerListener(new OnAccountClickListener() {
            @Override
            public void onClick(Account account) {
                mAccount = account;
                onOpenFolder(account.getAutoExpandFolderName());
                setupNavigationHeader();
                createFoldersMenu();
                navigationViewFolders.setVisibility(View.GONE);
                navigationViewAccounts.setVisibility(View.VISIBLE);
                drawerLayout.closeDrawers();
            }
        });
        ListAdapteeCollection<Account> adapteeCollection = new ListAdapteeCollection<>(accounts);

        accountAdapter = new RVRendererAdapter<>(rendererAccountBuilder, adapteeCollection);

        navigationAccounts.setLayoutManager(new LinearLayoutManager(this));
        navigationAccounts.setAdapter(accountAdapter);

        setupCreateAccountListener();
        configureAccountContainer = findViewById(R.id.configure_account_container);
        configureAccountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                onAccounts();
            }
        });
    }

    private void setupCreateAccountListener() {
        addAccountContainer = findViewById(R.id.add_account_container);
        addAccountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                AccountSetupBasics.actionNewAccount(getBaseContext());
            }
        });
    }

    private void onOpenFolder(String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(mAccount.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(this, search, false, false);
    }

    private void populateDrawerGroup() {
        if (menuFolders != null) {
            populateFolders(menuFolders);
        } else {
            MessagingController instance = MessagingController.getInstance(this);
            instance.listFolders(mAccount, false, new MessagingListener() {
                @Override
                public void listFolders(Account account, List<LocalFolder> folders) {
                    menuFolders = folders;
                    populateFolders(folders);
                }
            });
        }
    }

    private void populateFolders(List<LocalFolder> folders) {
        FolderRenderer folderRenderer = new FolderRenderer();
        rendererFolderBuilder = new RendererBuilder<>(folderRenderer);

        folderRenderer.setFolderClickListener(new OnFolderClickListener() {
            @Override
            public void onClick(LocalFolder folder) {
                LocalSearch search = getLocalSearch(mAccount, folder);
                MessageListFragment fragment = MessageListFragment.newInstance(search, false, false);
                addMessageListFragment(fragment, true);
                drawerLayout.closeDrawers();
            }

            @Override
            public void onClick(Integer position) {

            }
        });
        ListAdapteeCollection<LocalFolder> adapteeCollection = new ListAdapteeCollection<>(folders);

        folderAdapter = new RVRendererAdapter<>(rendererFolderBuilder, adapteeCollection);

        navigationFolders.setLayoutManager(new LinearLayoutManager(this));
        navigationFolders.setAdapter(folderAdapter);
    }

    @NonNull
    private LocalSearch getLocalSearch(Account account, LocalFolder folder) {
        String searchTitle = getString(R.string.search_title,
                getString(R.string.message_list_title, account.getDescription(),
                        folder.getName()),
                getString(R.string.flagged_modifier));

        LocalSearch search = new LocalSearch(searchTitle);

        search.addAllowedFolder(folder.getName());
        search.addAccountUuid(account.getUuid());
        return search;
    }

    private void initializeFabButton() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_button_compose_message);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMessageCompose();
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        if (mFirstBackStackId >= 0) {
            getFragmentManager().popBackStackImmediate(mFirstBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mFirstBackStackId = -1;
        }
        removeMessageListFragment();
        removeMessageViewFragment();

        mMessageReference = null;
        mSearch = null;
        mFolderName = null;

        if (!decodeExtras(intent)) {
            return;
        }

        initializeDisplayMode(null);
        initializeFragments();
        displayViews();
    }

    private void goToMessageCompose() {
        Intent intent = new Intent(this, MessageCompose.class);
        startActivity(intent);
    }

    /**
     * Get references to existing fragments if the activity was restarted.
     */
    private void findFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.message_list_container);
        mMessageViewFragment = (MessageViewFragment) fragmentManager.findFragmentById(
                R.id.message_view_container);
    }

    /**
     * Create fragment instances if necessary.
     *
     * @see #findFragments()
     */
    private void initializeFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        boolean hasMessageListFragment = (mMessageListFragment != null);

        if (!hasMessageListFragment) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mMessageListFragment = MessageListFragment.newInstance(mSearch, false,
                    (K9.isThreadedViewEnabled() && !mNoThreading));
            ft.add(R.id.message_list_container, mMessageListFragment);
            ft.commit();
        }

        // Check if the fragment wasn't restarted and has a MessageReference in the arguments. If
        // so, open the referenced message.
        if (!hasMessageListFragment && mMessageViewFragment == null &&
                mMessageReference != null) {
            openMessage(mMessageReference);
        }
    }

    /**
     * Set the initial display mode (message list, message view, or split view).
     *
     * <p><strong>Note:</strong>
     * This method has to be called after {@link #findFragments()} because the result depends on
     * the availability of a {@link MessageViewFragment} instance.
     * </p>
     *
     * @param savedInstanceState
     *         The saved instance state that was passed to the activity as argument to
     *         {@link #onCreate(Bundle)}. May be {@code null}.
     */
    private void initializeDisplayMode(Bundle savedInstanceState) {
        if (useSplitView()) {
            mDisplayMode = DisplayMode.SPLIT_VIEW;
            return;
        }

        if (savedInstanceState != null) {
            DisplayMode savedDisplayMode =
                    (DisplayMode) savedInstanceState.getSerializable(STATE_DISPLAY_MODE);
            if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
                mDisplayMode = savedDisplayMode;
                return;
            }
        }

        if (mMessageViewFragment != null || mMessageReference != null) {
            mDisplayMode = DisplayMode.MESSAGE_VIEW;
        } else {
            mDisplayMode = DisplayMode.MESSAGE_LIST;
        }
    }

    private boolean useSplitView() {
        SplitViewMode splitViewMode = K9.getSplitViewMode();
        int orientation = getResources().getConfiguration().orientation;

        return (splitViewMode == SplitViewMode.ALWAYS ||
                (splitViewMode == SplitViewMode.WHEN_IN_LANDSCAPE &&
                orientation == Configuration.ORIENTATION_LANDSCAPE));
    }

    private void initializeLayout() {
        mMessageViewContainer = (ViewGroup) findViewById(R.id.message_view_container);

        LayoutInflater layoutInflater = getLayoutInflater();
        mMessageViewPlaceHolder = layoutInflater.inflate(R.layout.empty_message_view, mMessageViewContainer, false);
    }

    private void displayViews() {
        switch (mDisplayMode) {
            case MESSAGE_LIST: {
                showMessageList();
                break;
            }
            case MESSAGE_VIEW: {
                showMessageView();
                break;
            }
            case SPLIT_VIEW: {
                mMessageListWasDisplayed = true;
                if (mMessageViewFragment == null) {
                    showMessageViewPlaceHolder();
                } else {
                    MessageReference activeMessage = mMessageViewFragment.getMessageReference();
                    if (activeMessage != null) {
                        mMessageListFragment.setActiveMessage(activeMessage);
                    }
                }
                break;
            }
        }
    }

    private boolean decodeExtras(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri uri = intent.getData();
            List<String> segmentList = uri.getPathSegments();

            String accountId = segmentList.get(0);
            Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
            for (Account account : accounts) {
                if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                    String folderName = segmentList.get(1);
                    String messageUid = segmentList.get(2);
                    mMessageReference = new MessageReference(account.getUuid(), folderName, messageUid, null);
                    break;
                }
            }
        } else if (ACTION_SHORTCUT.equals(action)) {
            // Handle shortcut intents
            String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
            if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
                mSearch = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
            } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
                mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
            }
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            // check if this intent comes from the system search ( remote )
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                //Query was received from Search Dialog
                String query = intent.getStringExtra(SearchManager.QUERY).trim();

                mSearch = new LocalSearch(getString(R.string.search_results));
                mSearch.setManualSearch(true);
                mNoThreading = true;

                mSearch.or(new SearchCondition(SearchField.SENDER, Attribute.CONTAINS, query));
                mSearch.or(new SearchCondition(SearchField.SUBJECT, Attribute.CONTAINS, query));
                mSearch.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

                Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
                    // searches started from a folder list activity will provide an account, but no folder
                    if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                        mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
                    }
                } else {
                    mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
                }
            }
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
            mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        }

        if (mMessageReference == null) {
            mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        }

        if (mMessageReference != null) {
            mSearch = new LocalSearch();
            mSearch.addAccountUuid(mMessageReference.getAccountUuid());
            mSearch.addAllowedFolder(mMessageReference.getFolderName());
        }

        if (mSearch == null) {
            // We've most likely been started by an old unread widget
            String accountUuid = intent.getStringExtra("account");
            String folderName = intent.getStringExtra("folder");

            mSearch = new LocalSearch(folderName);
            mSearch.addAccountUuid((accountUuid == null) ? "invalid" : accountUuid);
            if (folderName != null) {
                mSearch.addAllowedFolder(folderName);
            }
        }

        Preferences prefs = Preferences.getPreferences(getApplicationContext());

        String[] accountUuids = mSearch.getAccountUuids();
        if (mSearch.searchAllAccounts()) {
            List<Account> accounts = prefs.getAccounts();
            mSingleAccountMode = (accounts.size() == 1);
            if (mSingleAccountMode) {
                mAccount = accounts.get(0);
            }
        } else {
            mSingleAccountMode = (accountUuids.length == 1);
            if (mSingleAccountMode) {
                mAccount = prefs.getAccount(accountUuids[0]);
            }
        }
        mSingleFolderMode = mSingleAccountMode && (mSearch.getFolderNames().size() == 1);

        if (mSingleAccountMode && (mAccount == null || !mAccount.isAvailable(this))) {
            Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
            onAccountUnavailable();
            return false;
        }

        if (mSingleFolderMode) {
            mFolderName = mSearch.getFolderNames().get(0);
        }

        // now we know if we are in single account mode and need a subtitle

        mActionBarSubTitle.setVisibility((!mSingleFolderMode) ? View.GONE : View.VISIBLE);

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!(this instanceof Search)) {
            //necessary b/c no guarantee Search.onStop will be called before MessageList.onResume
            //when returning from search results
            Search.setActive(false);
        }

        if (mAccount != null && !mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_DISPLAY_MODE, mDisplayMode);
        outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED, mMessageListWasDisplayed);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mMessageListWasDisplayed = savedInstanceState.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED);
    }

    private void initializeActionBar() {
        setUpToolbar(true);
        customView = getToolbar().findViewById(R.id.actionbar_custom);
        mActionBarMessageList = customView.findViewById(R.id.actionbar_message_list);
        mActionBarMessageView = customView.findViewById(R.id.actionbar_message_view);
        mActionBarSubject = (MessageTitleView) customView.findViewById(R.id.message_title_view);
        mActionBarTitle = (TextView) customView.findViewById(R.id.actionbar_title_first);
        mActionBarSubTitle = (TextView) customView.findViewById(R.id.actionbar_title_sub);
        mActionBarUnread = (TextView) customView.findViewById(R.id.actionbar_unread_count);
        mActionBarProgress = (ProgressBar) customView.findViewById(R.id.actionbar_progress);
        mActionButtonIndeterminateProgress = getActionButtonIndeterminateProgress();
    }

    private void initializePepStatus() {
        mActionBarPepStatus = (TextView) customView.findViewById(R.id.tvPep);
        mActionBarPepStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMessageViewFragment.onPepStatus();
            }
        });
    }


    @SuppressLint("InflateParams")
    private View getActionButtonIndeterminateProgress() {
        return getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
            ret = onCustomKeyDown(event.getKeyCode(), event);
        }
        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    @Override
    public void onBackPressed() {
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW && mMessageListWasDisplayed) {
            showMessageList();
        } else {
            onAccounts();
        }
    }

    /**
     * Handle hotkeys
     *
     * <p>
     * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any view had the chance
     * to consume this key event.
     * </p>
     *
     * @param keyCode
     *         The value in {@code event.getKeyCode()}.
     * @param event
     *         Description of the key event.
     *
     * @return {@code true} if this event was consumed.
     */
    public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                if (mMessageViewFragment != null && mDisplayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showPreviousMessage();
                    return true;
                } else if (mDisplayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveUp();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if (mMessageViewFragment != null && mDisplayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showNextMessage();
                    return true;
                } else if (mDisplayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveDown();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_C: {
                mMessageListFragment.onCompose();
                return true;
            }
            case KeyEvent.KEYCODE_Q: {
                if (mMessageListFragment != null && mMessageListFragment.isSingleAccountMode()) {
                    onShowFolderList();
                }
                return true;
            }
            case KeyEvent.KEYCODE_O: {
                mMessageListFragment.onCycleSort();
                return true;
            }
            case KeyEvent.KEYCODE_I: {
                mMessageListFragment.onReverseSort();
                return true;
            }
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_D: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onDelete();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onDelete();
                }
                return true;
            }
            case KeyEvent.KEYCODE_S: {
                mMessageListFragment.toggleMessageSelect();
                return true;
            }
            case KeyEvent.KEYCODE_G: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onToggleFlagged();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onToggleFlagged();
                }
                return true;
            }
            case KeyEvent.KEYCODE_M: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onMove();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onMove();
                }
                return true;
            }
            case KeyEvent.KEYCODE_V: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onArchive();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onArchive();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Y: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onCopy();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onCopy();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Z: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onToggleRead();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onToggleRead();
                }
                return true;
            }
            case KeyEvent.KEYCODE_F: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onForward();
                }
                return true;
            }
            case KeyEvent.KEYCODE_A: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onReplyAll();
                }
                return true;
            }
            case KeyEvent.KEYCODE_R: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onReply();
                }
                return true;
            }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P: {
                if (mMessageViewFragment != null) {
                    showPreviousMessage();
                }
                return true;
            }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K: {
                if (mMessageViewFragment != null) {
                    showNextMessage();
                }
                return true;
            }
            /* FIXME
            case KeyEvent.KEYCODE_Z: {
                mMessageViewFragment.zoom(event);
                return true;
            }*/
            case KeyEvent.KEYCODE_H: {
                FeedbackTools.showLongFeedback(getRootView(), getString(R.string.message_list_help_key));
                return true;
            }

        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void onAccounts() {
        Accounts.listAccounts(this);
        finish();
    }

    private void onShowFolderList() {
        FolderList.actionHandleAccount(this, mAccount);
        finish();
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, mAccount);
    }

    @Override
    public boolean onSearchRequested() {
        return mMessageListFragment.onSearchRequested();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                goBack();
                return true;
            }
            case R.id.compose: {
                mMessageListFragment.onCompose();
                return true;
            }
            case R.id.toggle_message_view_theme: {
                onToggleTheme();
                return true;
            }
            // MessageList
            case R.id.check_mail: {
                mMessageListFragment.checkMail();
                return true;
            }
            case R.id.set_sort_date: {
                mMessageListFragment.changeSort(SortType.SORT_DATE);
                return true;
            }
            case R.id.set_sort_arrival: {
                mMessageListFragment.changeSort(SortType.SORT_ARRIVAL);
                return true;
            }
            case R.id.set_sort_subject: {
                mMessageListFragment.changeSort(SortType.SORT_SUBJECT);
                return true;
            }
            case R.id.set_sort_sender: {
                mMessageListFragment.changeSort(SortType.SORT_SENDER);
                return true;
            }
            case R.id.set_sort_flag: {
                mMessageListFragment.changeSort(SortType.SORT_FLAGGED);
                return true;
            }
            case R.id.set_sort_unread: {
                mMessageListFragment.changeSort(SortType.SORT_UNREAD);
                return true;
            }
            case R.id.set_sort_attach: {
                mMessageListFragment.changeSort(SortType.SORT_ATTACHMENT);
                return true;
            }
            case R.id.select_all: {
                mMessageListFragment.selectAll();
                return true;
            }
            case R.id.app_settings: {
                onEditPrefs();
                return true;
            }
            case R.id.account_settings: {
                onEditAccount();
                return true;
            }
            case R.id.search: {
                mMessageListFragment.onSearchRequested();
                return true;
            }
            case R.id.search_remote: {
                mMessageListFragment.onRemoteSearch();
                return true;
            }
            case R.id.mark_all_as_read: {
                mMessageListFragment.confirmMarkAllAsRead();
                return true;
            }
            case R.id.show_folder_list: {
                onShowFolderList();
                return true;
            }
            // MessageView
            case R.id.next_message: {
                showNextMessage();
                return true;
            }
            case R.id.previous_message: {
                showPreviousMessage();
                return true;
            }
            case R.id.delete: {
                mMessageViewFragment.onDelete();
                return true;
            }
            case R.id.reply: {
                mMessageViewFragment.onReply();
                return true;
            }
            case R.id.reply_all: {
                mMessageViewFragment.onReplyAll();
                return true;
            }
            case R.id.forward: {
                mMessageViewFragment.onForward();
                return true;
            }
            case R.id.share: {
                mMessageViewFragment.onSendAlternate();
                return true;
            }
            case R.id.toggle_unread: {
                mMessageViewFragment.onToggleRead();
                return true;
            }
            case R.id.archive:
            case R.id.refile_archive: {
                mMessageViewFragment.onArchive();
                return true;
            }
            case R.id.spam:
            case R.id.refile_spam: {
                mMessageViewFragment.onSpam();
                return true;
            }
            case R.id.move:
            case R.id.refile_move: {
                mMessageViewFragment.onMove();
                return true;
            }
            case R.id.copy:
            case R.id.refile_copy: {
                mMessageViewFragment.onCopy();
                return true;
            }
            case R.id.select_text: {
                mMessageViewFragment.onSelectText();
                return true;
            }
            case R.id.show_headers:
            case R.id.hide_headers: {
                mMessageViewFragment.onToggleAllHeadersView();
                updateMenu();
                return true;
            }
        }

        if (!mSingleFolderMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
            case R.id.send_messages: {
                mMessageListFragment.onSendPendingMessages();
                return true;
            }
            case R.id.folder_settings: {
                if (mFolderName != null) {
                    FolderSettings.actionSettings(this, mAccount, mFolderName);
                }
                return true;
            }
            case R.id.expunge: {
                mMessageListFragment.onExpunge();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        mMenuButtonCheckMail= menu.findItem(R.id.check_mail);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        configureMenu(menu);
        return true;
    }

    /**
     * Hide menu items not appropriate for the current context.
     *
     * <p><strong>Note:</strong>
     * Please adjust the comments in {@code res/menu/message_list_option.xml} if you change the
     * visibility of a menu item in this method.
     * </p>
     *
     * @param menu
     *         The {@link Menu} instance that should be modified. May be {@code null}; in that case
     *         the method does nothing and immediately returns.
     */
    private void configureMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        // Set visibility of account/folder settings menu items
        if (mMessageListFragment == null) {
            menu.findItem(R.id.account_settings).setVisible(false);
            menu.findItem(R.id.folder_settings).setVisible(false);
        } else {
            menu.findItem(R.id.account_settings).setVisible(
                    mMessageListFragment.isSingleAccountMode());
            menu.findItem(R.id.folder_settings).setVisible(
                    mMessageListFragment.isSingleFolderMode());
        }

        /*
         * Set visibility of menu items related to the message view
         */

        if (mDisplayMode == DisplayMode.MESSAGE_LIST
                || mMessageViewFragment == null
                || !mMessageViewFragment.isInitialized()) {
            menu.findItem(R.id.next_message).setVisible(false);
            menu.findItem(R.id.previous_message).setVisible(false);
            menu.findItem(R.id.single_message_options).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.compose).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
            menu.findItem(R.id.refile).setVisible(false);
            menu.findItem(R.id.toggle_unread).setVisible(false);
            menu.findItem(R.id.select_text).setVisible(false);
            menu.findItem(R.id.toggle_message_view_theme).setVisible(false);
            menu.findItem(R.id.show_headers).setVisible(false);
            menu.findItem(R.id.hide_headers).setVisible(false);
        } else {
            // hide prev/next buttons in split mode
            if (mDisplayMode != DisplayMode.MESSAGE_VIEW) {
                menu.findItem(R.id.next_message).setVisible(false);
                menu.findItem(R.id.previous_message).setVisible(false);
            } else {
                MessageReference ref = mMessageViewFragment.getMessageReference();
                boolean initialized = (mMessageListFragment != null &&
                        mMessageListFragment.isLoadFinished());
                boolean canDoPrev = (initialized && !mMessageListFragment.isFirst(ref));
                boolean canDoNext = (initialized && !mMessageListFragment.isLast(ref));

                MenuItem prev = menu.findItem(R.id.previous_message);
                prev.setEnabled(canDoPrev);
                prev.getIcon().setAlpha(canDoPrev ? 255 : 127);

                MenuItem next = menu.findItem(R.id.next_message);
                next.setEnabled(canDoNext);
                next.getIcon().setAlpha(canDoNext ? 255 : 127);
            }

            MenuItem toggleTheme = menu.findItem(R.id.toggle_message_view_theme);
            if (K9.useFixedMessageViewTheme()) {
                toggleTheme.setVisible(false);
            } else {
                // Set title of menu item to switch to dark/light theme
                if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
                    toggleTheme.setTitle(R.string.message_view_theme_action_light);
                } else {
                    toggleTheme.setTitle(R.string.message_view_theme_action_dark);
                }
                toggleTheme.setVisible(true);
            }

            // Set title of menu item to toggle the read state of the currently displayed message
            if (mMessageViewFragment.isMessageRead()) {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action);
            } else {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action);
            }

            // Jellybean has built-in long press selection support
            menu.findItem(R.id.select_text).setVisible(Build.VERSION.SDK_INT < 16);

            menu.findItem(R.id.delete).setVisible(K9.isMessageViewDeleteActionVisible());

            /*
             * Set visibility of copy, move, archive, spam in action bar and refile submenu
             */
            if (mMessageViewFragment.isCopyCapable()) {
                menu.findItem(R.id.copy).setVisible(K9.isMessageViewCopyActionVisible());
                menu.findItem(R.id.refile_copy).setVisible(true);
            } else {
                menu.findItem(R.id.copy).setVisible(false);
                menu.findItem(R.id.refile_copy).setVisible(false);
            }

            if (mMessageViewFragment.isMoveCapable()) {
                boolean canMessageBeArchived = mMessageViewFragment.canMessageBeArchived();
                boolean canMessageBeMovedToSpam = mMessageViewFragment.canMessageBeMovedToSpam();

                menu.findItem(R.id.move).setVisible(K9.isMessageViewMoveActionVisible());
                menu.findItem(R.id.archive).setVisible(canMessageBeArchived &&
                        K9.isMessageViewArchiveActionVisible());
                menu.findItem(R.id.spam).setVisible(canMessageBeMovedToSpam &&
                        K9.isMessageViewSpamActionVisible());

                menu.findItem(R.id.refile_move).setVisible(true);
                menu.findItem(R.id.refile_archive).setVisible(canMessageBeArchived);
                menu.findItem(R.id.refile_spam).setVisible(canMessageBeMovedToSpam);
            } else {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

                menu.findItem(R.id.refile).setVisible(false);
            }

            if (mMessageViewFragment.allHeadersVisible()) {
                menu.findItem(R.id.show_headers).setVisible(false);
            } else {
                menu.findItem(R.id.hide_headers).setVisible(false);
            }
        }


        /*
         * Set visibility of menu items related to the message list
         */

        // Hide both search menu items by default and enable one when appropriate
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.search_remote).setVisible(false);

        if (mDisplayMode == DisplayMode.MESSAGE_VIEW || mMessageListFragment == null ||
                !mMessageListFragment.isInitialized()) {
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(false);
            menu.findItem(R.id.select_all).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.show_folder_list).setVisible(false);
        } else {
            menu.findItem(R.id.set_sort).setVisible(true);
            menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.compose).setVisible(true);
            menu.findItem(R.id.mark_all_as_read).setVisible(
                    mMessageListFragment.isMarkAllAsReadSupported());

            if (!mMessageListFragment.isSingleAccountMode()) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.show_folder_list).setVisible(false);
            } else {
                menu.findItem(R.id.send_messages).setVisible(mMessageListFragment.isOutbox());
                menu.findItem(R.id.expunge).setVisible(mMessageListFragment.isRemoteFolder() &&
                        mMessageListFragment.isAccountExpungeCapable());
                menu.findItem(R.id.show_folder_list).setVisible(true);
            }

            menu.findItem(R.id.check_mail).setVisible(mMessageListFragment.isCheckMailSupported());

            // If this is an explicit local search, show the option to search on the server
            if (!mMessageListFragment.isRemoteSearch() &&
                    mMessageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!mMessageListFragment.isManualSearch()) {
                menu.findItem(R.id.search).setVisible(true);
            }
        }
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    public void setActionBarTitle(String title) {
        mActionBarTitle.setText(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        mActionBarSubTitle.setText(subTitle);
    }

    public void setActionBarUnread(int unread) {
        if (unread == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            mActionBarUnread.setVisibility(View.GONE);
            mActionBarUnread.setText(String.format("%d", unread));
        }
    }

    @Override
    public void setMessageListTitle(String title) {
        setActionBarTitle(title);
    }

    @Override
    public void setMessageListSubTitle(String subTitle) {
        setActionBarSubTitle(subTitle);
    }

    @Override
    public void setUnreadCount(int unread) {
        setActionBarUnread(unread);
    }

    @Override
    public void setMessageListProgress(int progress) {
        setProgress(progress);
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = prefs.getAccount(messageReference.getAccountUuid());
        String folderName = messageReference.getFolderName();

        if (folderName.equals(account.getDraftsFolderName())) {
            MessageActions.actionEditDraft(this, messageReference);
        } else {
            mMessageViewContainer.removeView(mMessageViewPlaceHolder);

            if (mMessageListFragment != null) {
                mMessageListFragment.setActiveMessage(messageReference);
            }

            MessageViewFragment fragment = MessageViewFragment.newInstance(messageReference);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.message_view_container, fragment);
            mMessageViewFragment = fragment;
            ft.commit();

            if (mDisplayMode != DisplayMode.SPLIT_VIEW) {
                showMessageView();
            }
        }
    }

    @Override
    public void onResendMessage(MessageReference messageReference) {
        MessageActions.actionEditDraft(this, messageReference);
    }

    @Override
    public void onForward(MessageReference messageReference, Rating colorRating) {
        onForward(messageReference, null, colorRating);
    }

    @Override
    public void onForward(MessageReference messageReference, Parcelable decryptionResultForReply, Rating rating) {
        MessageActions.actionForward(this, messageReference, decryptionResultForReply, rating);
    }

    @Override
    public void onReply(MessageReference messageReference) {
        onReply(messageReference, null);
    }

    @Override
    public void onReply(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionReply(this, messageReference, false, decryptionResultForReply);
    }

    @Override
    public void onReplyAll(MessageReference messageReference) {
        onReplyAll(messageReference, null);
    }

    @Override
    public void onReplyAll(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionReply(this, messageReference, true, decryptionResultForReply);
    }

    @Override
    public void onCompose(Account account) {
        MessageActions.actionCompose(this, account);
    }

    @Override
    public void showMoreFromSameSender(String senderAddress) {
        LocalSearch tmpSearch = new LocalSearch("From " + senderAddress);
        tmpSearch.addAccountUuids(mSearch.getAccountUuids());
        tmpSearch.and(SearchField.SENDER, senderAddress, Attribute.CONTAINS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, false, false);

        addMessageListFragment(fragment, true);
    }

    @Override
    public void onBackStackChanged() {
        findFragments();

        if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
            showMessageViewPlaceHolder();
        }

        configureMenu(mMenu);
    }

    private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (mAccount != null && providerId.equals(mAccount.getLocalStorageProviderId())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onAccountUnavailable();
                    }
                });
            }
        }

        @Override
        public void onMount(String providerId) {
            // no-op
        }
    }

    private void addMessageListFragment(MessageListFragment fragment, boolean addToBackStack) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);

        ft.replace(R.id.message_list_container, fragment);
        if (addToBackStack)
            ft.addToBackStack(null);

        mMessageListFragment = fragment;

        int transactionId = ft.commit();
        if (transactionId >= 0 && mFirstBackStackId < 0) {
            mFirstBackStackId = transactionId;
        }
    }

    @Override
    public boolean startSearch(Account account, String folderName) {
        // If this search was started from a MessageList of a single folder, pass along that folder info
        // so that we can enable remote search.
        if (account != null && folderName != null) {
            final Bundle appData = new Bundle();
            appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
            appData.putString(EXTRA_SEARCH_FOLDER, folderName);
            startSearch(null, false, appData, false);
        } else {
            // TODO Handle the case where we're searching from within a search result.
            startSearch(null, false, null, false);
        }

        return true;
    }

    @Override
    public void showThread(Account account, String folderName, long threadRootId) {
        showMessageViewPlaceHolder();

        LocalSearch tmpSearch = new LocalSearch();
        tmpSearch.addAccountUuid(account.getUuid());
        tmpSearch.and(SearchField.THREAD_ID, String.valueOf(threadRootId), Attribute.EQUALS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, true, false);
        addMessageListFragment(fragment, true);
    }

    private void showMessageViewPlaceHolder() {
        removeMessageViewFragment();

        // Add placeholder view if necessary
        if (mMessageViewPlaceHolder.getParent() == null) {
            mMessageViewContainer.addView(mMessageViewPlaceHolder);
        }

        mMessageListFragment.setActiveMessage(null);
    }

    /**
     * Remove MessageViewFragment if necessary.
     */
    private void removeMessageViewFragment() {
        if (mMessageViewFragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(mMessageViewFragment);
            mMessageViewFragment = null;
            ft.commit();

            showDefaultTitleView();
        }
    }

    private void removeMessageListFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(mMessageListFragment);
        mMessageListFragment = null;
        ft.commit();
    }

    @Override
    public void remoteSearchStarted() {
        // Remove action button for remote search
        configureMenu(mMenu);
    }

    @Override
    public void goBack() {
        FragmentManager fragmentManager = getFragmentManager();
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            showMessageList();
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else if (mMessageListFragment.isManualSearch()) {
            finish();
        } else if (!mSingleFolderMode) {
            onAccounts();
        } else {
            onShowFolderList();
        }
    }

    @Override
    public void enableActionBarProgress(boolean enable) {
        if (mMenuButtonCheckMail != null && mMenuButtonCheckMail.isVisible()) {
            mActionBarProgress.setVisibility(ProgressBar.GONE);
            if (enable) {
                mMenuButtonCheckMail
                        .setActionView(mActionButtonIndeterminateProgress);
            } else {
                mMenuButtonCheckMail.setActionView(null);
            }
        } else {
            if (mMenuButtonCheckMail != null)
                mMenuButtonCheckMail.setActionView(null);
            if (enable) {
                mActionBarProgress.setVisibility(ProgressBar.VISIBLE);
            } else {
                mActionBarProgress.setVisibility(ProgressBar.GONE);
            }
        }
    }

    @Override
    public void displayMessageSubject(String subject) {
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            mActionBarSubject.setText(subject);
        }
    }

    @Override
    public void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
            if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder();
            } else {
                showMessageList();
            }
        }
    }

    /**
     * Shows the next message in the direction the user was displaying messages.
     *
     * @return {@code true}
     */
    private boolean showLogicalNextMessage() {
        boolean result = false;
        if (mLastDirection == NEXT) {
            result = showNextMessage();
        } else if (mLastDirection == PREVIOUS) {
            result = showPreviousMessage();
        }

        if (!result) {
            result = showNextMessage() || showPreviousMessage();
        }

        return result;
    }

    @Override
    public void setProgress(boolean enable) {
        setProgressBarIndeterminateVisibility(enable);
    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader header) {
        mActionBarSubject.setMessageHeader(header);
    }

    public boolean showNextMessage() {
        MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openNext(ref)) {
                mLastDirection = NEXT;
                return true;
            }
        }
        return false;
    }

    public boolean showPreviousMessage() {
        MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openPrevious(ref)) {
                mLastDirection = PREVIOUS;
                return true;
            }
        }
        return false;
    }

    private void showMessageList() {
        //I left this here because I don't know nothing about the UI design
        PEpUtils.colorToolbar(PePUIArtefactCache.getInstance(getApplicationContext()), getToolbar(), Rating.pEpRatingTrustedAndAnonymized);
        setStatusBarPepColor(Rating.pEpRatingTrustedAndAnonymized);
        mMessageListWasDisplayed = true;
        mDisplayMode = DisplayMode.MESSAGE_LIST;
        mViewSwitcher.showFirstView();

        mMessageListFragment.setActiveMessage(null);

        showDefaultTitleView();
        configureMenu(mMenu);
    }

    private void showMessageView() {
        mDisplayMode = DisplayMode.MESSAGE_VIEW;

        if (!mMessageListWasDisplayed) {
            mViewSwitcher.setAnimateFirstView(false);
        }
        mViewSwitcher.showSecondView();

        showMessageTitleView();
        configureMenu(mMenu);
    }

    @Override
    public void updateMenu() {
        invalidateOptionsMenu();
    }



    @Override
    public void disableDeleteAction() {
        mMenu.findItem(R.id.delete).setEnabled(false);
    }

    private void onToggleTheme() {
        if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
            K9.setK9MessageViewThemeSetting(K9.Theme.LIGHT);
        } else {
            K9.setK9MessageViewThemeSetting(K9.Theme.DARK);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Context appContext = getApplicationContext();
                Preferences prefs = Preferences.getPreferences(appContext);
                StorageEditor editor = prefs.getStorage().edit();
                K9.save(editor);
                editor.commit();
            }
        }).start();

        recreate();
    }

    private void showDefaultTitleView() {
        mActionBarMessageView.setVisibility(View.GONE);
        mActionBarMessageList.setVisibility(View.VISIBLE);

        if (mMessageListFragment != null) {
            mMessageListFragment.updateTitle();
        }

        mActionBarSubject.setMessageHeader(null);
    }

    private void showMessageTitleView() {
        mActionBarMessageList.setVisibility(View.GONE);
        mActionBarMessageView.setVisibility(View.VISIBLE);

        if (mMessageViewFragment != null) {
            displayMessageSubject(null);
            mMessageViewFragment.updateTitle();
        }
    }

    @Override
    public void onSwitchComplete(int displayedChild) {
        if (displayedChild == 0) {
            removeMessageViewFragment();
        }
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent,
            int flagsMask, int flagsValues, int extraFlags) throws SendIntentException {
        requestCode |= REQUEST_MASK_PENDING_INTENT;
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode & REQUEST_MASK_PENDING_INTENT) == REQUEST_MASK_PENDING_INTENT) {
            requestCode ^= REQUEST_MASK_PENDING_INTENT;
            if (mMessageViewFragment != null) {
                mMessageViewFragment.onPendingIntentResult(requestCode, resultCode, data);
            }
        }
    }
}
