package security.planck.audit

import com.fsck.k9.K9
import com.fsck.k9.TestClock
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import foundation.pEp.jniadapter.Rating
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import security.planck.audit.PlanckAuditLogger.Companion.HEADER
import security.planck.audit.PlanckAuditLogger.Companion.SEPARATOR
import security.planck.audit.PlanckAuditLogger.Companion.SIGNATURE_ID
import security.planck.audit.PlanckAuditLogger.Companion.START_EVENT
import security.planck.audit.PlanckAuditLogger.Companion.STOP_EVENT
import java.io.File

@ExperimentalCoroutinesApi
class AuditLoggerTest {
    private val auditLoggerFile = File("messageAudit.csv")
    private val planckProvider: PlanckProvider = mockk()
    private val storageEditor: StorageEditor = mockk(relaxed = true)
    private val storage: Storage = mockk(relaxed = true) {
        every { edit() }.returns(storageEditor)
    }
    private val k9: K9 = mockk()
    private val clock = TestClock()
    private lateinit var auditLogger: AuditLogger
    private val mimeMessage: MimeMessage = mockk()
    private val collectedStates = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        auditLoggerFile.delete()
        collectedStates.clear()
        clock.time = WRITE_TIME * 1000
        every { planckProvider.getSignatureForText(any()) }
            .returns(ResultCompat.success(VALID_SIGNATURE))
        every { k9.isRunningInForeground }.returns(false)
        every { planckProvider.verifySignature(any(), any()) }.returns(ResultCompat.success(true))
        val mockAddress: Address = mockk(relaxed = true)
        every { mockAddress.address }.returns(FROM)
        every { mimeMessage.from }.returns(arrayOf(mockAddress))
    }

    @After
    fun tearDown() {
        auditLoggerFile.delete()
    }

    private fun initializeAuditLogger() {
        auditLogger = PlanckAuditLogger(
            planckProvider,
            auditLoggerFile,
            storage,
            k9,
            clock,
            THIRTY_DAYS
        )
        observeAuditLogWarning()
    }

    @Test
    fun `initially warning flow is unset`() {
        initializeAuditLogger()


        assertWarningStates(false)
    }

    @Test
    fun `on startup the audit log file folder is created`() {
        initializeAuditLogger()


        assertTrue(File("..").exists())
    }

    @Test
    fun `on startup if there was a warning fired in the background the warning flow is set`() {
        every { storage.lastTamperingDetectedTime }.returns(PENDING_WARNING_TIME)


        initializeAuditLogger()


        verify { storage.lastTamperingDetectedTime }
        assertWarningStates(true)
    }

    @Test
    fun `on startup if there is a persistent warning the warning flow is set`() {
        every { storage.persistentWarningOnStartup() }.returns(true)


        initializeAuditLogger()


        assertWarningStates(true)
    }

    @Test
    fun `when adding a log, if the log file was removed the warning flow is set`() {
        auditLoggerFile.delete()
        every { storage.auditLogFileExists() }.returns(true)


        initializeAuditLogger()
        auditLogger.addStartEventLog()


        assertWarningStates(false, true)
    }

    @Test
    fun `when adding a log, if file text was blank or the file did not exist, file existence is marked in Storage`() {
        auditLoggerFile.delete()


        initializeAuditLogger()
        auditLogger.addStartEventLog()


        verify { storage.edit() }
        verify { storageEditor.setAuditLogFileExists(true) }
    }

    @Test
    fun `resetTamperAlert() unsets warning flow and resets last tampered time in Storage`() {
        initializeAuditLogger()


        auditLogger.resetTamperAlert()


        verify { storage.edit() }
        verify { storageEditor.setLastTamperingDetectedTime(0) }
        assertWarningStates(false)
    }

    @Test
    fun `enablePersistentWarningOnStartup() sets persistent warning on startup in Storage`() {
        initializeAuditLogger()


        auditLogger.enablePersistentWarningOnStartup()


        verify { storage.edit() }
        verify { storageEditor.setPersistentAuditTamperWarningOnStartup(true) }
    }

    @Test
    fun `disablePersistentWarningOnStartup() unsets persistent warning on startup in Storage`() {
        initializeAuditLogger()


        auditLogger.disablePersistentWarningOnStartup()


        verify { storage.edit() }
        verify { storageEditor.setPersistentAuditTamperWarningOnStartup(false) }
    }

    @Test
    fun `checkPendingTamperingWarningFromBackground() sets the warning flow if there is a pending warning from background`() {
        every { storage.lastTamperingDetectedTime }.returns(PENDING_WARNING_TIME)
        initializeAuditLogger()


        auditLogger.checkPendingTamperingWarningFromBackground()


        verify { storage.lastTamperingDetectedTime }
        assertWarningStates(true)
    }


    @Test
    fun `addMessageAuditLog adds a line with info on the message and rating, header is not added twice`() {
        initializeAuditLogger()
        auditLoggerFile.writeText(HEADER)


        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)


        println(auditLoggerFile.readText())
        val lines = auditLoggerFile.readLines()
        assertEquals(20, lines.size)
        assertAuditText(
            """
$HEADER
$EXPECTED_LOG_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `addStartEventLog adds a start event to log file`() {
        initializeAuditLogger()
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStartEventLog()


        val lines = auditLoggerFile.readLines()
        assertEquals(20, lines.size)
        assertAuditText(
            """
$HEADER
$EXPECTED_START_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `addStopEventLog adds a stop event to log file`() {
        initializeAuditLogger()
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStopEventLog(WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(20, lines.size)
        assertAuditText(
            """
$HEADER
$EXPECTED_STOP_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `addStopEventLog does not add a stop event to log file if the time of stop event is too old`() {
        initializeAuditLogger()
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStopEventLog(OLD_WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(1, lines.size)
        assertAuditText(
            """
$HEADER
            """.trimIndent()
        )
    }

    @Test
    fun `if there are logs later than stop event, addStopEventLog changes stop event time to that of latest log`() {
        initializeAuditLogger()
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")


        auditLogger.addStopEventLog(WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(21, lines.size)
        assertAuditText(
            """
$HEADER
$LATE_WRITE_TIME;$FROM;someRating
$EXPECTED_MODIFIED_STOP_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `if the file is empty, the header is added with the signature`() {
        initializeAuditLogger()
        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)


        val lines = auditLoggerFile.readLines()
        assertEquals(20, lines.size)
        assertAuditText(
            """
$HEADER
$EXPECTED_LOG_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `addMessageAuditLog removes old logs when it adds a new one`() {
        initializeAuditLogger()
        StringBuilder(HEADER).also { sb ->
            repeat(10) {
                sb.append("$NEW_LINE$OLD_WRITE_TIME;$FROM;$TELLTALE_RATING$it")
            }
            auditLoggerFile.writeText(sb.toString())
        }


        auditLogger.addStartEventLog()
        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)
        auditLogger.addStopEventLog(WRITE_TIME)

        assertAuditText(
            """
$HEADER
$EXPECTED_START_LINE
$EXPECTED_LOG_LINE
$EXPECTED_STOP_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `addMessageAuditLog removes old garbage when it adds new content`() {
        initializeAuditLogger()
        StringBuilder(HEADER).also { sb ->
            repeat(10) {
                sb.append("$NEW_LINE$OLD_WRITE_TIME;$FROM;$TELLTALE_RATING$it")
                sb.append("$NEW_LINE$GARBAGE_LINE")
            }
            auditLoggerFile.writeText(sb.toString())
        }


        auditLogger.addStartEventLog()
        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)
        auditLogger.addStopEventLog(WRITE_TIME)

        assertAuditText(
            """
$HEADER
$GARBAGE_LINE // last garbage line is kept, since it was after last old log.
$EXPECTED_START_LINE
$EXPECTED_LOG_LINE
$EXPECTED_STOP_LINE
$EXPECTED_SIGNATURE_LINE
            """.trimIndent()
        )
    }

    @Test
    fun `AuditLogger uses PlanckProvider to verify file signature if a valid signature is found`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText(EXPECTED_SIGNATURE_LINE)
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify { planckProvider.verifySignature(any(), any()) }
    }

    @Test
    fun `AuditLogger removes signature and calls PlanckProvider_verifySignature with rest of signature line`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText("${NEW_LINE}$EXPECTED_SIGNATURE_LINE")
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify {
            planckProvider.verifySignature(
                """
$HEADER
$LATE_WRITE_TIME;$FROM;someRating
$WRITE_TIME$SEPARATOR$SIGNATURE_ID$SEPARATOR
                """.trimIndent(),
                VALID_SIGNATURE
            )
        }
    }

    @Test
    fun `AuditLogger sets warning flow if signature check is successful but negative`() {
        every { planckProvider.verifySignature(any(), any()) }.returns(ResultCompat.success(false))
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText(EXPECTED_SIGNATURE_LINE)
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify { planckProvider.verifySignature(any(), any()) }
        assertWarningStates(false, true)
    }

    @Test
    fun `AuditLogger sets warning flow if signature check fails`() {
        every { planckProvider.verifySignature(any(), any()) }.returns(
            ResultCompat.failure(
                RuntimeException("test")
            )
        )
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText(EXPECTED_SIGNATURE_LINE)
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify { planckProvider.verifySignature(any(), any()) }
        assertWarningStates(false, true)
    }

    @Test
    fun `AuditLogger sets warning flow if getting new signature fails`() {
        every { planckProvider.getSignatureForText(any()) }.returns(
            ResultCompat.failure(
                RuntimeException("test")
            )
        )
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText(EXPECTED_SIGNATURE_LINE)
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify { planckProvider.getSignatureForText(any()) }
        assertWarningStates(false, true)
    }

    @Test
    fun `when adding a log and file is blank, signature is not checked and warning flow is not set`() {
        every { planckProvider.verifySignature(any(), any()) }.returns(ResultCompat.success(false))
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify(exactly = 0) { planckProvider.verifySignature(any(), any()) }
        assertWarningStates(false)
    }

    @Test
    fun `AuditLogger sets warning flow if no valid signature is found and no call to PlanckProvider is done to verify`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText("$NEW_LINE$INVALID_SIGNATURE")
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify(exactly = 0) { planckProvider.verifySignature(any(), any()) }
        assertWarningStates(false, true)
    }

    @Test
    fun `AuditLogger sets warning flow if no signature is found and no call to PlanckProvider is done to verify`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify(exactly = 0) { planckProvider.verifySignature(any(), any()) }
        assertWarningStates(false, true)
    }

    @Test
    fun `When a tampering event is detected with the app running in background, last event time is saved in Storage`() {
        every { k9.isRunningInForeground }.returns(false)
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText("$NEW_LINE$INVALID_SIGNATURE")
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        verify { storage.edit() }
        verify { storageEditor.setLastTamperingDetectedTime(clock.time / 1000) }
    }

    @Test
    fun `AuditLogger leaves non-valid signature in place as garbage`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")
        auditLoggerFile.appendText("$NEW_LINE$INVALID_SIGNATURE")
        initializeAuditLogger()


        auditLogger.addStartEventLog()


        assertAuditText(
            """
$HEADER
$LATE_WRITE_TIME;$FROM;someRating
$INVALID_SIGNATURE
$EXPECTED_START_LINE
$EXPECTED_SIGNATURE_LINE
        """.trimIndent()
        )
    }


    private fun assertWarningStates(vararg states: Boolean) {
        assertEquals(states.toList(), collectedStates)
    }

    private fun observeAuditLogWarning() {
        auditLogger.tamperAlertFlow.onEach {
            collectedStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher()))
    }

    private fun assertAuditText(text: String) {
        assertEquals(text, auditLoggerFile.readText())
    }

    companion object {
        private const val THIRTY_DAYS = 30 * 24 * 60 * 60L
        private const val WRITE_TIME = 9999999999L
        private const val PENDING_WARNING_TIME = 10000L
        private const val OLD_WRITE_TIME = WRITE_TIME - THIRTY_DAYS - 1
        private const val LATE_WRITE_TIME = WRITE_TIME + 1
        private const val FROM = "testfrom@from.ch"
        private const val TELLTALE_RATING = "TELLTALE_RATING"
        private val EXPECTED_LOG_LINE =
            "$WRITE_TIME;$FROM;${PlanckUtils.ratingToString(Rating.pEpRatingUnencrypted)}"
        private const val EXPECTED_START_LINE = "$WRITE_TIME;$START_EVENT;"
        private const val EXPECTED_STOP_LINE = "$WRITE_TIME;$STOP_EVENT;"
        private const val EXPECTED_MODIFIED_STOP_LINE = "$LATE_WRITE_TIME;$STOP_EVENT;"
        private const val VALID_SIGNATURE = "-----BEGIN PGP MESSAGE-----\n" + // 18 LINES
                "\n" +
                "wsG7BAABCgBvBYJlH9W1CRCHrZDKbIBivEcUAAAAAAAeACBzYWx0QG5vdGF0aW9u\n" +
                "cy5zZXF1b2lhLXBncC5vcmcbPb27KLAcu9Ng22QD0BQ8XcsmNKnU06nWGZSLk0Kq\n" +
                "NxYhBBFTwmNDpREBpzPoeoetkMpsgGK8AADiaRAAyWWfcgl3ni+2QM8N62f2OX/R\n" +
                "NQCSq9NvBQKQ9QNtoLVxDkuepeO82wayf0hGgQexUrpSrBAjPuEFvj+lXztA5EB4\n" +
                "u0ZX5K/7OAy0wETgimAJwj/UWAilBM5QzNP2jbgFsNPL9eXMoI8Oqyk2ZhHfZXlj\n" +
                "AHnlu2b4PfQSU5VPX5oz922sx5dtDEBFV7hddNRI1tXGjTz6/hE1blXRXAqD7aNP\n" +
                "BUI29nvOfAwjc0TeIEwOoLyl9jHBrkfuyQPkGPDu+tRUmKz33pgXGXN3Ag7oR75b\n" +
                "W+q4mudaTlc7x1DlL0rdqLtwhHSufRjIgaAjFg+bfuuyZL0YyDBmdJbjEbiv/tPt\n" +
                "o0+4AG05AkXwyjyxx4U0L5RdnzNTCfIM17NhMnoTI28fKsmM4gmtFdWchro9mvdC\n" +
                "u7p9Ta5+sSprKZHH37IecN7MGOTVikTL6/2QcNkiUlnQViDuWrGSnXczJumHLnJm\n" +
                "v43K+2cVHNc9ooiqbj9o2NTfWNCB+HA8WOytQckkJ8PnjhiVi+nA3POgcwmfS0WJ\n" +
                "H7UhjL1sI5orbYIw9FKP9ff0Y67+cUy8dR0gvFXAlage8KT2aeenwDwfPPn1+BkQ\n" +
                "yZ9q1DYLX+Qreo3SJgTEPgXihVcltJYvikYYqKLWmEvoU3RariB0RKlu8iD36UUG\n" +
                "i9GSyydSA0huD41JZwg=\n" +
                "=gjCC\n" +
                "-----END PGP MESSAGE-----"

        private const val INVALID_SIGNATURE = "INVALID_SIGNATURE"
        private const val GARBAGE_LINE = "SOME_GARBAGE"

        private const val EXPECTED_SIGNATURE_LINE =
            "$WRITE_TIME$SEPARATOR$SIGNATURE_ID$SEPARATOR$VALID_SIGNATURE"
    }
}