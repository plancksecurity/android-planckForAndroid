package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepPermissionActivity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PEpPermissionView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PermissionsActivity extends PepPermissionActivity {

    @Bind(R.id.permission_contacts) PEpPermissionView contactsPermissionView;
    @Bind(R.id.permission_storage) PEpPermissionView storagePermissionView;
    @Bind(R.id.permission_battery) PEpPermissionView batteryPermissionView;
    private boolean askBatteryPermissionShowed;

    public static void actionAskPermissions(Context context) {
        Intent i = new Intent(context, PermissionsActivity.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        ButterKnife.bind(this);
        contactsPermissionView.initialize(getResources().getString(R.string.read_permission_rationale_title),
                getResources().getString(R.string.read_permission_first_explanation)
        );
        storagePermissionView.initialize(getResources().getString(R.string.download_permission_rationale_title),
                getResources().getString(R.string.download_snackbar_permission_rationale)
        );
        batteryPermissionView.initialize(getResources().getString(R.string.battery_optimization_rationale_title),
                getResources().getString(R.string.battery_optimiazation_explanation)
        );
    }

    @Override
    protected void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getLoaderManager(), getFragmentManager()))
                .build()
                .inject(this);
    }

    @Override
    public void showPermissionGranted(String permissionName) {
    }

    @Override
    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
    }

    @OnClick(R.id.action_continue)
    public void onContinueClicked() {
        createBasicPermissionsActivity(new PEpProvider.CompletedCallback() {
            @Override
            public void onComplete() {
                if(!askBatteryPermissionShowed) {
                    PEpUtils.askForBatteryOptimizationWhiteListing(PermissionsActivity.this);
                    askBatteryPermissionShowed = !askBatteryPermissionShowed;
                } else {
                    AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
                    finish();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if(!askBatteryPermissionShowed) {
                    PEpUtils.askForBatteryOptimizationWhiteListing(PermissionsActivity.this);
                    askBatteryPermissionShowed = !askBatteryPermissionShowed;
                } else {
                    AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
                    finish();
                }
            }
        });
    }
}
