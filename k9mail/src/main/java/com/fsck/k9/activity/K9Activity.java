package com.fsck.k9.activity;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.activity.setup.OAuthFlowActivity;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.ui.activities.SplashActivity;
import com.fsck.k9.planck.ui.tools.KeyboardUtils;
import com.fsck.k9.planck.ui.tools.ThemeManager;
import com.scottyab.rootbeer.RootBeer;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import security.planck.auth.OAuthTokenRevokedListener;
import security.planck.dialog.ConfirmationDialog;
import security.planck.dialog.ConfirmationDialogKt;
import security.planck.mdm.RestrictionsViewModel;
import security.planck.passphrase.PassphraseDialogMode;
import security.planck.passphrase.PassphraseManagementDialogKt;
import security.planck.passphrase.PassphraseUnlockRetryState;
import security.planck.passphrase.PassphraseUnlockViewModel;
import security.planck.ui.audit.AuditLogViewModel;
import timber.log.Timber;

public abstract class K9Activity extends AppCompatActivity implements K9ActivityMagic,
        OAuthTokenRevokedListener {

    @Nullable
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Nullable
    @Bind(R.id.toolbar_search_container)
    FrameLayout toolbarSearchContainer;
    @Nullable
    @Bind(R.id.search_input)
    EditText searchInput;
    @Nullable
    @Bind(R.id.search_clear)
    View clearSearchIcon;

    private static final String SHOWING_SEARCH_VIEW = "showingSearchView";
    private static final String K9ACTIVITY_SEARCH_TEXT = "searchText";
    private static final String AUDIT_LOG_TAMPER_DIALOG_TAG = "auditLogTamperConfirmationDialog";
    private static final String MDM_ACCOUNTS_REMOVED_DIALOG_TAG = "mdmAccountsRemovedConfirmationDialog";
    private static final String WRONG_MDM_ACCOUNT_SETTINGS_DIALOG_TAG = "wrongMdmAccountSettingsConfirmationDialog";

    @Inject
    K9ActivityCommon mBase;
    private View.OnClickListener onCloseSearchClickListener;
    private boolean isShowingSearchView;
    private String searchText;

    public static final int NO_ANIMATION = 0;
    protected AuditLogViewModel auditLogViewModel;
    protected RestrictionsViewModel restrictionsViewModel;
    protected PassphraseUnlockViewModel passphraseUnlockViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setSystemUiVisibility(
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        super.onCreate(savedInstanceState);
//        ((K9) getApplication()).pEpSyncProvider.setSyncHandshakeCallback(this);
        if (savedInstanceState != null) {

            isShowingSearchView = savedInstanceState.getBoolean(SHOWING_SEARCH_VIEW, false);

            searchText = savedInstanceState.getString(K9ACTIVITY_SEARCH_TEXT, null);
        }
        auditLogViewModel = new ViewModelProvider(this).get(AuditLogViewModel.class);
        restrictionsViewModel = new ViewModelProvider(this).get(RestrictionsViewModel.class);
        passphraseUnlockViewModel = new ViewModelProvider(this).get(PassphraseUnlockViewModel.class);
        initializeFragmentListeners();
    }

    private void initializeFragmentListeners() {
        initializeAuditLogAlertFragmentListener();
        initializeAccountsRemovedFragmentListener();
        initializeWrongMdmAccountSettingsFragmentListener();
    }

    private void initializeAuditLogAlertFragmentListener() {
        getSupportFragmentManager().setFragmentResultListener(
                AUDIT_LOG_TAMPER_DIALOG_TAG,
                this,
                (requestKey, bundle) -> {
                    if (requestKey.equals(AUDIT_LOG_TAMPER_DIALOG_TAG)) {
                        int result = bundle.getInt(ConfirmationDialog.RESULT_KEY);
                        if (result == DialogInterface.BUTTON_POSITIVE) {
                            auditLogViewModel.auditTamperingCloseApp(() -> {
                                finishAndRemoveTask();
                                System.exit(0);
                                return null;
                            });
                        } else if (result == DialogInterface.BUTTON_NEGATIVE) {
                            auditLogViewModel.auditTamperingAlertDismissed();
                        }
                    }
                });
    }

    private void initializeAccountsRemovedFragmentListener() {
        getSupportFragmentManager().setFragmentResultListener(
                MDM_ACCOUNTS_REMOVED_DIALOG_TAG,
                this,
                (requestKey, bundle) -> {
                    if (requestKey.equals(MDM_ACCOUNTS_REMOVED_DIALOG_TAG)) {
                        restartApp();
                    }
                });
    }

    private void initializeWrongMdmAccountSettingsFragmentListener() {
        getSupportFragmentManager().setFragmentResultListener(
                WRONG_MDM_ACCOUNT_SETTINGS_DIALOG_TAG,
                this,
                (requestKey, bundle) -> {
                    if (requestKey.equals(WRONG_MDM_ACCOUNT_SETTINGS_DIALOG_TAG)) {
                        restrictionsViewModel.resetWrongAccountSettingsWarning();
                    }
                });
    }

    private void restartApp() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAndRemoveTask();
        System.exit(0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }

    @Override
    public void removeGestureDetector() {
        mBase.onPause();
    }

    @Override
    protected void onDestroy() {
        PlanckUIArtefactCache planckUIArtefactCache = PlanckUIArtefactCache.getInstance(getApplicationContext());
        planckUIArtefactCache.removeCredentialsInPreferences();
        super.onDestroy();
    }

    public void setUpToolbar(boolean showUpButton) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            }
            if (ThemeManager.isDarkTheme()) {
                toolbar.setPopupTheme(R.style.planckThemeOverlay);
            }
        }
    }

    public void setUpToolbarHomeIcon(@DrawableRes int drawable) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(drawable);
        }
    }

    public void setUpToolbar(boolean showUpButton, View.OnClickListener onCloseSearchClickListener) {
        setUpToolbar(showUpButton);
        this.onCloseSearchClickListener = onCloseSearchClickListener;
    }

    public K9 getK9() {
        return (K9) getApplication();
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void initializeToolbar(Boolean showUpButton, @StringRes int stringResource) {
        setUpToolbar(showUpButton);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(stringResource));
        }
    }

    public void initializeToolbar(Boolean showUpButton, String title) {
        setUpToolbar(showUpButton);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(title);
        }
    }

    public View getRootView() {
        return (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
    }

    protected void showComposeFab(boolean show) {
    }

    public void showSearchView() {
        isShowingSearchView = true;
        if (toolbarSearchContainer != null && toolbar != null && searchInput != null) {
            toolbarSearchContainer.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.GONE);
            searchInput.setEnabled(true);
            setFocusOnKeyboard();
            searchInput.setError(null);
            showComposeFab(false);
            searchInput.setText(searchText);
        }
    }

    public boolean isShowingSearchView() {
        return isShowingSearchView;
    }

    private void setFocusOnKeyboard() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    protected boolean isSearchViewVisible() {
        return toolbarSearchContainer != null && toolbarSearchContainer.getVisibility() == View.VISIBLE;
    }

    @Nullable
    @OnClick(R.id.search_clear)
    public void hideSearchView() {
        isShowingSearchView = false;
        searchText = null;

        if (searchInput != null &&
                toolbarSearchContainer != null && toolbar != null) {
            toolbarSearchContainer.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            searchInput.setEnabled(false);
            searchInput.setText(null);
            KeyboardUtils.hideKeyboard(searchInput);
            if (onCloseSearchClickListener != null) {
                onCloseSearchClickListener.onClick(null);
            }
            showComposeFab(true);
        }
    }

    @Nullable
    @OnTextChanged(R.id.search_input)
    void onSearchInputChanged(CharSequence query) {
        if (clearSearchIcon != null) {
            if (query.toString().isEmpty()) {
                clearSearchIcon.setVisibility(View.GONE);
            } else {
                clearSearchIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    @Nullable
    @OnEditorAction(R.id.search_input)
    boolean onSearchInputSubmitted(KeyEvent keyEvent) {
        if (searchInput != null) {
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                String searchedText = searchInput.getText().toString();
                if (!searchedText.trim().isEmpty()) {
                    search(searchInput.getText().toString());
                    return true;
                } else {
                    searchInput.setError(getString(R.string.search_empty_error));
                }
            }
        }
        return false;
    }

    public void bindViews(@LayoutRes int layoutId) {
        setContentView(layoutId);
        ButterKnife.bind(this);
    }

    public void bindViews(View view) {
        setContentView(view);
        ButterKnife.bind(this);
    }

    public void search(String query) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isRoot = new RootBeer(this).isRooted();
        if (isRoot && !BuildConfig.DEBUG) {
            Toast.makeText(this, R.string.rooted_device_error, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (BuildConfig.DEBUG) {
            Timber.i("Device is (possibly) rooted: %s", isRoot);
        }

        observeViewModels();

        mBase.registerPassphraseReceiver();
        mBase.registerOAuthTokenRevokedReceiver();
        if (isShowingSearchView) {
            showSearchView();
        } else {
            KeyboardUtils.hideKeyboard(this);
        }
    }

    private void observeViewModels() {
        observeAuditLogViewModel();
        if (getK9().isRunningOnWorkProfile()) {
            observeRestrictionsViewModel();
        }
        observePassphraseUnlockViewModel();
    }

    private void observeAuditLogViewModel() {
        auditLogViewModel.getTamperAlert().observe(this, event -> {
            Boolean value = event.getContentIfNotHandled();
            if (value != null && value) {
                ConfirmationDialogKt.showConfirmationDialog(
                        this,
                        AUDIT_LOG_TAMPER_DIALOG_TAG,
                        getString(R.string.audit_log_tamper_dialog_title),
                        getString(R.string.audit_log_tamper_dialog_description),
                        getString(R.string.audit_log_tamper_dialog_positive_button),
                        getString(R.string.ok)
                );
            }
        });
    }

    private void observePassphraseUnlockViewModel() {
        passphraseUnlockViewModel.getPassphraseUnlockRetry().observe(this, event -> {
            PassphraseUnlockRetryState value = event.getContentIfNotHandled();
            if (value != null) {
                if (value.equals(PassphraseUnlockRetryState.TimeToStart.INSTANCE)) {
                    PassphraseManagementDialogKt.showPassphraseManagementDialog(this, PassphraseDialogMode.UNLOCK);
                } else if (value.equals(PassphraseUnlockRetryState.FinishApp.INSTANCE)) {
                    finishAndRemoveTask();
                    System.exit(0);
                } else if (value instanceof PassphraseUnlockRetryState.TimeToRetry) {
                    PassphraseManagementDialogKt.showPassphraseManagementDialog(this, PassphraseDialogMode.UNLOCK, ((PassphraseUnlockRetryState.TimeToRetry) value).getAccountsWithError());
                }
            }
        });
    }

    private void observeRestrictionsViewModel() {
        restrictionsViewModel.getAccountRemoved().observe(this, event -> {
            Boolean value = event.getContentIfNotHandled();
            if (value != null && value) {
                ConfirmationDialogKt.showConfirmationDialog(
                        this,
                        MDM_ACCOUNTS_REMOVED_DIALOG_TAG,
                        getString(R.string.mdm_account_removed_dialog_title),
                        getString(R.string.mdm_account_removed_dialog_description),
                        getString(R.string.ok)
                );
            }
        });
        restrictionsViewModel.getWrongAccountSettings().observe(this, event -> {
            Boolean value = event.getContentIfNotHandled();
            if (value != null && value) {
                ConfirmationDialogKt.showConfirmationDialog(
                        this,
                        WRONG_MDM_ACCOUNT_SETTINGS_DIALOG_TAG,
                        getString(R.string.mdm_wrong_account_settings_dialog_title),
                        getString(R.string.mdm_wrong_account_settings_dialog_description),
                        getString(R.string.ok)
                );
            }
        });
    }

    @Override
    public void onTokenRevoked(@NonNull String accountUuid) {
        blockAppInOAuthScreen(accountUuid);
    }

    private void blockAppInOAuthScreen(@NotNull String accountUuid) {
        OAuthFlowActivity.Companion.startOAuthFlowOnTokenRevoked(this, accountUuid);
        finishAffinity();
    }

    @Override
    protected void onPause() {
        stopObservingViewModels();
        mBase.unregisterPassphraseReceiver();
        mBase.unregisterOAuthTokenRevokedReceiver();
        if (isShowingSearchView) {
            searchText = searchInput.getText().toString();
        }
        super.onPause();
    }

    private void stopObservingViewModels() {
        auditLogViewModel.getTamperAlert().removeObservers(this);
        restrictionsViewModel.getAccountRemoved().removeObservers(this);
        restrictionsViewModel.getWrongAccountSettings().removeObservers(this);
        passphraseUnlockViewModel.getPassphraseUnlockRetry().removeObservers(this);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(K9ACTIVITY_SEARCH_TEXT, searchText);
        outState.putBoolean(SHOWING_SEARCH_VIEW, isShowingSearchView);
    }
}
