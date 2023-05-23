package com.fsck.k9.planck.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.infrastructure.components.ApplicationComponent;
import com.fsck.k9.planck.infrastructure.components.DaggerPlanckComponent;
import com.fsck.k9.planck.infrastructure.components.PlanckComponent;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.planck.infrastructure.modules.PlanckModule;

import foundation.pEp.jniadapter.Rating;
import security.planck.ui.toolbar.PlanckToolbarCustomizer;
import security.planck.ui.toolbar.ToolBarCustomizer;

public abstract class PlanckColoredActivity extends K9Activity {
    public static final String CURRENT_RATING = "current_color";
    public static final String PLANCK_COLOR_RATING_DETAIL_MESSAGE = "Cannot retrieve pEpRating";
    protected Rating planckRating = Rating.pEpRatingUndefined;
    PlanckUIArtefactCache uiCache;
    private PlanckProvider planck;
    private PlanckComponent planckComponent;

    ToolBarCustomizer toolBarCustomizer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolBarCustomizer = new PlanckToolbarCustomizer(this);
        initializeInjector(getApplicationComponent());
        inject();
    }

    protected abstract void inject();

    @Override
    public void search(String query) {

    }

    protected void colorActionBar() {
        if (getToolbar() != null) {
            toolBarCustomizer.setToolbarColor(planckRating);
            toolBarCustomizer.setStatusBarPlanckColor(planckRating);
        }
    }

    protected void colorActionBar(Rating pEpRating) {
        toolBarCustomizer.setToolbarColor(pEpRating);
        toolBarCustomizer.setStatusBarPlanckColor(pEpRating);
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
            planckRating = PlanckUtils.stringToRating(ratingString);
        } else {
            throw new RuntimeException(PLANCK_COLOR_RATING_DETAIL_MESSAGE);
        }
    }

    public void setPlanckRating(Rating planckRating) {
        this.planckRating = planckRating;
    }

    public Rating getPlanckRating() {
        return planckRating;
    }

    protected void initPep() {
        uiCache = PlanckUIArtefactCache.getInstance(getApplicationContext());
        planck = ((K9) getApplication()).getPlanckProvider();
    }

    public PlanckProvider getPlanck() {
        return planck;
    }

    public PlanckUIArtefactCache getUiCache() {
        return uiCache;
    }

    private ApplicationComponent getApplicationComponent() {
        return getAndroidApplication().getComponent();
    }

    public K9 getAndroidApplication() {
        return (K9) getApplication();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        planckComponent = DaggerPlanckComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .planckModule(new PlanckModule(this, getSupportLoaderManager(), getSupportFragmentManager()))
                .build();
    }

    public PlanckComponent getPlanckComponent() {
        return planckComponent;
    }
}
