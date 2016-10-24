package com.fsck.k9.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.KeyboardUtils;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;
import org.pEp.jniadapter.Sync;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public abstract class K9Activity extends AppCompatActivity implements K9ActivityMagic, Sync.showHandshakeCallback {

    @Nullable @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.toolbar_search_container) FrameLayout toolbarSearchContainer;
    @Bind(R.id.search_input) EditText searchInput;
    @Bind(R.id.search_clear) View clearSearchIcon;

    private K9ActivityCommon mBase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
        ((K9) getApplication()).pEpSyncProvider.setSyncHandshakeCallback(this);
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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }

    @Override
    protected void onDestroy() {
        mBase.onDestroy();
        super.onDestroy();
    }

    @Override
    public void showHandshake(Identity myself, Identity partner) {
        FeedbackTools.showLongFeedback(getRootView(), myself.fpr + "/n" + partner.fpr);
        Log.i("pEp", "showHandshake: " + myself.fpr + "/n" + partner.fpr);
    }

    public void setUpToolbar(boolean showUpButton) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            }
        }
    }

    @OnClick(R.id.search_clear) void onSearchClearClicked() {
        toolbarSearchContainer.setVisibility(View.GONE);
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
            getSupportActionBar().setTitle(getString(stringResource));
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
    public View getRootView() {
        return getWindow().getDecorView().getRootView();
    }

    public void showSearchView() {
        if (toolbarSearchContainer != null) {
            toolbarSearchContainer.setVisibility(View.VISIBLE);
        }
    }

    public void hideSearchView() {
        if (toolbarSearchContainer != null) {
            toolbarSearchContainer.setVisibility(View.GONE);
        }
    }

    @OnTextChanged(R.id.search_input)
    void onSearchInputChanged(CharSequence query) {
        if (query.toString().isEmpty()) {
            clearSearchIcon.setVisibility(View.GONE);
        } else {
            clearSearchIcon.setVisibility(View.VISIBLE);
        }
    }

    @OnEditorAction(R.id.search_input)
    boolean onSearchInputSubmitted(KeyEvent keyEvent) {
        if (!searchInput.getText().toString().isEmpty()) {
            search(searchInput.getText().toString());
        }
        return true;
    }

    @OnClick(R.id.search_clear)
    void onClearText() {
        searchInput.setText(null);
        toolbarSearchContainer.setVisibility(View.GONE);
        KeyboardUtils.hideKeyboard(searchInput);
    }

    public void bindViewsForLayout(int layoutId) {
        setContentView(layoutId);
        ButterKnife.bind(this);
    }

    public void bindViewsForView(View view) {
        setContentView(view);
        ButterKnife.bind(this);
    }

    public abstract void search(String query);
}
