package security.planck.audit

import android.util.Log
import com.fsck.k9.BuildConfig
import com.fsck.k9.Clock
import com.fsck.k9.K9
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.preferences.Storage
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlanckAuditLogger implementation of the AuditLogger interface which allowed to handle majority of the audit log related operations
 *
 * Audit Logs ViewModel is responsible to create a LifeData for sharing the update events
 * about tampering audit log with UI subscribers inherited K9Activity.
 * @constructor Creates a single instance of the PlanckAuditLogger
 * @property planckProvider [PlanckProvider] Current unix-time
 * @property auditLoggerFile [File] Actual audit log file for storing events about the security-related actions
 * @property storage [Storage] Database and other file related data storing operation class implementation
 * @property k9 [K9] Instance of the application singleton class
 * @property clock [Clock] RealClock implementation of the Clock interface for tests purposes
 * @property logAgeLimit [Long] Duration until the log record become expired
 * @property dispatcherProvider [DispatcherProvider] DefaultDispatcherProvider implementation of DispatcherProvider for handling events within the different Coroutines pools, etc.
*/
@Singleton
class PlanckAuditLogger(
    private val planckProvider: PlanckProvider,
    private val auditLoggerFile: File,
    private val storage: Storage,
    private val k9: K9,
    private val clock: Clock,
    override var logAgeLimit: Long,
    private val dispatcherProvider: DispatcherProvider,
) : AuditLogger {
    @Inject
    constructor(
        planckProvider: PlanckProvider,
        storage: Storage,
        k9: K9,
        clock: Clock,
        dispatcherProvider: DispatcherProvider,
    ) : this(
        planckProvider,
        File(k9.filesDir, AUDIT_LOGGER_ROUTE),
        storage,
        k9,
        clock,
        k9.auditLogDataTimeRetentionValue,
        dispatcherProvider
    )

    /**
    * @property currentTimeInSeconds [Long] Current unix-time
    */
    private val currentTimeInSeconds: Long
        get() = clock.time / 1000L

    /**
     * @property tamperAlertMF
     *
     * MutableStateFlow with default log state of undetected tampering
     */
    private val tamperAlertMF: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * @property tamperAlertFlow
     *
     * [StateFlow] responsible for handling different security states of the audit log.
     */
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
                MessageAuditLog.deserialize(it).isOutdated(newTime)
            }.getOrDefault(false)
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


    override suspend fun resetTamperAlert() = withContext(dispatcherProvider.io()) {
        tamperAlertMF.value = false
        storage.edit().setLastTamperingDetectedTime(0L)
    }

    override suspend fun enablePersistentWarningOnStartup() = withContext(dispatcherProvider.io()) {
        storage.edit().setPersistentAuditTamperWarningOnStartup(true)
    }

    override suspend fun disablePersistentWarningOnStartup() =
        withContext(dispatcherProvider.io()) {
            storage.edit().setPersistentAuditTamperWarningOnStartup(false)
        }

    override fun checkPendingTamperingWarningFromBackground() {
        CoroutineScope(dispatcherProvider.io()).launch {
            if (storage.lastTamperingDetectedTime > 0) {
                setTamperedAlert()
            }
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