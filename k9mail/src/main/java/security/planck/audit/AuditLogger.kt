package security.planck.audit

import com.fsck.k9.mail.internet.MimeMessage
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.StateFlow

/**
 * AuditLogger
 *
 * Logs message encryption and decryption events to a text file and keeps the logs for [logAgeLimit].
 */
interface AuditLogger {
    /**
     * logAgeLimit
     *
     * Maximum time for the logs to expire.
     * Expired logs are removed from the top of the file when adding new ones at the bottom.
     */
    var logAgeLimit: Long

    /**
     * tamperAlertFlow
     *
     * [StateFlow] that can be collected to receive updates on audit log issues.
     */
    val tamperAlertFlow: StateFlow<Boolean>

    /**
     * addStartEventLog
     *
     * Log the fact that logging has started, this should be done when application starts.
     */
    fun addStartEventLog()

    /**
     * addStopEventLog
     *
     * Log the fact that logging has stopped, this should be done when application finishes or is killed.
     *
     * @param stopTime Timestamp when the logging stopped.
     */
    fun addStopEventLog(stopTime: Long)

    /**
     * addMessageAuditLog
     *
     * Log a message encryption or decryption event.
     *
     * @param message [MimeMessage] that was encrypted or decrypted.
     * @param rating [Rating] of the encrypted or decrypted message.
     */
    fun addMessageAuditLog(message: MimeMessage, rating: Rating)

    /**
     * resetTamperAlert
     *
     * Reset the [tamperAlertFlow] to false and unset in disk the last tamper alert time.
     */
    suspend fun resetTamperAlert()

    /**
     * enablePersistentWarningOnStartup
     *
     * Set a flag in disk that will trigger the [tamperAlertFlow] on every startup.
     */
    suspend fun enablePersistentWarningOnStartup()

    /**
     * disablePersistentWarningOnStartup
     *
     * Unset a flag in disk that will trigger the [tamperAlertFlow] on every startup.
     */
    suspend fun disablePersistentWarningOnStartup()

    /**
     * checkPendingTamperingWarningFromBackground
     *
     * Checks for pending tamper alert saved in disk and triggers [tamperAlertFlow] if there was any.
     */
    fun checkPendingTamperingWarningFromBackground()
}