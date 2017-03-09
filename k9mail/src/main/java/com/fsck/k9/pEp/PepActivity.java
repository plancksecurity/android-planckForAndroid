package com.fsck.k9.pEp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;

import org.pEp.jniadapter.Rating;

public abstract class PepActivity extends K9Activity {
    public static final String CURRENT_RATING = "current_color";
    public static final String PEP_COLOR_RATING_DETAIL_MESSAGE = "Cannot retrieve pEpRating";
    protected Rating pEpRating = Rating.pEpRatingUndefined;
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
    }

    @Override
    public void search(String query) {
        /* no-op */
    }

    public void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            setTitle(title);
        }
    }

    protected void loadPepRating() {
        final Intent intent = getIntent();
        String ratingString;
        if (intent.hasExtra(CURRENT_RATING)) {
            ratingString = intent.getStringExtra(CURRENT_RATING);
            Log.d(K9.LOG_TAG, "Got color:" + ratingString);
            pEpRating = PEpUtils.stringToRating(ratingString);
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

    private ApplicationComponent getApplicationComponent() {
        return getAndroidApplication().getComponent();
    }

    public K9 getAndroidApplication() {
        return (K9) getApplication();
    }

    protected abstract void initializeInjector(ApplicationComponent applicationComponent);
}
