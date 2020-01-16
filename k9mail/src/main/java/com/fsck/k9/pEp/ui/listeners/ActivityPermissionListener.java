package com.fsck.k9.pEp.ui.listeners;

import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import security.pEp.ui.permissions.PepPermissionActivity;

public class ActivityPermissionListener implements PermissionListener {

    private final PepPermissionActivity activity;

    public ActivityPermissionListener(PepPermissionActivity activity) {
        this.activity = activity;
    }

    @Override public void onPermissionGranted(PermissionGrantedResponse response) {
        activity.showPermissionGranted(response.getPermissionName());
    }

    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
        activity.showPermissionDenied(response.getPermissionName(), response.isPermanentlyDenied());
    }

    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                                                             PermissionToken token) {
    }
}
