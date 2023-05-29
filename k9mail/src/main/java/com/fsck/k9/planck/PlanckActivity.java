package com.fsck.k9.planck;

import android.os.Bundle;

import androidx.loader.app.LoaderManager;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.planck.infrastructure.components.ApplicationComponent;
import com.fsck.k9.planck.infrastructure.components.PlanckComponent;

public abstract class PlanckActivity extends K9Activity {
    private PlanckProvider planck;
    private PlanckComponent planckComponent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
        inject();
    }

    @Override
    public void search(String query) {
        /* no-op */
    }

    protected void initPep() {
        planck = ((K9) getApplication()).getPlanckProvider();
    }

    public PlanckProvider getPlanck() {
        return planck;
    }

    private ApplicationComponent getApplicationComponent() {
        return getK9().getComponent();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        planckComponent = getK9().getComponent().planckComponentFactory()
                .create(this, this, LoaderManager.getInstance(this), getSupportFragmentManager());
    }

    public abstract void inject();

    public PlanckComponent getPlanckComponent() {
        return planckComponent;
    }
}
