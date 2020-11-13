package com.fsck.k9.activity;

import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.tools.KeyboardUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import security.pEp.mdm.RestrictionsListener;
import org.jetbrains.annotations.NotNull;

public abstract class K9Activity extends AppCompatActivity implements K9ActivityMagic{

    @Nullable @Bind(R.id.toolbar) Toolbar toolbar;
    @Nullable @Bind(R.id.toolbar_search_container) FrameLayout toolbarSearchContainer;
    @Nullable @Bind(R.id.search_input) EditText searchInput;
    @Nullable @Bind(R.id.search_clear) View clearSearchIcon;
    @Nullable @Bind(R.id.fab_button_compose_message) View fabButton;

    private static final String SHOWING_SEARCH_VIEW = "showingSearchView";
    private static final String K9ACTIVITY_SEARCH_TEXT = "searchText";

    private K9ActivityCommon mBase;
    private View.OnClickListener onCloseSearchClickListener;
    private boolean isAndroidLollipop = Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1;
    private boolean isShowingSearchView;
    private String searchText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getWindow().getDecorView().setSystemUiVisibility(
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
//        ((K9) getApplication()).pEpSyncProvider.setSyncHandshakeCallback(this);
        if(savedInstanceState != null) {

            isShowingSearchView = savedInstanceState.getBoolean(SHOWING_SEARCH_VIEW, false);

            searchText = savedInstanceState.getString(K9ACTIVITY_SEARCH_TEXT, null);


        }
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
    protected void onDestroy() {
        mBase = null;
        PePUIArtefactCache pePUIArtefactCache = PePUIArtefactCache.getInstance(getApplicationContext());
        pePUIArtefactCache.removeCredentialsInPreferences();
        super.onDestroy();
    }

//    @Override
//    public void showHandshake(Identity myself, Identity partner) {
//        Toast.makeText(getApplicationContext(), myself.fpr + "/n" + partner.fpr, Toast.LENGTH_LONG).show();
//        Log.i("pEp", "showHandshake: " + myself.fpr + "/n" + partner.fpr);
//    }

    public void setConfigurationManagerListener(RestrictionsListener listener) {
        mBase.setConfigurationManagerListener(listener);
    }

    public void setUpToolbar(boolean showUpButton) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            }
            if (K9.getK9Theme() == K9.Theme.DARK) {
                toolbar.setPopupTheme(R.style.PEpThemeOverlay);
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

    public ViewGroup getRootView() {
        return (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
    }

    public boolean isAndroidLollipop() {
        return isAndroidLollipop;
    }

    protected void showComposeFab(boolean show) {
        if (fabButton != null) {
            fabButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void showSearchView() {
        isShowingSearchView = true;
        if (isAndroidLollipop) {
            onSearchRequested();
            showComposeFab(false);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchManager.setOnDismissListener(() -> showComposeFab(true));
        } else if (toolbarSearchContainer != null && toolbar != null && searchInput != null) {
            toolbarSearchContainer.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.GONE);
            searchInput.setEnabled(true);
            setFocusOnKeyboard();
            searchInput.setError(null);
            showComposeFab(false);
            searchInput.setText(searchText);
        }
    }

    private void setFocusOnKeyboard() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    protected boolean isSearchViewVisible() {
        return toolbarSearchContainer != null && toolbarSearchContainer.getVisibility() == View.VISIBLE;
    }

    @Nullable @OnClick(R.id.search_clear)
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

    @Nullable @OnTextChanged(R.id.search_input)
    void onSearchInputChanged(CharSequence query) {
        if (clearSearchIcon != null) {
            if (query.toString().isEmpty()) {
                clearSearchIcon.setVisibility(View.GONE);
            } else {
                clearSearchIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    @Nullable @OnEditorAction(R.id.search_input)
    boolean onSearchInputSubmitted(KeyEvent keyEvent) {
        if (searchInput != null) {
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                String searchedText = searchInput.getText().toString();
                if (!searchedText.trim().isEmpty()) {
                    search(searchInput.getText().toString());
                    return true;
                }
                else {
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

    public abstract void search(String query);

    @Override
    protected void onResume() {
        super.onResume();
        mBase.registerPassphraseReceiver();
        mBase.registerConfigurationManager();
        if(isShowingSearchView) {
            showSearchView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBase.unregisterPassphraseReceiver();
        mBase.unregisterConfigurationManager();
        if(isShowingSearchView) {
            searchText = searchInput.getText().toString();
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(K9ACTIVITY_SEARCH_TEXT, searchText);
        outState.putBoolean(SHOWING_SEARCH_VIEW, isShowingSearchView);
    }
}
