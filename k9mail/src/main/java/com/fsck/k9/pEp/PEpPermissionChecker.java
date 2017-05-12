package com.fsck.k9.pEp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

public class PEpPermissionChecker {

    public static Boolean hasBasicPermission(Context context) {
        return hasContactsPermission(context) && hasWriteExternalPermission(context);
    }

    public static Boolean hasWriteExternalPermission(Context context) {
        int res = context.getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static Boolean hasContactsPermission(Context context) {
        int res = context.getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_CONTACTS);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
}
