package security.pEp.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat

import security.pEp.permissions.PermissionChecker

class PEpPermissionChecker(private val context: Context) : PermissionChecker {

    override fun hasBasicPermission(): Boolean {
        return hasContactsPermission() && hasWriteExternalPermission()
    }

    override fun hasWriteExternalPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return true
        val res = getExternalStoragePermission()
        return arePermissionsGranted(res)
    }

    override fun doesntHaveWriteExternalPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return false
        val res = getExternalStoragePermission()
        return arePermissionsDenied(res)
    }

    override fun doesntHaveContactsPermission(): Boolean {
        val readRes = getReadContactsPermission()
        val writeRes = getWriteContactsPermission()
        return arePermissionsDenied(readRes, writeRes)
    }

    override fun hasContactsPermission(): Boolean {
        val readRes = getReadContactsPermission()
        val writeRes = getWriteContactsPermission()
        return arePermissionsGranted(readRes, writeRes)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun doesntHavePostNotificationsPermission(): Boolean {
        val res = getPostNotificationsPermission()
        return arePermissionsDenied(res)
    }

    private fun arePermissionsGranted(vararg permissionsStatusResult: Int): Boolean {
        return permissionsStatusResult
                .map { permissionStatus -> permissionStatus == PackageManager.PERMISSION_GRANTED }
                .reduce { permissionsGranted, newValue -> permissionsGranted && newValue }
    }

    private fun arePermissionsDenied(vararg permissionsStatusResult: Int): Boolean {
        return permissionsStatusResult
                .map { permissionStatus -> permissionStatus == PackageManager.PERMISSION_DENIED }
                .reduce { permissionsDenied, newValue -> permissionsDenied && newValue }
    }

    private fun getReadContactsPermission(): Int {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
    }

    private fun getWriteContactsPermission(): Int {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
    }

    private fun getExternalStoragePermission(): Int {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPostNotificationsPermission(): Int {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    }
}
