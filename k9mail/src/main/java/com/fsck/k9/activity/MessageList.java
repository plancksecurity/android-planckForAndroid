package com.fsck.k9.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.DrawerFolderPopulator;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.notification.NotificationChannelManager;
import com.fsck.k9.pEp.AccountUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.models.FolderModel;
import com.fsck.k9.pEp.ui.infrastructure.DrawerLocker;
import com.fsck.k9.pEp.ui.infrastructure.MessageSwipeDirection;
import com.fsck.k9.pEp.ui.infrastructure.Router;
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
import com.google.android.material.navigation.NavigationView;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import foundation.pEp.jniadapter.Rating;
import security.pEp.permissions.PermissionChecker;
import security.pEp.permissions.PermissionRequester;
import security.pEp.ui.PEpUIUtils;
import security.pEp.ui.intro.WelcomeMessageKt;
import security.pEp.ui.nav_view.NavFolderAccountButton;
import security.pEp.ui.resources.ResourcesProvider;
import security.pEp.ui.toolbar.ToolBarCustomizer;
import timber.log.Timber;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends PepActivity implements MessageListFragmentListener,
        MessageViewFragmentListener, OnBackStackChangedListener, OnSwitchCompleteListener,
        NavigationView.OnNavigationItemSelectedListener, DrawerLocker {

    @Inject AccountUtils accountUtils;
    @Inject
    NotificationChannelManager channelUtils;
    @Inject
    PermissionRequester permissionRequester;
    @Inject
    ToolBarCustomizer toolBarCustomizer;
    @Inject
    Preferences preferences;
    @Inject
    ResourcesProvider resourcesProvider;
    @Inject
    DrawerFolderPopulator drawerFolderPopulator;

    @Deprecated
    //TODO: Remove after 2017-09-11
    private static final String EXTRA_SEARCH_OLD = "search";

    private static final String EXTRA_SEARCH = "search_bytes";
    private static final String EXTRA_FOLDER = "open_folder";
    private static final String EXTRA_NO_THREADING = "no_threading";

    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    // used for remote search
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private static final String STATE_DISPLAY_MODE = "displayMode";
    private static final String STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed";
    private static final String STATE_FIRST_BACK_STACK_ID = "firstBackstackId";

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
    private NavFolderAccountButton navFoldersAccountsButton;
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
    private DrawerLayout.DrawerListener drawerCloseListener;
    private boolean messageViewVisible;
    private boolean isThreadDisplayed;
    private MessageSwipeDirection direction;
    private Account lastUsedAccount;
    private SearchAccount unifiedInboxAccount;
    private SearchAccount allMessagesAccount;
    private String specialAccountUuid;

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean isFolder) {
        actionDisplaySearch(context, search, noThreading, newTask, false, isFolder);
    }

    public static void actionDisplaySearch(Context context, SearchSpecification search,
                                           boolean noThreading, boolean newTask) {
        actionDisplaySearch(context, search, noThreading, newTask, true, false);
    }

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop, boolean isFolder) {
        context.startActivity(
                intentDisplaySearch(context, search, noThreading, newTask, clearTop, isFolder));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop) {
        return intentDisplaySearch(context, search, noThreading, newTask, clearTop, false);
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop, boolean isFolder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_SEARCH, search);
        intent.putExtra(EXTRA_FOLDER, isFolder);
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
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
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
        specialAccountUuid = searchAccount.getUuid();
        LocalSearch search = searchAccount.getRelatedSearch();
        MessageListFragment fragment = MessageListFragment.newInstance(search, false,
                !mNoThreading);
        addMessageListFragment(fragment, !isHomeScreen(search));
        drawerLayout.closeDrawers();
    }

    @Override
    public void setDrawerEnabled(boolean enabled) {
        if (drawerLayout != null) {
            int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED :
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
            drawerLayout.setDrawerLockMode(lockMode);
            toggle.setDrawerIndicatorEnabled(enabled);
            if (!enabled) {
                toggle.setToolbarNavigationClickListener(v -> onBackPressed());
                if(messageViewVisible) {
                    setUpToolbarHomeIcon(resourcesProvider.getAttributeResource(R.attr.iconActionCancel));
                }
            }
        }
    }

    private void handleDrawerState() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.END);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    public void setMessageViewVisible(Boolean visible) {
        messageViewVisible = visible;
    }

    public Boolean isMessageViewVisible() {
        return messageViewVisible;
    }

    public void setThreadDisplay(Boolean threadDisplay) {
        this.isThreadDisplayed = threadDisplay;
    }

    public Boolean isThreadDisplayed() {
        return isThreadDisplayed;
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
    private MenuItem flaggedCheckbox;
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

    private RendererBuilder<FolderModel> rendererFolderBuilder;
    private RVRendererAdapter<FolderModel> folderAdapter;
    private RendererBuilder<Account> rendererAccountBuilder;
    private RVRendererAdapter<Account> accountAdapter;

    @Inject
    PermissionChecker permissionChecker;

    private void askForContactPermission() {
        if (permissionChecker.doesntHaveContactsPermission()) {
            permissionRequester.requestContactsPermission(getRootView());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        if (useSplitView()) {
            bindViews(R.layout.split_message_list);
        } else {
            bindViews(R.layout.message_list);
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

        permissionRequester.requestBatteryOptimizationPermission();
        askForContactPermission();
        findFragments();
        initializeDisplayMode(savedInstanceState);
        initializeLayout();
        initializeFragments();
        displayViews();
        channelUtils.updateChannels();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        initializeDrawerToggle();

        foldersDrawerLayout = findViewById(R.id.navigation_bar_folders_layout);
        accountsDrawerLayout = findViewById(R.id.navigation_bar_accounts_layout);
    }

    @Override
    public void search(String query) {
        if (mAccount != null && query != null) {
            final Bundle appData = new Bundle();
            if(isUnifiedInbox(mSearch)) specialAccountUuid = SearchAccount.UNIFIED_INBOX;
            if(specialAccountUuid != null) {
                appData.putString(EXTRA_SEARCH_ACCOUNT, specialAccountUuid);
            }
            else {
                appData.putString(EXTRA_SEARCH_ACCOUNT, mAccount.getUuid());
                appData.putString(EXTRA_SEARCH_FOLDER, mFolderName);
            }
            triggerSearch(query, appData);
        }
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
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
        navFoldersAccountsButton = findViewById(R.id.navFoldersAccountsButton);

        navigationAccounts = (RecyclerView) findViewById(R.id.navigation_accounts);
        setupNavigationHeader();

        setupNavigationFoldersList();

        createFoldersMenu();

        setNavigationViewInsets();
    }

    private void setNavigationViewInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                navigationView,
                (v, insets) -> {
                    View view = navigationView.findViewById(R.id.menu_header);
                    view.setPadding(
                            view.getPaddingLeft(),
                            insets.getSystemWindowInsetTop(),
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );
                    return insets.consumeSystemWindowInsets();
                });
    }

    private void setupNavigationFoldersList() {
        if (navigationFolders != null) {
            navigationFolders.setLayoutManager(getDrawerLayoutManager());
        }
        FolderRenderer folderRenderer = new FolderRenderer();
        rendererFolderBuilder = new RendererBuilder<>(folderRenderer);

        folderRenderer.setFolderClickListener(new OnFolderClickListener() {
            @Override
            public void onClick(LocalFolder folder) {
                changeFolder(folder);
            }

            @Override
            public void onClick(Integer position) {

            }
        });
        ListAdapteeCollection<FolderModel> adapteeCollection = new ListAdapteeCollection<>(Collections.emptyList());

        folderAdapter = new RVRendererAdapter<>(rendererFolderBuilder, adapteeCollection);
        if (navigationFolders != null) {
            navigationFolders.setAdapter(folderAdapter);
        }
    }

    private void setupNavigationHeader() {
        mainAccountText.setText(PEpUIUtils.accountNameSummary(mAccount.getName()));
        mainAccountName.setText(mAccount.getName());
        mainAccountEmail.setText(mAccount.getEmail());
        setupNavigationHeaderListeners();
        setupAccountsListeners();
    }

    private void setupNavigationHeaderListeners() {
        findViewById(R.id.menu_header).setOnClickListener(v -> {
                    if (!showingAccountsMenu) {
                        navFoldersAccountsButton.showFolders();
                        createAccountsMenu();
                    } else {
                        navFoldersAccountsButton.showAccounts();
                        createFoldersMenu();
                    }
                }
        );
    }

    private void setupAccountsListeners() {
        List<Account> accounts = new ArrayList<>(preferences.getAccounts());
        accounts.remove(mAccount);
        if (accounts.size() > 1) {
            setupThreeAcountsListeners(accounts);
        } else if (accounts.size() > 0) {
            setupTwoAcountsListeners(accounts);
        } else {
            firstAccountLayout.setVisibility(View.GONE);
            secondAccountLayout.setVisibility(View.GONE);
        }
    }

    private void setupTwoAcountsListeners(List<Account> accounts) {
        final Account firstAccount = accounts.get(accounts.size() - 1);
        firstAccountLayout.setVisibility(View.VISIBLE);
        secondAccountLayout.setVisibility(View.GONE);
        firstAccountText.setText(PEpUIUtils.accountNameSummary(firstAccount.getName()));
        firstAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageListFragment.showLoadingMessages();
                mainAccountText.setText(PEpUIUtils.accountNameSummary(firstAccount.getName()));
                firstAccountText.setText(PEpUIUtils.accountNameSummary(mAccount.getName()));
                mainAccountEmail.setText(firstAccount.getEmail());
                mainAccountName.setText(firstAccount.getName());
                changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount);
            }
        });
    }

    private void setupThreeAcountsListeners(List<Account> accounts) {
        final Account firstAccount = accounts.get(accounts.size() - 1);
        final Account lastAccount = accounts.get(accounts.size() - 2);
        firstAccountLayout.setVisibility(View.VISIBLE);
        secondAccountLayout.setVisibility(View.VISIBLE);
        firstAccountText.setText(PEpUIUtils.accountNameSummary(firstAccount.getName()));
        secondAccountText.setText(PEpUIUtils.accountNameSummary(lastAccount.getName()));
        firstAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageListFragment.showLoadingMessages();
                mainAccountText.setText(PEpUIUtils.accountNameSummary(firstAccount.getName()));
                mainAccountEmail.setText(firstAccount.getEmail());
                mainAccountName.setText(firstAccount.getName());
                firstAccountText.setText(PEpUIUtils.accountNameSummary(lastAccount.getName()));
                secondAccountText.setText(PEpUIUtils.accountNameSummary(mAccount.getName()));
                changeAccountAnimation(mainAccountLayout, firstAccountLayout, firstAccount);
            }
        });
        secondAccountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageListFragment.showLoadingMessages();
                mainAccountText.setText(PEpUIUtils.accountNameSummary(lastAccount.getName()));
                mainAccountEmail.setText(lastAccount.getEmail());
                mainAccountName.setText(lastAccount.getName());
                secondAccountText.setText(PEpUIUtils.accountNameSummary(mAccount.getName()));
                changeAccountAnimation(mainAccountLayout, secondAccountLayout, lastAccount);
            }
        });
    }

    private void changeAccountAnimation(final View goToView, final View fromView, final Account accountClicked) {
        final int firstAccountLayoutPosition[] = new int[2];
        fromView.getLocationOnScreen( firstAccountLayoutPosition );
        final int mainAccountLayoutPosition[] = new int[2];
        goToView.getLocationOnScreen(mainAccountLayoutPosition);

        TranslateAnimation anim = new TranslateAnimation(0, goToView.getX() + goToView.getWidth()/2 - firstAccountLayoutPosition[0] , 0, goToView.getY() + goToView.getHeight()/2 - firstAccountLayoutPosition[1]);
        anim.setDuration(500);
        fromView.startAnimation(anim);
        initializeDrawerListener(fromView, accountClicked);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Animation dissapearAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.scale_down);
                dissapearAnimation.setDuration(500);
                goToView.startAnimation(dissapearAnimation);

                drawerLayout.addDrawerListener(drawerCloseListener);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fromView.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.scale_up));
                drawerLayout.closeDrawers();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void initializeDrawerListener(final View fromView, final Account accountClicked) {
        drawerCloseListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                changeAccount(fromView, accountClicked);
                drawerLayout.removeDrawerListener(drawerCloseListener);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };
    }

    private void changeAccount(View fromView, Account accountClicked) {
        fromView.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.scale_up));
        drawerLayout.closeDrawers();
        mAccount = accountClicked;
        setupNavigationHeader();
        String folder = accountClicked.getAutoExpandFolderName();
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(accountClicked.getUuid());
        search.addAllowedFolder(folder);
        refreshMessages(search);
        changeAccountsOrder();
    }

    private void changeAccountsOrder() {
        List<Account> accounts = preferences.getAccounts();
        List<Account> clonedAccounts = new ArrayList<>(accounts.size());
        clonedAccounts.addAll(accounts);
        clonedAccounts.remove(mAccount);
        ArrayList<Account> reorderedAccounts = new ArrayList<>(accounts.size());
        reorderedAccounts.add(mAccount);
        reorderedAccounts.addAll(clonedAccounts);
        preferences.setAccounts(reorderedAccounts);
    }

    private void createFoldersMenu() {
        showingAccountsMenu = false;
        foldersDrawerLayout.setVisibility(View.VISIBLE);
        accountsDrawerLayout.setVisibility(View.GONE);
        setupMainFolders();
        populateDrawerGroup();
    }

    private void setupMainFolders() {
        setupMainFoldersUnreadMessages();

        setupMainFoldersListeners(unifiedInboxAccount, allMessagesAccount);
    }

    private void setNewInboxMessages(AccountStats stats, TextView unreadMessagesView) {
        if (stats.unreadMessageCount > 0) {
            unreadMessagesView.setVisibility(View.VISIBLE);
            unreadMessagesView.setText(String.valueOf(stats.unreadMessageCount));
        } else {
            unreadMessagesView.setVisibility(View.GONE);
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

    private void createAccountsMenu() {
        if (navigationAccounts != null) {
            navigationAccounts.setLayoutManager(getDrawerLayoutManager());
        }

        showingAccountsMenu = true;
        foldersDrawerLayout.setVisibility(View.GONE);
        accountsDrawerLayout.setVisibility(View.VISIBLE);

        List<Account> accounts = new ArrayList<>(preferences.getAccounts());
        accounts.remove(mAccount);
        AccountRenderer accountRenderer = new AccountRenderer();
        rendererAccountBuilder = new RendererBuilder<>(accountRenderer);

        accountRenderer.setOnAccountClickListenerListener(new OnAccountClickListener() {
            @Override
            public void onClick(final Account account) {
                mMessageListFragment.showLoadingMessages();
                mAccount = account;
                PePUIArtefactCache.getInstance(MessageList.this).setLastUsedAccount(mAccount);
                drawerCloseListener = new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {

                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        String folder = account.getAutoExpandFolderName();
                        LocalSearch search = new LocalSearch(folder);
                        search.addAccountUuid(mAccount.getUuid());
                        search.addAllowedFolder(folder);
                        refreshMessages(search);
                        setupNavigationHeader();
                        createFoldersMenu();
                        navFoldersAccountsButton.showAccounts();
                        drawerLayout.removeDrawerListener(drawerCloseListener);
                        changeAccountsOrder();
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {

                    }
                };
                drawerLayout.addDrawerListener(drawerCloseListener);
                drawerLayout.closeDrawers();
//                onOpenFolder(account.getAutoExpandFolderName());
            }
        });
        ListAdapteeCollection<Account> adapteeCollection = new ListAdapteeCollection<>(accounts);

        accountAdapter = new RVRendererAdapter<>(rendererAccountBuilder, adapteeCollection);
        if (navigationAccounts != null) {
            navigationAccounts.setAdapter(accountAdapter);
        }

        setupCreateAccountListener();
        configureAccountContainer = findViewById(R.id.configure_account_container);
        configureAccountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                onEditSettings();
            }
        });
    }

    @NonNull
    private LinearLayoutManager getDrawerLayoutManager() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        return linearLayoutManager;
    }

    private void setupCreateAccountListener() {
        addAccountContainer = findViewById(R.id.add_account_container);
        addAccountContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                AccountSetupBasics.actionNewAccount(MessageList.this);
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
        if (menuFolders != null && menuFolders.size() > 0 && mAccount != null
                && menuFolders.get(0).getAccountUuid().equals(mAccount.getUuid())) {
            populateFolders(menuFolders);
        } else if (mAccount != null) {
            MessagingController instance = MessagingController.getInstance(this);
            instance.listFolders(mAccount, false, new SimpleMessagingListener() {
                    @Override
                    public void listFolders(Account account, List<LocalFolder> folders) {
                        menuFolders = folders;
                        populateFolders(menuFolders);
                    }
                });
        }
    }

    private void populateFolders(List<LocalFolder> folders) {
        List<LocalFolder> foldersFiltered = filterLocalFolders(folders);
        runOnUiThread(() -> {
            drawerFolderPopulator.populateFoldersIfNeeded(folderAdapter, foldersFiltered, mAccount);
            setupMainFolders();
        });
    }

    @NonNull
    private List<LocalFolder> filterLocalFolders(List<LocalFolder> folders) {
        String allMessagesFolderName = getString(R.string.search_all_messages_title);
        String unifiedFolderName = getString(R.string.integrated_inbox_title);
        List<LocalFolder> foldersFiltered = new ArrayList<>();
        for (LocalFolder folder : folders) {
            if (!folder.getName().equals(allMessagesFolderName) &&
                    !folder.getName().equals(unifiedFolderName)) {
                foldersFiltered.add(folder);
            }
        }
        return foldersFiltered;
    }

    private  boolean isInboxFolder(LocalSearch search) {
        String inbox = mAccount.getIdentity(0).getEmail() +":"+ mAccount.getInboxFolderName();
        return search.getName().equals(inbox)
                || search.getName().equals(inbox + " - Starred");
    }

    private boolean isUnifiedInbox(LocalSearch search) {
        return search.getName().equals(getString(R.string.integrated_inbox_title));
    }

    private boolean isHomeScreen(LocalSearch search) {
        return (K9.startIntegratedInbox() && isUnifiedInbox(search)) || (!K9.startIntegratedInbox() && isInboxFolder(search));
    }

    private void changeFolder(final LocalFolder folder) {
        mFolderName = folder.getName();
        mMessageListFragment.showLoadingMessages();
        drawerCloseListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                LocalSearch search = getLocalSearch(mAccount, folder);
                MessageListFragment fragment = MessageListFragment.newInstance(search, false,
                        !mNoThreading);
                addMessageListFragment(fragment, !isHomeScreen(search));
                drawerLayout.removeDrawerListener(drawerCloseListener);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        };
        drawerLayout.addDrawerListener(drawerCloseListener);
        drawerLayout.closeDrawers();
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

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (isFinishing()) {
            return;
        }

        setIntent(intent);

        if (mFirstBackStackId >= 0) {
            getSupportFragmentManager().popBackStackImmediate(mFirstBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mFirstBackStackId = -1;
        }
        removeMessageListFragment();
        removeMessageViewFragment();

        mMessageReference = null;
        mSearch = null;

        initializeActionBar();
        if (!decodeExtras(intent)) {
            return;
        }

        initializeDisplayMode(null);
        initializeFragments();
        displayViews();

        channelUtils.updateChannels();
        initializeDrawerToggle();

    }

    private void initializeDrawerToggle() {
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, getToolbar(), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.removeDrawerListener(toggle);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    /**
     * Get references to existing fragments if the activity was restarted.
     */
    private void findFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        boolean hasMessageListFragment = (mMessageListFragment != null);

        if (!hasMessageListFragment) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            if(mSearch.searchAllAccounts() && specialAccountUuid != null) {
                if(specialAccountUuid.equals(SearchAccount.UNIFIED_INBOX) || specialAccountUuid.equals(SearchAccount.ALL_MESSAGES)) {
                    mMessageListFragment = MessageListFragment.newInstance(mSearch, false, false);
                }
                specialAccountUuid = null;
            }
            else {
                mMessageListFragment = MessageListFragment.newInstance(mSearch, false,
                         !mNoThreading);
            }
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

    private void refreshMessages(LocalSearch search) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        boolean hasMessageListFragment = (mMessageListFragment != null);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        mMessageListFragment = MessageListFragment.newInstance(search, false, !mNoThreading);
        if (!hasMessageListFragment) {
            ft.add(R.id.message_list_container, mMessageListFragment);
        } else {
            ft.replace(R.id.message_list_container, mMessageListFragment);
        }
        ft.commit();
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
        messageViewVisible = false;
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
            Collection<Account> accounts = preferences.getAvailableAccounts();
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
                Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    String accountExtra = appData.getString(EXTRA_SEARCH_ACCOUNT);
                    mSearch = new LocalSearch(getString(R.string.search_results));
                    if(accountExtra.equals(SearchAccount.UNIFIED_INBOX)) {
                        prepareSpecialManualSearch(accountExtra, query, SearchField.INTEGRATE);
                    }
                    else if(accountExtra.equals(SearchAccount.ALL_MESSAGES)) {
                        prepareSpecialManualSearch(accountExtra, query,SearchField.SEARCHABLE);
                    }
                    else {
                        mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
                        addManualSearchConditions(query);
                        if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                            mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
                        }
                    }
                }
                else {
                    mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
                }

                mSearch.setManualSearch(true);
                mNoThreading = true;

            }
        } else if (intent.hasExtra(EXTRA_SEARCH_OLD)) {
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH_OLD);
            mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
            mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        }

        if (mMessageReference == null) {
            String messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE);
            mMessageReference = MessageReference.parse(messageReferenceString);
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


        String[] accountUuids = mSearch.getAccountUuids();
        if (mSearch.searchAllAccounts()) {
            List<Account> accounts = preferences.getAccounts();
            mSingleAccountMode = (accounts.size() == 1);
            if (mSingleAccountMode) {
                mAccount = accounts.get(0);
            }
            else {
                mAccount = preferences.getDefaultAccount();
            }
        } else {
            mSingleAccountMode = (accountUuids.length == 1);
            if (mSingleAccountMode) {
                mAccount = preferences.getAccount(accountUuids[0]);
            }
        }
        mSingleFolderMode = mSingleAccountMode && (mSearch.getFolderNames().size() == 1);

        if (mSingleAccountMode && (mAccount == null || !mAccount.isAvailable(this))) {
            Timber.i("not opening MessageList of unavailable account");
            onAccountUnavailable();
            return false;
        }

        // now we know if we are in single account mode and need a subtitle
        mActionBarSubTitle.setVisibility((!mSingleFolderMode) ? View.GONE : View.VISIBLE);

        mActionBarSubTitle.setVisibility(Intent.ACTION_SEARCH.equals(intent.getAction()) || !mSingleFolderMode  ? View.GONE : View.VISIBLE);
        return true;
    }

    private void prepareSpecialManualSearch(String accountExtra, String query, SearchField searchField) {
        specialAccountUuid = accountExtra;
        mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
        addManualSearchConditions(query);
        mSearch.and(searchField, "1", Attribute.EQUALS);
    }

    private void addManualSearchConditions(String query) {
        mSearch.or(new SearchCondition(SearchField.SENDER, Attribute.CONTAINS, query));
        mSearch.or(new SearchCondition(SearchField.SUBJECT, Attribute.CONTAINS, query));
        mSearch.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));
        mSearch.or(new SearchCondition(SearchField.TO, Attribute.CONTAINS, query));

    }

    @Override
    public void onPause() {
        super.onPause();

        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
        drawerFolderPopulator.clearFolders();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!(this instanceof Search)) {
            //necessary b/c no guarantee Search.onStop will be called before MessageList.onResume
            //when returning from search results
            if(Search.isActive()) {
                Search.setActive(false);
                hideSearchView();
            }
        }

        if (mAccount != null && !mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);

        if (!messageViewVisible && !isThreadDisplayed) {
            toolBarCustomizer.setToolbarColor(Rating.pEpRatingTrustedAndAnonymized);
            toolBarCustomizer.setStatusBarPepColor(Rating.pEpRatingTrustedAndAnonymized);
        }

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            setDrawerEnabled(false);
        } else {
            setDrawerEnabled(true);
        }

        if (mAccount != null) {
            loadNavigationView();
        }
    }

    private void setupMainFoldersUnreadMessages() {
        unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
        allMessagesAccount = SearchAccount.createAllMessagesAccount(this);
        accountUtils.loadSearchAccountStats(this, unifiedInboxAccount,
                (account, stats) -> {
                    TextView unifiedInboxMessages = (TextView) findViewById(R.id.unified_inbox_new_messages);
                    setNewInboxMessages(stats, unifiedInboxMessages);
                });
        accountUtils.loadSearchAccountStats(this, allMessagesAccount,
                (account, stats) -> {
                    TextView allMessages = (TextView) findViewById(R.id.all_messages_new_messages);
                    setNewInboxMessages(stats, allMessages);});
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_DISPLAY_MODE, mDisplayMode);
        outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED, mMessageListWasDisplayed);
        outState.putInt(STATE_FIRST_BACK_STACK_ID, mFirstBackStackId);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mMessageListWasDisplayed = savedInstanceState.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED);
        mFirstBackStackId = savedInstanceState.getInt(STATE_FIRST_BACK_STACK_ID);
    }

    private void initializeActionBar() {
        setUpToolbar(true, v -> setDrawerEnabled(true));
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

    @SuppressLint("InflateParams")
    private View getActionButtonIndeterminateProgress() {
        return getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()
                && !isSearchViewVisible()) {
            ret = onCustomKeyDown(event.getKeyCode(), event);
        }
        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (isMessageViewVisible()) {
            setMessageViewVisible(false);
        }
        if (isSearchViewVisible()) {
            hideSearchView();
        }
        else {
            goBack();
        }
    }

    private void updateToolbarColorToOriginal() {
        toolBarCustomizer.setToolbarColor(Rating.pEpRatingFullyAnonymous);
        toolBarCustomizer.setStatusBarPepColor(Rating.pEpRatingFullyAnonymous);
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
            case KeyEvent.KEYCODE_DPAD_LEFT: {
                if (mMessageViewFragment != null && mDisplayMode == DisplayMode.MESSAGE_VIEW) {
                    return showPreviousMessage();
                }
                return false;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                if (mMessageViewFragment != null && mDisplayMode == DisplayMode.MESSAGE_VIEW) {
                    return showNextMessage();
                }
                return false;
            }

        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                Timber.v("Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void onAccounts() {
        SettingsActivity.Companion.listAccounts(this);
        finish();
    }

    private void onShowFolderList() {
        FolderList.actionHandleAccount(this, mAccount);
    }

    private void onEditSettings() {
        SettingsActivity.launch(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.compose: {
                MessageActions.actionCompose(this, mAccount);
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
            case R.id.settings: {
                onEditSettings();
                return true;
            }
            case R.id.search: {
                PePUIArtefactCache.getInstance(MessageList.this).setLastUsedAccount(mAccount);
                setDrawerEnabled(isAndroidLollipop());
                showSearchView();
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
            case R.id.tutorial: {
                WelcomeMessageKt.startTutorialMessage(this);
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
            case R.id.privacyStatus:
                mMessageViewFragment.onPEpPrivacyStatus(true);
                return true;
            case R.id.flag:
                mMessageViewFragment.onToggleFlagged();
                return true;
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
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        mMenuButtonCheckMail = menu.findItem(R.id.check_mail);
        flaggedCheckbox = menu.findItem(R.id.flag);
        return true;
    }

    private void checkFlagMenuItemChecked(boolean check) {
        if(check) {
            flaggedCheckbox.setIcon(resourcesProvider.getAttributeResource(R.attr.flagOpaqueCheckedIcon));
            flaggedCheckbox.setTitle(R.string.unflag_action);
        }
        else {
            flaggedCheckbox.setIcon(resourcesProvider.getAttributeResource(R.attr.flagOpaqueUncheckedIcon));
            flaggedCheckbox.setTitle(R.string.flag_action);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
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

        /*
         * Set visibility of menu items related to the message view
         */

        if (mDisplayMode == DisplayMode.MESSAGE_LIST
                || mMessageViewFragment == null
                || !mMessageViewFragment.isInitialized()) {
            toolBarCustomizer.colorizeToolbarActionItemsAndNavButton(ContextCompat.getColor(this,R.color.white));
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
            menu.findItem(R.id.show_headers).setVisible(false);
            menu.findItem(R.id.hide_headers).setVisible(false);
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            toolBarCustomizer.colorizeToolbarActionItemsAndNavButton(ContextCompat.getColor(this,R.color.light_black));
            menu.findItem(R.id.delete).setIcon(R.drawable.ic_delete_black_24dp);
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

            checkFlagMenuItemChecked(mMessageViewFragment.isMessageFlagged());

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
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.show_folder_list).setVisible(false);
            menu.findItem(R.id.privacyStatus).setVisible(true);
            setDrawerEnabled(false);
        } else {
            menu.findItem(R.id.privacyStatus).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(true);
            menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.compose).setVisible(true);
            menu.findItem(R.id.mark_all_as_read).setVisible(
                    mMessageListFragment.isMarkAllAsReadSupported());


            menu.findItem(R.id.send_messages).setVisible(mMessageListFragment.isOutbox());
            menu.findItem(R.id.show_folder_list).setVisible(true);
            setDrawerEnabled(true);

            if(isThreadDisplayed) {
                setDrawerEnabled(false);
            }

            menu.findItem(R.id.check_mail).setVisible(mMessageListFragment.isCheckMailSupported());
            // configure action bar in search screen
            if(mMessageListFragment.isManualSearch()) {
                setDrawerEnabled(false);
                menu.findItem(R.id.check_mail).setVisible(false);
                menu.findItem(R.id.compose).setVisible(false);
                menu.findItem(R.id.show_folder_list).setVisible(false);
                menu.findItem(R.id.settings).setVisible(false);
            }
            // If this is an explicit local search, show the option to search on the server
            if (!mMessageListFragment.isRemoteSearch() &&
                    mMessageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!mMessageListFragment.isManualSearch() && !isThreadDisplayed) {
                menu.findItem(R.id.search).setVisible(true);
            }
        }
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        SettingsActivity.Companion.listAccounts(this);
    }

    public void setActionBarTitle(String title) {
        mActionBarTitle.setText(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        if (subTitle != null && !subTitle.isEmpty()) {
            mActionBarSubTitle.setVisibility(View.VISIBLE);
            mActionBarSubTitle.setText(subTitle);
        } else {
            mActionBarSubTitle.setVisibility(View.GONE);
        }
    }

    public void setActionBarUnread(int unread) {
        // TODO: 08/06/17 Review
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
        populateDrawerGroup();
        setupMainFoldersUnreadMessages();
    }

    @Override
    public void setMessageListProgress(int progress) {
        setProgress(progress);
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        Account account = preferences.getAccount(messageReference.getAccountUuid());
        String folderName = messageReference.getFolderName();

        if (folderName.equals(account.getDraftsFolderName())) {
            MessageActions.actionEditDraft(this, messageReference);
        } else {
            mMessageViewContainer.removeView(mMessageViewPlaceHolder);

            if (mMessageListFragment != null) {
                mMessageListFragment.setActiveMessage(messageReference);
            }

            MessageViewFragment fragment = MessageViewFragment.newInstance(messageReference);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (direction == null || direction.equals(MessageSwipeDirection.FORWARD)) {
                ft.setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out);
            } else  {
                ft.setCustomAnimations(R.animator.fade_in_right, R.animator.fade_out);
            }
            resetDirection();
            ft.replace(R.id.message_view_container, fragment);
            mMessageViewFragment = fragment;
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();

            if (mDisplayMode != DisplayMode.SPLIT_VIEW) {
                showMessageView();
            }

            setUpToolbarHomeIcon(resourcesProvider.getAttributeResource(R.attr.iconActionCancel));

        }
    }

    private void resetDirection() {
        direction = MessageSwipeDirection.FORWARD;
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
    public void onReply(MessageReference messageReference,
                        Rating pEpRating) {
        onReply(messageReference, null,  pEpRating);
    }

    @Override
    public void onReply(MessageReference messageReference, Parcelable decryptionResultForReply,
                        Rating rating) {
        MessageActions.actionReply(this, messageReference, false, decryptionResultForReply, rating);
    }

    @Override
    public void onReplyAll(MessageReference messageReference, Rating pEpRating) {
        onReplyAll(messageReference, null, pEpRating);
    }

    @Override
    public void onReplyAll(MessageReference messageReference, Parcelable decryptionResultForReply,
                           Rating rating) {
        MessageActions.actionReply(this, messageReference, true, decryptionResultForReply, rating);
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

        MessageListFragment fragment = MessageListFragment.newInstance(
                tmpSearch, false, !mNoThreading);

        addMessageListFragment(fragment, !isHomeScreen(tmpSearch));
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
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.message_list_container, fragment);

        fm.popBackStack();

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
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(mMessageViewFragment);
            mMessageViewFragment = null;
            ft.commit();

            showDefaultTitleView();
        }
    }

    private void removeMessageListFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW && mMessageListWasDisplayed) {
            showMessageList();
        }
        else if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            if(isBackstackClear()) {
                if (mAccount != null) {
                    Router.onOpenAccount(this, mAccount);
                } else {
                    onAccountUnavailable();
                }
            } else {
                finish();
            }
        } else if (getIntent().getBooleanExtra(EXTRA_FOLDER, false)) {
            if(isBackstackClear()) {
                LocalSearch search = new LocalSearch(mAccount.getAutoExpandFolderName());
                search.addAllowedFolder(mAccount.getAutoExpandFolderName());
                search.addAccountUuid(mAccount.getUuid());
                Intent intent = MessageList.intentDisplaySearch(MessageList.this, search, false, false, true, false);
                finish();
                startActivity(intent);
            } else {
                finish();
            }
        } else {
            if(isThreadDisplayed) {
                updateToolbarColorToOriginal();
                isThreadDisplayed = false;
            }
            super.onBackPressed();
        }
    }

    private boolean isBackstackClear() {
        ActivityManager mngr = (ActivityManager) getSystemService( ACTIVITY_SERVICE );

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        return taskList.get(0).numActivities == 1 &&
                taskList.get(0).topActivity.getClassName().equals(this.getClass().getName());
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
        } else {
            mActionBarSubject.showSubjectInMessageHeader();
        }
    }

    @Override
    public void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
            if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder();
            } else {
                goBack();
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
        mMessageListWasDisplayed = true;
        mDisplayMode = DisplayMode.MESSAGE_LIST;
        mViewSwitcher.showFirstView();

        mMessageListFragment.setActiveMessage(null);
        removeMessageViewFragment();
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
    public void setDirection(MessageSwipeDirection direction) {
        this.direction = direction;
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
                StorageEditor editor = preferences.getStorage().edit();
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

        if (mMessageViewFragment != null) {
            mMessageViewFragment.onPendingIntentResult(requestCode, resultCode, data);
        }
    }

    public MessageViewFragment getMessageViewFragment() {
        return mMessageViewFragment;
    }
}
