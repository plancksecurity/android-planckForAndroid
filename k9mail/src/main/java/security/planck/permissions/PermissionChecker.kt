package security.planck.permissions


interface PermissionChecker {

    fun hasBasicPermission(): Boolean

    fun hasWriteExternalPermission(): Boolean
    fun doesntHaveWriteExternalPermission(): Boolean

    fun doesntHaveContactsPermission(): Boolean
    fun hasContactsPermission(): Boolean

    fun doesntHavePostNotificationsPermission(): Boolean
}