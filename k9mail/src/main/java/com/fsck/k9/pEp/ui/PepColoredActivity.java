/*
Created by Helm  01/07/16.
*/


package com.fsck.k9.pEp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;

import org.pEp.jniadapter.Rating;

public class PepColoredActivity extends K9Activity {
    public static final String CURRENT_RATING = "current_color";
    public static final String PEP_COLOR_RATING_DETAIL_MESSAGE = "Cannot retrieve pEpRating";
    private Rating pEpRating = Rating.pEpRatingUndefined;
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void colorActionBar() {
        if (getToolbar() != null) {
            PEpUtils.colorToolbar(uiCache, getToolbar(), pEpRating);
        }
    }

    protected void loadPepRating() {
        final Intent intent = getIntent();
        String ratingString;
        if (intent.hasExtra(CURRENT_RATING)) {
            ratingString = intent.getStringExtra(CURRENT_RATING);
            Log.d(K9.LOG_TAG, "Got color:" + ratingString);
            pEpRating = Rating.valueOf(ratingString);
        } else {
            throw new RuntimeException(PEP_COLOR_RATING_DETAIL_MESSAGE);
        }
    }

    public void setpEpRating(Rating pEpRating) {
        this.pEpRating = pEpRating;
    }
    public Rating getpEpRating() {
        return pEpRating;
    }

    protected void initPep() {
        uiCache = PePUIArtefactCache.getInstance(getApplicationContext());
        pEp = ((K9) getApplication()).getpEpProvider();
    }

    public PEpProvider getpEp() {
        return pEp;
    }

    public PePUIArtefactCache getUiCache() {
        return uiCache;
    }
}
