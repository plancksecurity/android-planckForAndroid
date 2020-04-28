package security.pEp.ui.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.PermissionErrorListener;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import security.pEp.permissions.PermissionRequester;

public class PepPermissionRequester implements PermissionRequester {

    private final K9 app;
    private final Activity activity;

    public PepPermissionRequester(Activity activity) {
        this.activity = activity;
        this.app = ((K9) activity.getApplicationContext());
    }

    @Override
    public void requestStoragePermission(@NotNull View view, PermissionListener listener) {
        String explanation = activity.getString(R.string.download_permission_first_explanation);
        startDexterActivity(
                view,
                explanation,
                listener,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }


    @Override
    public void requestContactsPermission(@NotNull View view, PermissionListener listener) {
        String explanation = activity.getString(R.string.read_permission_first_explanation);
        startDexterActivity(
                view,
                explanation,
                listener,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS);
    }

    @Override
    public void requestStoragePermission(@NotNull View view) {
        requestStoragePermission(view, null);
    }

    @Override
    public void requestContactsPermission(@NotNull View view) {
        requestContactsPermission(view, null);
    }

    private void startDexterActivity(@NotNull View view, @NotNull String explanation, PermissionListener listener, String... permissions) {
        MultiplePermissionsListener compositePermissionsListener =
                getMultiplePermissionListener(view, listener, explanation);

        for (String permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                showRationaleSnackBar(view, explanation);
                return;
            }
        }

        Dexter.withActivity(activity)
                .withPermissions(permissions)
                .withListener(compositePermissionsListener)
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }

    private MultiplePermissionsListener getMultiplePermissionListener(View view, PermissionListener listener, String explanation) {
        MultiplePermissionsListener snackBarMultiplePermissionsListener =
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(view, explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .build();

        MultiplePermissionsListener dialogMultiplePermissionsListener =
                new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if(listener != null) {
                            for (PermissionGrantedResponse response : report.getGrantedPermissionResponses()) {
                                listener.onPermissionGranted(response);
                            }
                            for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
                                listener.onPermissionDenied(response);
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        if (listener != null &&
                                permissions != null && permissions.size() > 0) {
                            listener.onPermissionRationaleShouldBeShown(permissions.get(0), token);
                        }
                    }
                };
        return new CompositeMultiplePermissionsListener(dialogMultiplePermissionsListener, snackBarMultiplePermissionsListener);
    }

    @SuppressLint("BatteryLife")
    @Override
    public void requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !app.isBatteryOptimizationAsked()) {
            Intent intent = new Intent();
            String packageName = activity.getPackageName();
            PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            } else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
            activity.startActivity(intent);
            app.batteryOptimizationAsked();
        }
    }

    @Override
    public void requestAllPermissions(@NotNull PermissionListener listener) {
        Dexter.withActivity(activity)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(
                        new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report != null && report.getGrantedPermissionResponses().size() > 0)
                                    listener.onPermissionGranted(report.getGrantedPermissionResponses().get(0));
                                else if (report != null && report.getDeniedPermissionResponses().size() > 0)
                                    listener.onPermissionDenied(report.getDeniedPermissionResponses().get(0));
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                if (permissions != null && permissions.size() > 0)
                                    listener.onPermissionRationaleShouldBeShown(permissions.get(0), token);
                            }
                        })
                .withErrorListener(new PermissionErrorListener())
                .check();
    }

    @Override
    public void showRationaleSnackBar(@NotNull View view, String explanation) {
        FeedbackTools.showLongFeedback(view, explanation, activity.getString(R.string.button_settings), v -> {
            Context context = v.getContext();
            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.getPackageName()));
            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
            myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myAppSettings);
        });
    }
}
