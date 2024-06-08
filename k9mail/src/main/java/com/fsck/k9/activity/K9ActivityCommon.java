package com.fsck.k9.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.SwipeGestureDetector;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.planck.LangUtils;
import com.fsck.k9.planck.ui.tools.ThemeManager;

import java.util.Locale;

import javax.inject.Inject;

import security.planck.auth.OAuthTokenRevokedReceiver;
import security.planck.mdm.ConfigurationManager;
import security.planck.ui.passphrase.old.PassphraseActivity;
import security.planck.ui.passphrase.old.PassphraseActivityKt;
import timber.log.Timber;


/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 */
public class K9ActivityCommon {
    private PassphraseRequestReceiver passphraseReceiver;
    private IntentFilter passphraseReceiverfilter;
    private OAuthTokenRevokedReceiver oAuthTokenRevokedReceiver;

    public static void setLanguage(Context context, String language) {
        invalidateChromeLocaleForWebView(context);
        Locale locale;
        if (TextUtils.isEmpty(language)) {
            locale = LangUtils.getDefaultLocale();
        } else if (language.length() == 5 && language.charAt(2) == '_') {
            // language is in the form: en_US
            locale = new Locale(language.substring(0, 2), language.substring(3));
        } else {
            locale = new Locale(language);
        }

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        Locale.setDefault(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private static void invalidateChromeLocaleForWebView(Context context) {
        new WebView(context).destroy();
    }

    /**
     * Base activities need to implement this interface.
     *
     * <p>The implementing class simply has to call through to the implementation of these methods
     * in {@link K9ActivityCommon}.</p>
     */
    public interface K9ActivityMagic {
        void setupGestureDetector(OnSwipeGestureListener listener);

        void removeGestureDetector();
    }


    private Activity mActivity;
    private GestureDetector mGestureDetector;
    private SwipeGestureDetector swipeGestureDetector;

    @Inject
    public K9ActivityCommon(
            Activity activity,
            ConfigurationManager configurationManager,
            PassphraseRequestReceiver passphraseRequestReceiver,
            IntentFilter passphraseReceiverfilter,
            OAuthTokenRevokedReceiver oAuthTokenRevokedReceiver
    ) {
        mActivity = activity;
        setLanguage(mActivity, K9.getK9Language());
        mActivity.setTheme(ThemeManager.getAppThemeResourceId());
        this.passphraseReceiverfilter = passphraseReceiverfilter;
        initPassphraseRequestReceiver();
        this.oAuthTokenRevokedReceiver = oAuthTokenRevokedReceiver;
        this.passphraseReceiver = passphraseRequestReceiver;
        this.passphraseReceiverfilter = passphraseReceiverfilter;
        configureNavigationBar(activity);
    }

    public static void configureNavigationBar(Activity activity) {
        if(ThemeManager.isDarkTheme()) {
            View decorView = activity.getWindow().getDecorView();
            int vis = decorView.getSystemUiVisibility();
            vis &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decorView.setSystemUiVisibility(vis);
        }
    }

    /**
     * Call this before calling {@code super.dispatchTouchEvent(MotionEvent)}.
     */
    public void preDispatchTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
    }

    /**
     * Get the background color of the theme used for this activity.
     *
     * @return The background color of the current theme.
     */
    public int getThemeBackgroundColor() {
        TypedArray array = mActivity.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.colorBackground });

        int backgroundColor = array.getColor(0, 0xFF00FF);

        array.recycle();

        return backgroundColor;
    }

    /**
     * Call this if you wish to use the swipe gesture detector.
     *
     * @param listener
     *         A listener that will be notified if a left to right or right to left swipe has been
     *         detected.
     */
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        if (mGestureDetector == null) {
            swipeGestureDetector = new SwipeGestureDetector(mActivity,listener);
            mGestureDetector = new GestureDetector(mActivity, swipeGestureDetector);
        } else{
            swipeGestureDetector.setListener(listener);
        }
    }

    public void onPause() {
        if (swipeGestureDetector != null)
            swipeGestureDetector.onPause();
        swipeGestureDetector = null;
        mGestureDetector = null;
    }

    private void initPassphraseRequestReceiver() {
        passphraseReceiverfilter.addAction(PassphraseActivityKt.PASSPHRASE_REQUEST_ACTION);
        passphraseReceiverfilter.setPriority(1);
    }

    private void initOAuthTokenRevokedReceiver() {
        oAuthTokenRevokedReceiver = new OAuthTokenRevokedReceiver();
    }

    public void registerPassphraseReceiver() {
        Timber.e("pEpEngine-passphrase register receiver");
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext())
                .registerReceiver(passphraseReceiver, passphraseReceiverfilter);
    }

    public void unregisterPassphraseReceiver() {
        LocalBroadcastManager.getInstance(mActivity.getApplicationContext())
                .unregisterReceiver(passphraseReceiver);
    }

    public void registerOAuthTokenRevokedReceiver() {
        oAuthTokenRevokedReceiver.register(mActivity);
    }

    public void unregisterOAuthTokenRevokedReceiver() {
        oAuthTokenRevokedReceiver.unregister(mActivity);
    }

    public static class PassphraseRequestReceiver extends BroadcastReceiver {

        @Inject
        public PassphraseRequestReceiver() {}

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.e("pEpEngine-passphrase, onReceive");
            PassphraseActivity.launchIntent(context, intent);
        }
    }

}
