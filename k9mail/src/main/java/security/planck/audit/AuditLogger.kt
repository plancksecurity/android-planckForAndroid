package security.planck.audit

import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.Calendar

class AuditLogger(
    private val planckProvider: PlanckProvider,
    private val auditLoggerFile: File,
    var logAgeLimit: Long,
) {
    private val currentTimeInSeconds: Long
        get() = Calendar.getInstance().timeInMillis / 1000

    private val tamperAlertMF: MutableStateFlow<Int> = MutableStateFlow(0)
    val tamperAlertFlow: StateFlow<Int> = tamperAlertMF.asStateFlow()

    init {
        auditLoggerFile.parentFile?.mkdirs()
    }

    private data class MessageAuditLog(
        val timeStamp: Long,
        val senderId: String,
        val securityRating: String = ""
    ) {
        fun toCsv(): String =
            "$NEW_LINE${serialize()}"

        fun isStopEvent(): Boolean = senderId == STOP_EVENT

        fun serialize(): String = "$timeStamp$SEPARATOR$senderId$SEPARATOR$securityRating"

        companion object {
            fun deserialize(serialized: String): MessageAuditLog {
                val parts = serialized.split(SEPARATOR)
                return MessageAuditLog(parts[0].toLong(), parts[1], parts[2])
            }
        }
    }

    private fun MessageAuditLog.isOutdated(newTime: Long): Boolean =
        newTime - timeStamp > logAgeLimit

    fun addStartEventLog() {
        addSpecialEventLog(START_EVENT, currentTimeInSeconds)
    }

    fun addStopEventLog(stopTime: Long) {
        if (currentTimeInSeconds - stopTime < logAgeLimit) {
            addSpecialEventLog(STOP_EVENT, stopTime)
        }
    }

    private fun addSpecialEventLog(eventName: String, eventTime: Long) {
        addMessageAuditLog(
            MessageAuditLog(
                eventTime,
                eventName
            )
        )
    }

    @Synchronized
    fun addMessageAuditLog(message: MimeMessage, rating: Rating) {
        addMessageAuditLog(
            MessageAuditLog(
                currentTimeInSeconds,
                message.from.firstOrNull()?.address.toString(),
                PlanckUtils.ratingToString(rating)
            )
        )
    }

    private fun addMessageAuditLog(messageAuditLog: MessageAuditLog) {
        try {
            val allFileText = if (auditLoggerFile.exists()) auditLoggerFile.readText() else ""

            when {
                allFileText.isBlank() -> {
                    addHeader()
                    appendLog(messageAuditLog)
                }

                allFileText == HEADER -> {
                    appendLog(messageAuditLog)
                }

                else -> {
                    writeLogRemovingOldLogs(allFileText, messageAuditLog)
                }
            }
        } catch (e: IOException) {
            if (K9.isDebug()) {
                Log.e(CONSOLE_LOG_TAG, "Error adding audit log", e)
            }
        }
    }

    private fun appendLog(log: MessageAuditLog) {
        auditLoggerFile.appendText(log.toCsv())
    }

    private fun writeLogRemovingOldLogs(
        allFileText: String,
        messageAuditLog: MessageAuditLog,
    ) {
        val newTime = messageAuditLog.timeStamp
        val cleanText = allFileText.removeOldLogs(newTime)
        val log = messageAuditLog.updateStopTimeToLateLogTime(cleanText, newTime)
        auditLoggerFile.writeText(cleanText + log.toCsv())
    }

    private fun MessageAuditLog.updateStopTimeToLateLogTime(
        cleanText: String,
        newTime: Long,
    ): MessageAuditLog {
        var log = this
        if (log.isStopEvent()) {
            val lastMessageTime = kotlin.runCatching {
                MessageAuditLog.deserialize(cleanText.substringAfterLast(NEW_LINE)).timeStamp
            }.getOrDefault(0)
            if (lastMessageTime > newTime) {
                log = log.copy(timeStamp = lastMessageTime)
            }
        }
        return log
    }

    private fun String.removeOldLogs(newTime: Long): String {
        val textWithoutHeader = substringAfter("$HEADER$NEW_LINE")
            .split(NEW_LINE)
            .filterNot { logLine ->
                kotlin.runCatching { MessageAuditLog.deserialize(logLine).isOutdated(newTime) }
                    .getOrDefault(true)
            }.joinToString(NEW_LINE)
        return "$HEADER$NEW_LINE" + textWithoutHeader
    }

    private fun addHeader() {
        auditLoggerFile.writeText(HEADER)
    }

    private fun addSignature() {
        val signatureLog = MessageAuditLog(currentTimeInSeconds, SIGNATURE_ID, "").toCsv()
        val textToSign = auditLoggerFile.readText() + signatureLog
        planckProvider.getSignatureForText(textToSign)
            .onSuccess { signature -> // only add the whole signature log line on success
                auditLoggerFile.appendText(signatureLog)
                auditLoggerFile.appendText(signature)
            }.onFailure {
                // same as tamper detected on failure
                tamperAlertMF.value = tamperAlertMF.value + 1
            }
    }

    private fun verifyAuditText(auditText: String, signature: String) {
        if (signature.isBlank()
            || !planckProvider.verifySignature(auditText, signature)
                .getOrDefault(false) // same as tamper detected on failure
        ) {
            // tamper detected
            tamperAlertMF.value = tamperAlertMF.value + 1
        }
    }

    companion object {
        const val AUDIT_LOGGER_ROUTE = "audit/messageAudit.csv"
        const val START_EVENT = "AUDIT LOGGING START"
        const val STOP_EVENT = "AUDIT LOGGING STOP"
        const val SIGNATURE_ID = "SIGNATURE"
        private const val SEPARATOR = ";"
        internal const val HEADER =
            "TIMESTAMP${SEPARATOR}SENDER-ID${SEPARATOR}SECURITY-RATING"
        private const val CONSOLE_LOG_TAG = "AuditLogger"
    }
}