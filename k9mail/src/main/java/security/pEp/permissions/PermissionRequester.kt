package security.pEp.permissions

import android.view.View
import com.karumi.dexter.listener.single.PermissionListener


interface PermissionRequester {

    fun requestStoragePermission(view: View, listener: PermissionListener)

    fun requestContactsPermission(view: View, listener: PermissionListener)

    fun requestBatteryOptimizationPermission()

    fun requestAllPermissions(listener: PermissionListener)

}