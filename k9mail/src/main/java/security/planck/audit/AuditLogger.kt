package security.planck.audit

import android.util.Log
import com.fsck.k9.BuildConfig
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
            /**
             * Empty rating field is allowed as correct format.
             */
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
            val allFileText = getAuditTextWithoutSignature()

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
            addSignature()
        } catch (e: IOException) {
            if (BuildConfig.DEBUG || K9.isDebug()) {
                Log.e(CONSOLE_LOG_TAG, "Error adding audit log", e)
            }
        }
    }

    private fun getAuditTextWithoutSignature(): String {
        var allFileText = if (auditLoggerFile.exists()) {
            auditLoggerFile.readText()
        } else {
            // TODO: if file was removed fire tampering warning. We will have a boolean "audit_log_exists" in some shared prefs file. Once file is recreated we update it.
            ""
        }
        if (allFileText.isNotBlank()) {
            val newFileText = verifyAndRemoveSignature(allFileText)
            allFileText = newFileText
        }
        return allFileText
    }

    private fun appendLog(log: MessageAuditLog) {
        auditLoggerFile.appendText(log.toCsv())
    }

    private fun writeLogRemovingOldLogs(
        allFileText: String,
        messageAuditLog: MessageAuditLog,
    ) {
        val newTime = messageAuditLog.timeStamp
        val cleanText = allFileText.removeOldLogsAndGarbage(newTime)
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
            }
                .getOrDefault(0) // in case of empty text / just header left, no need to change log time
            if (lastMessageTime > newTime) {
                log = log.copy(timeStamp = lastMessageTime)
            }
        }
        return log
    }

    private fun String.removeOldLogsAndGarbage(newTime: Long): String {
        // remove old logs and old garbage, keep new logs and new garbage in place
        val reAddHeader = this.startsWith("$HEADER$NEW_LINE")
        val textWithoutHeader = substringAfter("$HEADER$NEW_LINE")
        val lastOutdatedLog = findLastOutdatedLog(textWithoutHeader, newTime)
        val onlyNewLogsAndGarbage = lastOutdatedLog?.let {
            textWithoutHeader.substringAfter(
                "$lastOutdatedLog$NEW_LINE",
                missingDelimiterValue = "" // if no new line after last outdated log, even last log is outdated
            )
        } ?: textWithoutHeader
        return if (reAddHeader) "$HEADER$NEW_LINE$onlyNewLogsAndGarbage" else onlyNewLogsAndGarbage
    }

    private fun findLastOutdatedLog(textWithoutHeader: String, newTime: Long): String? {
        val allLines = textWithoutHeader.split(NEW_LINE)

        return allLines.lastOrNull { // we assume logs are entered in time order in the file
            kotlin.runCatching {
                MessageAuditLog.deserialize(it)
            }.map { it.isOutdated(newTime) }.getOrDefault(false)
        }
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
                setTamperedAlert()
            }
    }

    private fun verifyAndRemoveSignature(auditText: String): String {
        val signatureIndex = auditText.indexOf(SIGNATURE_START)
        val signature = if (signatureIndex < 0) "" else auditText.substring(signatureIndex)
        val textToVerify = auditText.substringBefore(SIGNATURE_START)
        verifyAuditText(textToVerify, signature)
        val newAuditText = when {
            textToVerify.substringAfterLast(NEW_LINE).isSignatureLog() ->
                textToVerify.substringBeforeLast(NEW_LINE)

            else -> textToVerify
        }
        auditLoggerFile.writeText(newAuditText)
        return newAuditText
    }

    private fun String.isSignatureLog() =
        kotlin.runCatching {
            MessageAuditLog.deserialize(this).senderId == SIGNATURE_ID
        }.getOrDefault(false)

    private fun verifyAuditText(auditText: String, signature: String) {
        if (signature.isBlank()
            || !planckProvider.verifySignature(auditText, signature)
                .getOrDefault(false) // same as tamper detected on failure
        ) {
            // tamper detected
            setTamperedAlert()
        }
    }

    private fun setTamperedAlert() {
        // TODO: if app is running in the background, then set last tamper time in some shared pref file. The value should be reset when warning is displayed.
        tamperAlertMF.value = tamperAlertMF.value + 1
    }

    fun resetTamperAlert() {
        tamperAlertMF.value = 0
    }

    companion object {
        const val AUDIT_LOGGER_ROUTE = "audit/messageAudit.csv"
        const val START_EVENT = "**AUDIT LOGGING START**"
        const val STOP_EVENT = "**AUDIT LOGGING STOP**"
        const val SIGNATURE_START = "-----BEGIN PGP MESSAGE-----"
        const val SIGNATURE_ID = "**SIGNATURE**"
        private const val SEPARATOR = ";"
        internal const val HEADER =
            "TIMESTAMP${SEPARATOR}SENDER-ID${SEPARATOR}SECURITY-RATING"
        private const val CONSOLE_LOG_TAG = "AuditLogger"
    }
}