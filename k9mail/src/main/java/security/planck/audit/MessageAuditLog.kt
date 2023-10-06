package security.planck.audit

import com.fsck.k9.planck.infrastructure.NEW_LINE

internal data class MessageAuditLog(
    val timeStamp: Long,
    val senderId: String,
    val securityRating: String = ""
) {
    fun toCsv(): String =
        "$NEW_LINE${serialize()}"

    fun isStopEvent(): Boolean = senderId == PlanckAuditLogger.STOP_EVENT

    fun serialize(): String =
        "$timeStamp${PlanckAuditLogger.SEPARATOR}$senderId${PlanckAuditLogger.SEPARATOR}$securityRating"

    companion object {
        /**
         * Empty rating field is allowed as correct format.
         */
        fun deserialize(serialized: String): MessageAuditLog {
            try {
                val parts = serialized.split(PlanckAuditLogger.SEPARATOR)
                if (parts.size != 3) error("3 parts expected for log: $serialized")
                return MessageAuditLog(parts[0].toLong(), parts[1], parts[2])
            } catch (e: Exception) {
                throw LogBadlyFormattedException(serialized, e)
            }
        }
    }
}