package security.planck.audit

import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckUtils
import foundation.pEp.jniadapter.Rating
import java.io.File
import java.io.IOException
import java.util.Calendar

class AuditLogger(
    private val auditLoggerFile: File,
    var logAgeLimit: Long,
) {
    private val currentTimeInSeconds: Long
        get() = Calendar.getInstance().timeInMillis / 1000

    init {
        auditLoggerFile.parentFile?.mkdirs()
    }

    private data class MessageAuditLog(
        val timeStamp: Long,
        val senderId: String,
        val securityRating: String = ""
    ) {
        fun toCsv(): String =
            "\n$timeStamp$SEPARATOR$senderId$SEPARATOR$securityRating"
    }

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
            val textToAppend = messageAuditLog.toCsv()

            when {
                allFileText.isBlank() -> {
                    addHeader()
                    auditLoggerFile.appendText(textToAppend)
                }

                else -> {
                    writeLogRemovingOldLogs(allFileText, textToAppend, messageAuditLog.timeStamp)
                }
            }
        } catch (e: IOException) {
            if (K9.isDebug()) {
                Log.e(CONSOLE_LOG_TAG, "Error adding audit log", e)
            }
        }
    }

    private fun writeLogRemovingOldLogs(allFileText: String, textToAppend: String, newTime: Long) {
        auditLoggerFile.writeText(allFileText.removeOldLogs(newTime) + textToAppend)
    }

    private fun String.removeOldLogs(newTime: Long): String {
        val textWithoutHeader = substringAfter("$HEADER\n", substringAfter(HEADER))
            .split("\n")
            .filter { logLine ->
                logLine.isNotBlank() && newTime - getLogTime(logLine) < logAgeLimit
            }.joinToString("\n")
        val header = if (textWithoutHeader.isBlank()) HEADER else "$HEADER\n"
        return header + textWithoutHeader
    }

    private fun getLogTime(logLine: String): Long {
        return logLine.substringBefore(SEPARATOR).toLong()
    }

    private fun addHeader() {
        auditLoggerFile.writeText(HEADER)
    }

    companion object {
        const val auditLoggerFileRoute = "audit/messageAudit.csv"
        const val START_EVENT = "AUDIT LOGGING START"
        const val STOP_EVENT = "AUDIT LOGGING STOP"
        private const val SEPARATOR = ";"
        internal const val HEADER =
            "TIMESTAMP${SEPARATOR}SENDER-ID${SEPARATOR}SECURITY-RATING"
        private const val CONSOLE_LOG_TAG = "AuditLogger"
    }
}