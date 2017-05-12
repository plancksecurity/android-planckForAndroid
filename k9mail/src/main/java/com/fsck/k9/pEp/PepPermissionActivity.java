package com.fsck.k9.pEp;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.ui.PermissionErrorListener;
import com.fsck.k9.pEp.ui.listeners.ActivityPermissionListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

public abstract class PepPermissionActivity extends K9Activity {
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;
    private CompositePermissionListener storagePermissionListener;
    private CompositePermissionListener contactPermissionListener;

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

    public void createStoragePermissionListeners() {
        ActivityPermissionListener feedbackViewPermissionListener = new ActivityPermissionListener(PepPermissionActivity.this);

        String explanation = getResources().getString(R.string.download_permission_first_explanation);
        storagePermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
                SnackbarOnDeniedPermissionListener.Builder.with(getRootView(), explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .build());
        Dexter.withActivity(PepPermissionActivity.this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(storagePermissionListener)
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }

    public void createContactsPermissionListeners() {
        PermissionListener feedbackViewPermissionListener = new ActivityPermissionListener(this);

        String explanation = getResources().getString(R.string.read_permission_first_explanation);

        contactPermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
                SnackbarOnDeniedPermissionListener.Builder.with(getRootView(),
                        explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .withCallback(new Snackbar.Callback() {
                            @Override public void onShown(Snackbar snackbar) {
                                super.onShown(snackbar);
                            }

                            @Override public void onDismissed(Snackbar snackbar, int event) {
                                super.onDismissed(snackbar, event);
                            }
                        })
                        .build());
        Dexter.withActivity(PepPermissionActivity.this)
                .withPermission(Manifest.permission.WRITE_CONTACTS)
                .withListener(contactPermissionListener)
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }
}
