package com.fsck.k9.pEp.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepPermissionActivity;
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
    public void showPermissionGranted(String permissionName) {
    }

    @Override
    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }


    @OnClick(R.id.action_continue)
    public void onContinueClicked() {
        if (noPermissionGrantedOrDenied()) {
            createBasicPermissionsActivity(permissionsCompletedCallback());
        } else if (!isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            showNeedPermissionsDialog();
        } else if(!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showNeedPermissionsDialog();
        } else {
            goToSetupAccount();
        }
    }

    private void showNeedPermissionsDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissions_needed_message)
                .setPositiveButton(R.string.okay_action, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToSetupAccount();
                    }
                })
                .show();
    }

    @NonNull
    private PEpProvider.CompletedCallback permissionsCompletedCallback() {
        return new PEpProvider.CompletedCallback() {
            @Override
            public void onComplete() {
                if(!askBatteryPermissionShowed) {
                    PEpUtils.askForBatteryOptimizationWhiteListing(getK9());
                    askBatteryPermissionShowed = !askBatteryPermissionShowed;
                } else {
                    goToSetupAccount();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if(!askBatteryPermissionShowed) {
                    PEpUtils.askForBatteryOptimizationWhiteListing(getK9());
                    askBatteryPermissionShowed = !askBatteryPermissionShowed;
                } else {
                    goToSetupAccount();
                }
            }
        };
    }

    private void goToSetupAccount() {
        AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
        finish();
    }

    private boolean noPermissionGrantedOrDenied() {
        return isPermissionDenied(Manifest.permission.READ_CONTACTS) &&
                isPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean isPermissionDenied(String readContacts) {
        return ContextCompat.checkSelfPermission(this, readContacts) == PackageManager.PERMISSION_DENIED;
    }

    private boolean isPermissionGranted(String readContacts) {
        return ContextCompat.checkSelfPermission(this, readContacts) == PackageManager.PERMISSION_GRANTED;
    }
}
