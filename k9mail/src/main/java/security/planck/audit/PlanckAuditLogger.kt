package security.planck.audit

import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.Clock
import com.fsck.k9.K9
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.preferences.Storage
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanckAuditLogger(
    private val planckProvider: PlanckProvider,
    private val auditLoggerFile: File,
    private val storage: Storage,
    private val k9: K9,
    private val clock: Clock,
    override var logAgeLimit: Long,
) : AuditLogger {
    @Inject
    constructor(
        planckProvider: PlanckProvider,
        storage: Storage,
        k9: K9,
        clock: Clock,
    ) : this(
        planckProvider,
        File(k9.filesDir, AUDIT_LOGGER_ROUTE),
        storage,
        k9,
        clock,
        k9.auditLogDataTimeRetentionValue
    )

    private val currentTimeInSeconds: Long
        get() = clock.time / 1000

    private val tamperAlertMF: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tamperAlertFlow: StateFlow<Boolean> = tamperAlertMF.asStateFlow()

    init {
        auditLoggerFile.parentFile?.mkdirs()
        val pendingAlert =
            storage.lastTamperingDetectedTime > 0
                    || storage.persistentWarningOnStartup()
        if (pendingAlert) {
            setTamperedAlert()
        }
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
                try {
                    val parts = serialized.split(SEPARATOR)
                    if (parts.size != 3) error("3 parts expected for log: $serialized")
                    return MessageAuditLog(parts[0].toLong(), parts[1], parts[2])
                } catch (e: Exception) {
                    throw LogBadlyFormattedException(serialized, e)
                }
            }
        }
    }

    private fun MessageAuditLog.isOutdated(newTime: Long): Boolean =
        newTime - timeStamp > logAgeLimit

    override fun addStartEventLog() {
        addSpecialEventLog(START_EVENT, currentTimeInSeconds)
    }

    override fun addStopEventLog(stopTime: Long) {
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
    override fun addMessageAuditLog(message: MimeMessage, rating: Rating) {
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
                    storage.edit().setAuditLogFileExists(true)
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
        var allFileText = getAllFileText()
        if (allFileText.isNotBlank()) {
            val newFileText = verifyAndRemoveSignature(allFileText)
            allFileText = newFileText
        }
        return allFileText
    }

    private fun getAllFileText() = if (auditLoggerFile.exists()) {
        auditLoggerFile.readText()
    } else {
        checkPreviousFileExistenceOnNonExistentFile()
        ""
    }

    private fun checkPreviousFileExistenceOnNonExistentFile() {
        val auditLogExisted = storage.auditLogFileExists()
        if (auditLogExisted) {
            setTamperedAlertAndSaveTime()
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

        return when {
            reAddHeader ->
                if (onlyNewLogsAndGarbage.isBlank()) HEADER
                else "$HEADER$NEW_LINE$onlyNewLogsAndGarbage"

            else -> onlyNewLogsAndGarbage
        }
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
                setTamperedAlertAndSaveTime()
            }
    }

    private fun verifyAndRemoveSignature(auditText: String): String {
        val signatureStartIndex = auditText.indexOf(SIGNATURE_START)
        val signatureEndIndex = auditText.indexOf(SIGNATURE_END) + SIGNATURE_END.length
        var textToVerify = auditText
        var signature = "" // by default if signature is not found, it is empty
        var newAuditText = auditText
        if (signatureHasRightFormat(signatureStartIndex, signatureEndIndex)
        ) {
            val beforeSignature = auditText.substring(0, signatureStartIndex)
            val afterSignature = auditText.substring(signatureEndIndex).removeSuffix(NEW_LINE)
            signature = auditText.substring(signatureStartIndex, signatureEndIndex)
            textToVerify = beforeSignature + afterSignature
            newAuditText =
                removeSignatureLogIfPresent(beforeSignature, afterSignature, textToVerify)
        }

        verifyAuditText(textToVerify, signature)
        auditLoggerFile.writeText(newAuditText)
        return newAuditText
    }

    private fun signatureHasRightFormat(signatureStartIndex: Int, signatureEndIndex: Int) =
        signatureStartIndex >= 0
                && signatureEndIndex - signatureStartIndex == SIGNATURE_EXPECTED_LENGTH

    private fun removeSignatureLogIfPresent(
        beforeSignature: String,
        afterSignature: String,
        textToVerify: String
    ) = when {
        beforeSignature.substringAfterLast(NEW_LINE).isSignatureLog() ->
            beforeSignature.substringBeforeLast(NEW_LINE) + afterSignature // Remove signature timestamp log if present

        else -> textToVerify
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
            setTamperedAlertAndSaveTime()
        }
    }

    private fun setTamperedAlertAndSaveTime() {
        if (!k9.isRunningInForeground) {
            storage.edit().setLastTamperingDetectedTime(currentTimeInSeconds)
        }
        setTamperedAlert()
    }

    private fun setTamperedAlert() {
        tamperAlertMF.value = true
    }


    override fun resetTamperAlert() {
        tamperAlertMF.value = false
        storage.edit().setLastTamperingDetectedTime(0L)
    }

    override fun enablePersistentWarningOnStartup() {
        storage.edit().setPersistentAuditTamperWarningOnStartup(true)
    }

    override fun disablePersistentWarningOnStartup() {
        storage.edit().setPersistentAuditTamperWarningOnStartup(false)
    }

    override fun checkPendingTamperingWarningFromBackground() {
        if (storage.lastTamperingDetectedTime > 0) {
            setTamperedAlert()
        }
    }

    companion object {
        const val AUDIT_LOGGER_ROUTE = "audit/messageAudit.csv"
        internal const val START_EVENT = "**AUDIT LOGGING START**"
        internal const val STOP_EVENT = "**AUDIT LOGGING STOP**"
        internal const val SIGNATURE_START = "-----BEGIN PGP MESSAGE-----"
        internal const val SIGNATURE_END = "-----END PGP MESSAGE-----"
        internal const val SIGNATURE_EXPECTED_LENGTH = 926
        internal const val SIGNATURE_ID = "**SIGNATURE**"
        internal const val SEPARATOR = ";"
        internal const val HEADER =
            "TIMESTAMP${SEPARATOR}SENDER-ID${SEPARATOR}SECURITY-RATING"
        private const val CONSOLE_LOG_TAG = "AuditLogger"
    }
}