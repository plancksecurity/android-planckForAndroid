package security.pEp.ui.permissions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.ui.PEpPermissionView;
import com.fsck.k9.preferences.StorageEditor;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PermissionsActivity extends PepPermissionActivity {

    @Bind(R.id.permission_contacts)
    PEpPermissionView contactsPermissionView;
    @Bind(R.id.permission_storage)
    PEpPermissionView storagePermissionView;
    @Bind(R.id.permission_battery)
    PEpPermissionView batteryPermissionView;


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
        if (K9.isShallRequestPermissions()) {
            disableRequestPermissions();
            createBasicPermissionsActivity(permissionsCompletedCallback());
        } else {
            goToSetupAccount();
        }
    }

    @NonNull
    private PEpProvider.CompletedCallback permissionsCompletedCallback() {
        return new PEpProvider.CompletedCallback() {
            @Override
            public void onComplete() {
                goToSetupAccount();
            }

            @Override
            public void onError(Throwable throwable) {
                goToSetupAccount();
            }
        };
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
