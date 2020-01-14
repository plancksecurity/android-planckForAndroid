package com.fsck.k9.pEp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import security.pEp.sync.permissions.PermissionChecker;

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

    public static Boolean hasReadContactsPermission(Context context) {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static Boolean doesntHaveWriteExternalPermission(Context context) {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_DENIED);
    }

    public static Boolean doesntHaveReadContactsPermission(Context context) {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        return (res == PackageManager.PERMISSION_DENIED);
    }

    @Override
    public boolean hasBasicPermission() {
        return hasWriteContactsPermission() && hasWriteExternalPermission();
    }

    @Override
    public boolean hasWriteExternalPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public boolean hasWriteContactsPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public boolean hasReadContactsPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public boolean doesntHaveWriteExternalPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_DENIED);
    }

    @Override
    public boolean doesntHaveReadContactsPermission() {
        int res = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        return (res == PackageManager.PERMISSION_DENIED);
    }



}
