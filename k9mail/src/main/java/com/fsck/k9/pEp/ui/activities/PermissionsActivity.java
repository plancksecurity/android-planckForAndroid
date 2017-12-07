package com.fsck.k9.pEp.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PepPermissionActivity;
import com.fsck.k9.pEp.ui.PEpPermissionView;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PermissionsActivity extends PepPermissionActivity {

    public static final int SETTINGS_CODE = 777;
    @Bind(R.id.permission_contacts) PEpPermissionView contactsPermissionView;
    @Bind(R.id.permission_storage) PEpPermissionView storagePermissionView;
    @Bind(R.id.permission_battery) PEpPermissionView batteryPermissionView;

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
    protected void onResume() {
        super.onResume();
        askForBatteryOptimizationWhiteListing();
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


    @RequiresApi(api = Build.VERSION_CODES.M)
    @OnClick(R.id.action_continue)
    public void onContinueClicked() {
        handlePermissionsLogic();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void handlePermissionsLogic() {
        if(isPermissionGranted(Manifest.permission.READ_CONTACTS)
                && isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            goToSetupAccount();
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                    || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showRationalePermissionsDialog();
            } else {
                createBasicPermissionsActivity(permissionsCompletedCallback());
            }
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
                    startActivityForResult(intent, SETTINGS_CODE);
                })
                .setNegativeButton(R.string.cancel_action, (dialog, which) -> goToSetupAccount())
                .show();
    }

    private void showRationalePermissionsDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissions_needed_message)
                .setPositiveButton(R.string.okay_action, (dialog, which) -> {
                    createBasicPermissionsActivity(permissionsCompletedCallback());
                })
                .setNegativeButton(R.string.cancel_action, (dialog, which) -> goToSetupAccount())
                .show();
    }

    @NonNull
    private MultiplePermissionsListener permissionsCompletedCallback() {
        return new MultiplePermissionsListener() {

            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(isPermissionGranted(Manifest.permission.READ_CONTACTS)
                        && isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    goToSetupAccount();
                } else {
                    showNeedPermissionsDialog();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        };
    }

    private void goToSetupAccount() {
        AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
        finish();

    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onContinueClicked();
    }

    private boolean isPermissionGranted(String readContacts) {
        return ContextCompat.checkSelfPermission(this, readContacts) == PackageManager.PERMISSION_GRANTED;
    }
}
