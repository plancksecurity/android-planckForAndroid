package com.fsck.k9.pEp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PermissionErrorListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

import java.util.List;

public abstract class PepPermissionActivity extends K9Activity {
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
                .pEpModule(new PEpModule(this, getSupportLoaderManager(), getSupportFragmentManager()))
                .build();
    }

    public abstract void showPermissionGranted(String permissionName);

    public abstract void showPermissionDenied(String permissionName, boolean permanentlyDenied);

    public void createStoragePermissionListeners() {
        String explanation = getResources().getString(R.string.download_permission_first_explanation);
        Dexter.withActivity(PepPermissionActivity.this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(SnackbarOnDeniedPermissionListener.Builder.with(getRootView(), explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .build())
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }

    public void createContactsPermissionListeners() {
        String explanation = getResources().getString(R.string.read_permission_first_explanation);
        Dexter.withActivity(PepPermissionActivity.this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS)
                .withListener(SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(getRootView(), explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .build())
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }

    public void createBasicPermissionsActivity(PEpProvider.CompletedCallback completedCallback) {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                completedCallback.onComplete();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                completedCallback.onError(new Throwable(token.toString()));
            }
        }).check();
    }

    public abstract void inject();

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }

    public void askForBatteryOptimizationWhiteListing() {
        K9 k9 = getK9();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !k9.isBatteryOptimizationAsked()) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) k9.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            } else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
            startActivity(intent);
            k9.batteryOptimizationAsked();
        }
    }
}
