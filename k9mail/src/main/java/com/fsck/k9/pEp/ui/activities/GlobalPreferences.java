package com.fsck.k9.pEp.ui.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;

import org.pEp.jniadapter.Rating;

public class GlobalPreferences extends K9Activity {

    public static void actionPrefs(Context context) {
        Intent i = new Intent(context, GlobalPreferences.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_preferences);
        initializeToolbar();
        setupPreferences();
    }

    private void setupPreferences() {
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        Prefs mPrefsFragment = new Prefs();
        mFragmentTransaction.replace(R.id.preferences_content, mPrefsFragment);
        mFragmentTransaction.commit();
    }

    private void initializeToolbar() {
        initializeToolbar(true, R.string.prefs_title);
        PEpUtils.colorToolbar(PePUIArtefactCache.getInstance(getApplicationContext()), getToolbar(), Rating.pEpRatingTrustedAndAnonymized);
    }
}
