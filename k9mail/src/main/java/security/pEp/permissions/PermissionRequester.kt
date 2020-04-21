package security.pEp.permissions

import android.view.View
import com.karumi.dexter.listener.single.PermissionListener


interface PermissionRequester {

    fun requestStoragePermission(view: View, listener: PermissionListener)

    fun requestStoragePermission(view: View)

    fun requestContactsPermission(view: View, listener: PermissionListener)

    fun requestContactsPermission(view: View)

    fun requestBatteryOptimizationPermission()

    fun requestAllPermissions(listener: PermissionListener)

    fun showRationaleSnackBar(view: View, explanation: String)

}