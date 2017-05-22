package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PEpPermissionChecker;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PepPermissionActivity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PEpPermissionView;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PermissionsActivity extends PepPermissionActivity {

    @Bind(R.id.permission_contacts) PEpPermissionView contactsPermissionView;
    @Bind(R.id.permission_storage) PEpPermissionView storagePermissionView;
    @Bind(R.id.action_continue) Button continueButton;

    public static void actionAskPermissions(Context context) {
        Intent i = new Intent(context, PermissionsActivity.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        ButterKnife.bind(this);
        updateViews();
        contactsPermissionView.initialize(getResources().getString(R.string.read_permission_rationale_title),
                getResources().getString(R.string.read_permission_first_explanation),
                (buttonView, isChecked) -> {
                    createContactsPermissionListeners();
                    updateViews();
                });
        storagePermissionView.initialize(getResources().getString(R.string.download_permission_rationale_title),
                getResources().getString(R.string.download_snackbar_permission_rationale),
                (buttonView, isChecked) -> {
                    createStoragePermissionListeners();
                    updateViews();
                });
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
        updateViews();
    }

    @Override
    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
        updateViews();
    }

    private void updateViews() {
        contactsPermissionView.setChecked(PEpPermissionChecker.hasContactsPermission(this));
        storagePermissionView.setChecked(PEpPermissionChecker.hasWriteExternalPermission(this));
        contactsPermissionView.enable(!PEpPermissionChecker.hasContactsPermission(this));
        storagePermissionView.enable(!PEpPermissionChecker.hasWriteExternalPermission(this));
    }

    @OnClick(R.id.action_cancel)
    public void onCancelClicked() {
        AccountSetupBasics.actionNewAccount(this);
        finish();
    }

    @OnClick(R.id.action_continue)
    public void onContinueClicked() {
        createBasicPermissionsActivity(new PEpProvider.CompletedCallback() {
            @Override
            public void onComplete() {
                updateViews();
                AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
                finish();
            }

            @Override
            public void onError(Throwable throwable) {
                updateViews();
            }
        });
    }
}
