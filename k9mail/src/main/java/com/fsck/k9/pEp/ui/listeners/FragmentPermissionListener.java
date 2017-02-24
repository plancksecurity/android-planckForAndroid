package com.fsck.k9.pEp.ui.listeners;

import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class FragmentPermissionListener implements PermissionListener {

    private final MessageViewFragment fragment;

    public FragmentPermissionListener(MessageViewFragment fragment) {
        this.fragment = fragment;
    }

    @Override public void onPermissionGranted(PermissionGrantedResponse response) {
        fragment.showPermissionGranted(response.getPermissionName());
    }

    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
        fragment.showPermissionDenied(response.getPermissionName(), response.isPermanentlyDenied());
    }

    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                                                             PermissionToken token) {
        fragment.showPermissionRationale(token);
    }
}
