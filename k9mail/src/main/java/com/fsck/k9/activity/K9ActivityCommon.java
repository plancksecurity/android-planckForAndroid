package com.fsck.k9.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.misc.SwipeGestureDetector;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.pEp.LangUtils;
import com.fsck.k9.pEp.ui.tools.ThemeManager;

import java.util.Locale;

import security.pEp.mdm.ConfigurationManager;
import security.pEp.mdm.RestrictionsListener;
import security.pEp.ui.passphrase.PassphraseActivity;
import security.pEp.ui.passphrase.PassphraseActivityKt;
import timber.log.Timber;


/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 * @see K9ListActivity
 */
public class K9ActivityCommon {
    private PassphraseRequestReceiver passphraseReceiver;
    private IntentFilter passphraseReceiverfilter;
    private ConfigurationManager configurationManager;

    private static final boolean isAndroidLollipop = Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1;

    /**
     * Creates a new instance of {@link K9ActivityCommon} bound to the specified activity.
     *
     * @param activity
     *         The {@link Activity} the returned {@code K9ActivityCommon} instance will be bound to.
     *
     * @return The {@link K9ActivityCommon} instance that will provide the base functionality of the
     *         "K9" activities.
     */
    public static K9ActivityCommon newInstance(Activity activity) {
        return new K9ActivityCommon(activity);
    }

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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Locale.setDefault(locale);
        else {
            Configuration systemConfig = Resources.getSystem().getConfiguration();
            systemConfig.setLocale(locale);
            Resources.getSystem().updateConfiguration(systemConfig, null);
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private static void invalidateChromeLocaleForWebView(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            new WebView(context).destroy();
        }
    }

    public void setConfigurationManagerListener(RestrictionsListener listener) {
        configurationManager.setListener(listener);
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


    private K9ActivityCommon(Activity activity) {
        mActivity = activity;
        setLanguage(mActivity, K9.getK9Language());
        mActivity.setTheme(ThemeManager.getAppThemeResourceId());
        initPassphraseRequestReceiver();
        initConfigurationManager();
        configureNavigationBar(activity);
    }

    private void initConfigurationManager() {
        Preferences preferences = Preferences.getPreferences(mActivity);
        configurationManager = new ConfigurationManager(mActivity, preferences);
    }

    public static void configureNavigationBar(Activity activity) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ThemeManager.isDarkTheme()) {
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
        passphraseReceiver = new PassphraseRequestReceiver();
        passphraseReceiverfilter = new IntentFilter();
        passphraseReceiverfilter.addAction(PassphraseActivityKt.PASSPHRASE_REQUEST_ACTION);
        passphraseReceiverfilter.setPriority(1);
    }

    public void registerPassphraseReceiver() {
        Timber.e("pEpEngine-passphrase register receiver");
        mActivity.getApplicationContext()
                .registerReceiver(passphraseReceiver, passphraseReceiverfilter);
    }

    public void unregisterPassphraseReceiver() {
        mActivity.getApplicationContext().unregisterReceiver(passphraseReceiver);
    }

    public void registerConfigurationManager() {
        configurationManager.registerReceiver();
    }

    public void unregisterConfigurationManager() {
        configurationManager.unregisterReceiver();
    }

    public static class PassphraseRequestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.e("pEpEngine-passphrase, onReceive");
            PassphraseActivity.launchIntent(context, intent);
        }
    }

    public static boolean isAndroidLollipop() {
        return isAndroidLollipop;
    }
}
