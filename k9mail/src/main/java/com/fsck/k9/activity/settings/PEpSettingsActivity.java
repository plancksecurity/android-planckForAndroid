package com.fsck.k9.activity.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.fsck.k9.R;

public abstract class PEpSettingsActivity extends PreferenceActivity {

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        AppBarLayout bar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            bar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.simple_toolbar_layout, root, false);
            root.addView(bar, 0);
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);
            root.removeAllViews();
            bar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.simple_toolbar_layout, root, false);

            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }else{
                height = bar.getHeight();
            }

            content.setPadding(0, height, 0, 0);

            root.addView(content);
            root.addView(bar);
        }

        Toolbar Tbar = (Toolbar) bar.getChildAt(0);

        Tbar.setTitle(getTitle());
        Tbar.setTitleTextColor(getResources().getColor(R.color.white));
        Tbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference!=null) {
            if (preference instanceof PreferenceScreen) {
                if (((PreferenceScreen) preference).getDialog() != null) {
                    ((PreferenceScreen) preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
                    setUpNestedScreen((PreferenceScreen) preference);
                }
            }
        }

        return false;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        AppBarLayout appBar;

        View listRoot = dialog.findViewById(android.R.id.list);
        ViewGroup mRootView = (ViewGroup) dialog.findViewById(android.R.id.content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent().getParent();
            appBar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.simple_toolbar_layout, root, false);
            root.addView(appBar, 0);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
            appBar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.simple_toolbar_layout, root, false);
            root.addView(appBar, 0);
        } else {
            ListView content = (ListView) mRootView.getChildAt(0);
            mRootView.removeAllViews();

            LinearLayout LL = new LinearLayout(this);
            LL.setOrientation(LinearLayout.VERTICAL);

            ViewGroup.LayoutParams LLParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            LL.setLayoutParams(LLParams);

            appBar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.simple_toolbar_layout, mRootView, false);

            LL.addView(appBar);
            LL.addView(content);

            mRootView.addView(LL);
        }

        Toolbar Tbar = (Toolbar) appBar.getChildAt(0);

        Tbar.setTitle(preferenceScreen.getTitle());
        Tbar.setTitleTextColor(getResources().getColor(R.color.white));
        Tbar.setNavigationOnClickListener(v -> dialog.dismiss());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {

    }
}