package security.planck.permissions


interface PermissionChecker {

    fun hasBasicPermission(): Boolean

    fun doesntHaveContactsPermission(): Boolean
    fun hasContactsPermission(): Boolean

    fun doesntHavePostNotificationsPermission(): Boolean
}