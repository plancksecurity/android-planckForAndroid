package security.planck.audit

import android.util.Log
import com.fsck.k9.K9
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.infrastructure.threading.PlanckEnginePool
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Calendar

val PlanckDispatcher by lazy { PlanckEnginePool().asCoroutineDispatcher() }
class AuditLogger(
    private val planckProvider: PlanckProvider,
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
        val securityRating: String = "",
        val signature: String = ""
    ) {
        fun toCsv(): String =
            "$NEW_LINE$timeStamp$SEPARATOR$senderId$SEPARATOR$securityRating$SEPARATOR$signature"

        fun isStopEvent(): Boolean = senderId == STOP_EVENT
    }

    fun addStartEventLog() {
        runBlocking {
            withContext(PlanckDispatcher) {
                ResultCompat.of {
                    planckProvider.getSignatureForText(START_EVENT)
                }.onSuccess {
                    addSpecialEventLog(
                        START_EVENT,
                        currentTimeInSeconds,
                        it.getOrThrow()
                    )
                }.onFailure {
                    if (K9.isDebug()) {
                        Log.e(CONSOLE_LOG_TAG, "Error adding start event", it)
                    }
                }
            }
        }
    }

    fun addStopEventLog(stopTime: Long) {
        if (currentTimeInSeconds - stopTime < logAgeLimit) {
            runBlocking {
                withContext(PlanckDispatcher) {
                    ResultCompat.of {
                        planckProvider.getSignatureForText(START_EVENT)
                    }.onSuccess {
                        addSpecialEventLog(STOP_EVENT, stopTime, it.getOrThrow())
                    }.onFailure {
                        if (K9.isDebug()) {
                            Log.e(CONSOLE_LOG_TAG, "Error adding stop event", it)
                        }
                    }
                }
            }
        }
    }

    private fun addSpecialEventLog(eventName: String, eventTime: Long, signature: String) {
        addMessageAuditLog(
            MessageAuditLog(
                timeStamp = eventTime,
                senderId = eventName,
                signature=signature
            )
        )
    }

    @Synchronized
    fun addMessageAuditLog(message: MimeMessage, rating: Rating) {
        runBlocking {
            withContext(PlanckDispatcher) {
                ResultCompat.of {
                    planckProvider.getSignatureForText(message.from.firstOrNull()?.address.toString())
                }.onSuccess {
                    addMessageAuditLog(
                        MessageAuditLog(
                            currentTimeInSeconds,
                            message.from.firstOrNull()?.address.toString(),
                            PlanckUtils.ratingToString(rating),
                            it.getOrThrow()
                        )
                    )
                }.onFailure {
                    if (K9.isDebug()) {
                        Log.e(CONSOLE_LOG_TAG, "Error adding log event", it)
                    }
                }
            }
        }
    }

    private fun addMessageAuditLog(messageAuditLog: MessageAuditLog) {
        try {
            val allFileText = if (auditLoggerFile.exists()) auditLoggerFile.readText() else ""

            when {
                allFileText.isBlank() -> {
                    addHeader()
                    appendLog(log = messageAuditLog, blankFile = true)
                }

                allFileText == HEADER -> {
                    appendLog(log = messageAuditLog, blankFile = false)
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

    @Throws(TamperProofException::class)
    private fun appendLog(log: MessageAuditLog, blankFile: Boolean = false) {
        if (!blankFile) {
            val previousLogRecord = getPreviousLogOrThrow()
            verifyOrThrow(previousLogRecord.senderId, previousLogRecord.signature)
        }
        auditLoggerFile.appendText(log.toCsv())
    }

    @Throws(TamperProofException::class)
    private fun getPreviousLogOrThrow(): MessageAuditLog {

        //TODO: read previous log record and parse it for the further verification
        return MessageAuditLog(0L,"")
    }

    @Throws(TamperProofException::class)
    private fun verifyOrThrow(text: String, signature: String) {
        runBlocking {
            withContext(PlanckDispatcher) {
                ResultCompat.of {
                    planckProvider.verifySignature(text, signature)
                }.onSuccess {
                    if (K9.isDebug()) {
                        Log.i(CONSOLE_LOG_TAG, "Verification confirmation: verified")
                    }
                }.onFailure {
                    if (K9.isDebug()) {
                        Log.e(CONSOLE_LOG_TAG, "Error verifyOrThrow", it)
                    }
                    throw TamperProofException(critical = true, message = it.message, cause = it)
                }
            }
        }
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
            val lastMessageTime = getLogTime(cleanText.substringAfterLast(NEW_LINE))
            if (lastMessageTime > newTime) {
                log = log.copy(timeStamp = lastMessageTime)
            }
        }
        return log
    }

    //TODO: Double-check if it legit according to tamper proof audit log otherwise it might be exploit

    private fun String.removeOldLogs(newTime: Long): String {
        val textWithoutHeader = substringAfter("$HEADER$NEW_LINE")
            .split(NEW_LINE)
            .filter { logLine ->
                logLine.isNotBlank() && newTime - getLogTime(logLine) < logAgeLimit
            }.joinToString(NEW_LINE)
        return "$HEADER$NEW_LINE" + textWithoutHeader
    }

    private fun getLogTime(logLine: String): Long {
        return logLine.substringBefore(SEPARATOR).toLong()
    }

    private fun addHeader() {
        auditLoggerFile.writeText(HEADER)
    }

    companion object {
        const val AUDIT_LOGGER_ROUTE = "audit/messageAudit.csv"
        const val START_EVENT = "AUDIT LOGGING START"
        const val STOP_EVENT = "AUDIT LOGGING STOP"
        private const val SEPARATOR = ";"
        internal const val HEADER =
            "TIMESTAMP${SEPARATOR}SENDER-ID${SEPARATOR}SECURITY-RATING"
        private const val CONSOLE_LOG_TAG = "AuditLogger"
    }
}