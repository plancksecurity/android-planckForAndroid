package com.fsck.k9.activity;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.fsck.k9.K9;
import com.fsck.k9.R;

import security.pEp.ui.toolbar.PEpToolbarCustomizer;
import security.pEp.ui.toolbar.ToolBarCustomizer;


public abstract class K9PreferenceActivity extends PreferenceActivity implements LifecycleOwner {
    private LifecycleRegistry lifecycleRegistry;

    private AppCompatDelegate mDelegate;
    private Toolbar toolbar;

    ToolBarCustomizer toolBarCustomizer;

    @Override
    public void onCreate(Bundle icicle) {
        K9ActivityCommon.setLanguage(this, K9.getK9Language());
        setTheme(K9.getK9ThemeResourceId());
        super.onCreate(icicle);
        toolBarCustomizer = new PEpToolbarCustomizer(this);
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.markState(State.CREATED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleRegistry.markState(State.STARTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleRegistry.markState(State.RESUMED);
    }

    @Override
    protected void onPause() {
        lifecycleRegistry.markState(State.STARTED);
        super.onPause();
    }

    @Override
    protected void onStop() {
        lifecycleRegistry.markState(State.CREATED);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lifecycleRegistry.markState(State.DESTROYED);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // see https://developer.android.com/topic/libraries/architecture/lifecycle.html#onStop-and-savedState
        lifecycleRegistry.markState(State.CREATED);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        LinearLayout bar;

        ListView list = findViewById(android.R.id.list);
        ListView.MarginLayoutParams layoutParams = (ListView.MarginLayoutParams) list.getLayoutParams();
        ViewGroup root = (ViewGroup) list.getParent().getParent().getParent();
        bar = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
        layoutParams.setMargins(0, bar.getHeight(), 0, 0);
        root.addView(bar, 0); // insert at top

        this.toolbar = (Toolbar) bar.getChildAt(0);
        this.toolbar.setNavigationOnClickListener(v -> finish());
        setStatusBar();
    }

    public void setStatusBar() {
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolBarCustomizer.setToolbarColor(getResources().getColor(R.color.colorPrimary));
        toolBarCustomizer.setStatusBarPepColor(getResources().getColor(R.color.colorPrimary));
    }

    /**
     * Set up the {@link ListPreference} instance identified by {@code key}.
     *
     * @param key   The key of the {@link ListPreference} object.
     * @param value Initial value for the {@link ListPreference} object.
     * @return The {@link ListPreference} instance identified by {@code key}.
     */
    protected ListPreference setupListPreference(final String key, final String value) {
        final ListPreference prefView = (ListPreference) findPreference(key);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
        return prefView;
    }

    /**
     * Initialize a given {@link ListPreference} instance.
     *
     * @param prefView    The {@link ListPreference} instance to initialize.
     * @param value       Initial value for the {@link ListPreference} object.
     * @param entries     Sets the human-readable entries to be shown in the list.
     * @param entryValues The array to find the value to save for a preference when an
     *                    entry from entries is selected.
     */
    protected void initListPreference(final ListPreference prefView, final String value,
                                      final CharSequence[] entries, final CharSequence[] entryValues) {
        prefView.setEntries(entries);
        prefView.setEntryValues(entryValues);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
    }

    /**
     * This class handles value changes of the {@link ListPreference} objects.
     */
    private static class PreferenceChangeListener implements Preference.OnPreferenceChangeListener {

        private ListPreference mPrefView;

        private PreferenceChangeListener(final ListPreference prefView) {
            this.mPrefView = prefView;
        }

        /**
         * Show the preference value in the preference summary field.
         */
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String summary = newValue.toString();
            final int index = mPrefView.findIndexOfValue(summary);
            mPrefView.setSummary(mPrefView.getEntries()[index]);
            mPrefView.setValue(summary);
            return false;
        }

    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the screen
        if (preference instanceof PreferenceScreen) {
            setUpNestedScreen((PreferenceScreen) preference);
        }

        return true;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        setTheme(K9.getK9ThemeResourceId());

        LinearLayout bar;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            ListView content = dialog.findViewById(android.R.id.list);
            ViewGroup root = (ViewGroup) content.getParent().getParent();
            bar = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);

            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                height = bar.getHeight();
            }
            content.setPadding(0, height, 0, 0);
            root.addView(bar, 0); // insert at top
        } else {
            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent().getParent();
            bar = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
            root.addView(bar, 0); // insert at top
        }
        toolbar = (Toolbar) bar.getChildAt(0);
        toolbar.setTitle(preferenceScreen.getTitle());
        setStatusBar();
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
    }
}
