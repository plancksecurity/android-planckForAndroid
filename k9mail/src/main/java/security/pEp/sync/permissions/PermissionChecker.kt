package security.pEp.sync.permissions

interface PermissionChecker {
    fun hasBasicPermission(): Boolean
    fun hasWriteExternalPermission(): Boolean
    fun hasWriteContactsPermission(): Boolean
    fun hasReadContactsPermission(): Boolean
    fun doesntHaveWriteExternalPermission(): Boolean
    fun doesntHaveReadContactsPermission(): Boolean
}