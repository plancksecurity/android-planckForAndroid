package security.pEp.mdm

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService

class UserProfile {
    fun isRunningOnWorkProfile(context: Context): Boolean {
        return getSystemService(context, DevicePolicyManager::class.java)?.let { devicePolicyManager ->
            devicePolicyManager.activeAdmins?.any {
                devicePolicyManager.isProfileOwnerApp(it.packageName)
            }
        } ?: false
    }
}