package com.fsck.k9.planck.ui;

import android.os.Bundle;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.infrastructure.components.ApplicationComponent;
import com.fsck.k9.planck.infrastructure.components.DaggerPlanckComponent;
import com.fsck.k9.planck.infrastructure.components.PlanckComponent;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.planck.infrastructure.modules.PlanckModule;

public abstract class PlanckColoredActivity extends K9Activity {
    public static final String CURRENT_RATING = "current_color";
    PlanckUIArtefactCache uiCache;
    private PlanckProvider planck;
    private PlanckComponent planckComponent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
        inject();
    }

    protected abstract void inject();

    @Override
    public void search(String query) {

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
