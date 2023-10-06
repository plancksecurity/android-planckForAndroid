package security.planck.audit

import com.fsck.k9.mail.internet.MimeMessage
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.StateFlow

interface AuditLogger {
    var logAgeLimit: Long
    val tamperAlertFlow: StateFlow<Boolean>
    fun addStartEventLog()
    fun addStopEventLog(stopTime: Long)
    fun addMessageAuditLog(message: MimeMessage, rating: Rating)
    fun resetTamperAlert()
    fun enablePersistentWarningOnStartup()
    fun disablePersistentWarningOnStartup()
}