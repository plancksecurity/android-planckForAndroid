package com.fsck.k9.pEp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

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

    public void simplePermissionsCheck() {
        if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_CONTACTS
                    ).withListener(new MultiplePermissionsListener() {
                @Override public void onPermissionsChecked(MultiplePermissionsReport report) {

                }
                @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
        }
    }

    private boolean isPermissionGranted(String readContacts) {
        return ContextCompat.checkSelfPermission(this, readContacts) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        simplePermissionsCheck();
    }
}
