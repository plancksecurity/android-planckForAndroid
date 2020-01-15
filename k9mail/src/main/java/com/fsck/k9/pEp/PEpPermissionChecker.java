package com.fsck.k9.pEp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import security.pEp.permissions.PermissionChecker;

public class PEpPermissionChecker implements PermissionChecker {

    private final Context context;

    public PEpPermissionChecker(Context context) {
        this.context = context;
    }

    public static Boolean hasWriteExternalPermission(Context context) {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static Boolean hasWriteContactsPermission(Context context) {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public boolean hasBasicPermission() {
        return hasContactsPermission() && hasWriteExternalPermission();
    }

    @Override
    public boolean hasWriteExternalPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public boolean doesntHaveWriteExternalPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_DENIED);
    }

    @Override
    public boolean doesntHaveContactsPermission() {
        int readRes = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        int writeRes = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS);
        return (readRes == PackageManager.PERMISSION_DENIED &&
                writeRes == PackageManager.PERMISSION_DENIED
        );
    }

    @Override
    public boolean hasContactsPermission() {
        int readRes = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        int writeRes = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS);
        return (readRes == PackageManager.PERMISSION_GRANTED &&
                writeRes == PackageManager.PERMISSION_GRANTED
        );
    }


}
