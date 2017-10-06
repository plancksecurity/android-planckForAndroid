package com.fsck.k9.pEp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PermissionErrorListener;
import com.fsck.k9.pEp.ui.listeners.ActivityPermissionListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

import java.util.List;

public abstract class PepPermissionActivity extends K9Activity {
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;
    private CompositePermissionListener storagePermissionListener;
    private CompositePermissionListener contactPermissionListener;
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

    public void createBasicPermissionsActivity(PEpProvider.CompletedCallback completedCallback) {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
                completedCallback.onComplete();
            }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                completedCallback.onError(new Throwable(token.toString()));
            }
        }).check();
    }

    public abstract void inject();

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }

    public void askForBatteryOptimizationWhiteListing() {
        K9 k9 = (K9) getApplication();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !k9.isBatteryOptimizationAsked()) {
            //TODO Probably is a good idea to first explain to the user why we need this
            //and  if the user rejects it, give the option to don't ask again and again
            Intent intent = new Intent();
            String packageName = k9.getPackageName();
            PowerManager pm = (PowerManager) k9.getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
            k9.startActivity(intent);
            k9.batteryOptimizationAsked();
        }
    }
}
