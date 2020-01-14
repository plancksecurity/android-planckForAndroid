package security.pEp.sync.permissions


interface PermissionChecker {

    fun hasBasicPermission(): Boolean

    fun hasWriteExternalPermission(): Boolean
    fun doesntHaveWriteExternalPermission(): Boolean

    fun hasWriteContactsPermission(): Boolean
    fun doesntHaveWriteContactsPermission(): Boolean

    fun hasReadContactsPermission(): Boolean
    fun doesntHaveReadContactsPermission(): Boolean

}