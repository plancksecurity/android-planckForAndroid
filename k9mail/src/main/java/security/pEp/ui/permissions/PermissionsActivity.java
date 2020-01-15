package security.pEp.ui.permissions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.PEpPermissionView;
import com.fsck.k9.preferences.StorageEditor;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import security.pEp.permissions.PermissionRequester;

public class PermissionsActivity extends PepActivity {

    @Bind(R.id.permission_contacts)
    PEpPermissionView contactsPermissionView;
    @Bind(R.id.permission_storage)
    PEpPermissionView storagePermissionView;
    @Bind(R.id.permission_battery)
    PEpPermissionView batteryPermissionView;

    @Inject
    PermissionRequester permissionRequester;

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
    public void inject() {
        getpEpComponent().inject(this);
    }

    @OnClick(R.id.action_continue)
    public void onContinueClicked() {
        if (K9.isShallRequestPermissions()) {
            disableRequestPermissions();
            permissionRequester.requestAllPermissions(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {
                    goToSetupAccount();
                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse response) {
                    goToSetupAccount();
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    goToSetupAccount();
                }
            });
        } else {
            goToSetupAccount();
        }
    }

    private void goToSetupAccount() {
        AccountSetupBasics.actionNewAccount(PermissionsActivity.this);
        finish();
    }

    private void disableRequestPermissions() {
        K9.setShallRequestPermissions(false);

        new Thread(() -> {
            Preferences prefs = Preferences.getPreferences(getApplicationContext());
            StorageEditor editor = prefs.getStorage().edit();
            K9.save(editor);
            editor.commit();
        }).start();
    }
}
