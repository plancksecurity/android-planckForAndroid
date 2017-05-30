package com.fsck.k9.pEp;

import android.os.Bundle;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

public abstract class PepActivity extends K9Activity {
    PePUIArtefactCache uiCache;
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
        return getK9().getComponent();
    }

    public K9 getK9() {
        return (K9) getApplication();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        pEpComponent = DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getLoaderManager(), getFragmentManager()))
                .build();
    }

    public abstract void inject();

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }
}
