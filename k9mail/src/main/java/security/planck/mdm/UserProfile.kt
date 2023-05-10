package security.planck.mdm

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.getSystemService

class UserProfile {
    fun isRunningOnWorkProfile(context: Context): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            return false
        }
        return getSystemService(context, DevicePolicyManager::class.java)?.let { devicePolicyManager ->
            devicePolicyManager.activeAdmins?.any {
                devicePolicyManager.isProfileOwnerApp(it.packageName)
            }
        } ?: false
    }
}