package com.fsck.k9.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.tools.KeyboardUtils;

import org.pEp.jniadapter.Rating;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;


public abstract class K9Activity extends AppCompatActivity implements K9ActivityMagic {

    @Nullable @Bind(R.id.toolbar) Toolbar toolbar;
    @Nullable @Bind(R.id.toolbar_search_container) FrameLayout toolbarSearchContainer;
    @Nullable @Bind(R.id.search_input) EditText searchInput;
    @Nullable @Bind(R.id.search_clear) View clearSearchIcon;

    private K9ActivityCommon mBase;
    private View.OnClickListener onCloseSearchClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
//        ((K9) getApplication()).pEpSyncProvider.setSyncHandshakeCallback(this);

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
        mBase.onDestroy();
        PePUIArtefactCache pePUIArtefactCache = PePUIArtefactCache.getInstance(getApplicationContext());
        pePUIArtefactCache.removeCredentialsInPreferences();
        super.onDestroy();
    }

//    @Override
//    public void showHandshake(Identity myself, Identity partner) {
//        Toast.makeText(getApplicationContext(), myself.fpr + "/n" + partner.fpr, Toast.LENGTH_LONG).show();
//        Log.i("pEp", "showHandshake: " + myself.fpr + "/n" + partner.fpr);
//    }



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

    public void setUpToolbar(boolean showUpButton, View.OnClickListener onCloseSearchClickListener) {
        setUpToolbar(showUpButton);
        this.onCloseSearchClickListener = onCloseSearchClickListener;
    }

    @Nullable @OnClick(R.id.search_clear)
    void onClearSeachClicked() {
        hideSearchView();
        if (onCloseSearchClickListener != null) {
            onCloseSearchClickListener.onClick(null);
        }
        searchInput.setText(null);
        KeyboardUtils.hideKeyboard(searchInput);
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

    public void setStatusBarPepColor(Rating pEpRating) {
        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PePUIArtefactCache uiCache = PePUIArtefactCache.getInstance(getApplicationContext());
            int color = (uiCache.getColor(pEpRating) & 0x00FFFFFF);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            float[] hsv = new float[3];
            Color.RGBToHSV(red, green, blue, hsv);
            hsv[2] = hsv[2]*0.9f;
            color = Color.HSVToColor(hsv);
            window.setStatusBarColor(color);
        }
    }

    public void setStatusBarPepColor(Integer colorReference) {
        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PePUIArtefactCache uiCache = PePUIArtefactCache.getInstance(getApplicationContext());
            int color = (colorReference & 0x00FFFFFF);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            float[] hsv = new float[3];
            Color.RGBToHSV(red, green, blue, hsv);
            hsv[2] = hsv[2]*0.9f;
            color = Color.HSVToColor(hsv);
            window.setStatusBarColor(color);
        }
    }
    public ViewGroup getRootView() {
        return (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
    }

    public void showSearchView() {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            onSearchRequested();
        } else {
            if (toolbarSearchContainer != null && toolbar != null) {
                toolbarSearchContainer.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.GONE);
                setFocusOnKeyboard();
            }
        }
    }

    private void setFocusOnKeyboard() {
        if (searchInput != null) {
            searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void hideSearchView() {
        if (toolbarSearchContainer != null && toolbar != null) {
            toolbarSearchContainer.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
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
            String searchedText = searchInput.getText().toString();
            if (!searchedText.trim().isEmpty()) {
                search(searchInput.getText().toString());
                return true;
            }
        }
        return false;
    }

    @Nullable @OnClick(R.id.search_clear)
    void onClearText() {
        if (searchInput != null) {
            searchInput.setText(null);
            hideSearchView();
            KeyboardUtils.hideKeyboard(searchInput);
        }
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
}
