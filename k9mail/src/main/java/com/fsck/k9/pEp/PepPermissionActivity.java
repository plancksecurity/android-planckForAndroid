package com.fsck.k9.pEp;

import android.os.Bundle;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;

public abstract class PepPermissionActivity extends K9Activity {
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

    protected abstract void initializeInjector(ApplicationComponent applicationComponent);

    public abstract void showPermissionGranted(String permissionName);

    public abstract void showPermissionDenied(String permissionName, boolean permanentlyDenied);
}
