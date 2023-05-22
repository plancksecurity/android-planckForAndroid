package com.fsck.k9.planck;

import android.os.Bundle;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.planck.infrastructure.components.ApplicationComponent;
import com.fsck.k9.planck.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.planck.infrastructure.components.PEpComponent;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.planck.infrastructure.modules.PEpModule;

public abstract class PepActivity extends K9Activity {
    private PEpProvider pEp;
    private PEpComponent pEpComponent;

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
        pEp = ((K9) getApplication()).getpEpProvider();
    }

    public PEpProvider getpEp() {
        return pEp;
    }

    private ApplicationComponent getApplicationComponent() {
        return getK9().getComponent();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        pEpComponent = DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getSupportLoaderManager(), getSupportFragmentManager()))
                .build();
    }

    public abstract void inject();

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }
}
