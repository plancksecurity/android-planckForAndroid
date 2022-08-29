package com.fsck.k9.mail

/**
 * TransitoryFileBody
 *
 * Represents a body containing temporary files that eventually need to be deleted.
 */
interface TransitoryFileBody {
    fun getTransitoryFilePaths(): List<String>
}
