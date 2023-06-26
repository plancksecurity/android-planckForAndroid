package security.planck.audit

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.NEW_LINE
import foundation.pEp.jniadapter.Rating
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import security.planck.audit.AuditLogger.Companion.HEADER
import java.io.File
import java.util.Calendar

class AuditLoggerTest {
    private val auditLoggerFile = File("messageAudit.csv")
    private val auditLogger = AuditLogger(auditLoggerFile, THIRTY_DAYS)
    private val mimeMessage: MimeMessage = mockk()
    private val mockCalendar: Calendar = mockk()

    @Before
    fun setUp() {
        auditLoggerFile.delete()
        every { mockCalendar.timeInMillis }.returns(WRITE_TIME * 1000)
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() }.returns(mockCalendar)
        val mockAddress: Address = mockk(relaxed = true)
        every { mockAddress.address }.returns(FROM)
        every { mimeMessage.from }.returns(arrayOf(mockAddress))
    }

    @After
    fun tearDown() {
        unmockkStatic(Calendar::class)
        auditLoggerFile.delete()
    }

    @Test
    fun `addMessageAuditLog adds a line with info on the message and rating, header is not added twice`() {
        auditLoggerFile.writeText(HEADER)


        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)


        val lines = auditLoggerFile.readLines()
        assertEquals(2, lines.size)
        assertEquals(HEADER, lines.first())
        assertEquals(
            EXPECTED_LOG_LINE,
            lines.last()
        )
    }

    @Test
    fun `addStartEventLog adds a start event to log file`() {
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStartEventLog()


        val lines = auditLoggerFile.readLines()
        assertEquals(2, lines.size)
        assertEquals(HEADER, lines.first())
        assertEquals(
            EXPECTED_START_LINE,
            lines.last()
        )
    }

    @Test
    fun `addStopEventLog adds a stop event to log file`() {
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStopEventLog(WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(2, lines.size)
        assertEquals(HEADER, lines.first())
        assertEquals(
            EXPECTED_STOP_LINE,
            lines.last()
        )
    }

    @Test
    fun `addStopEventLog does not add a stop event to log file if the time of stop event is too old`() {
        auditLoggerFile.writeText(HEADER)


        auditLogger.addStopEventLog(OLD_WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(1, lines.size)
        assertEquals(HEADER, lines.first())
    }

    @Test
    fun `if there are logs later than stop event, addStopEventLog changes stop event time to that of latest log`() {
        auditLoggerFile.writeText(HEADER)
        auditLoggerFile.appendText("$NEW_LINE$LATE_WRITE_TIME;$FROM;someRating")


        auditLogger.addStopEventLog(WRITE_TIME)


        val lines = auditLoggerFile.readLines()
        assertEquals(3, lines.size)
        assertEquals(HEADER, lines.first())
        assertEquals(
            EXPECTED_MODIFIED_STOP_LINE,
            lines.last()
        )
    }

    @Test
    fun `if the file is empty, the header is added`() {
        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)


        val lines = auditLoggerFile.readLines()
        assertEquals(2, lines.size)
        assertEquals(HEADER, lines.first())
        assertEquals(
            EXPECTED_LOG_LINE,
            lines.last()
        )
    }

    @Test
    fun `addMessageAuditLog removes old logs when it adds a new one`() {
        StringBuilder(HEADER).also { sb ->
            repeat(10) {
                sb.append("$NEW_LINE$OLD_WRITE_TIME;$FROM;$TELLTALE_RATING$it")
            }
            auditLoggerFile.writeText(sb.toString())
        }


        auditLogger.addStartEventLog()
        auditLogger.addMessageAuditLog(mimeMessage, Rating.pEpRatingUnencrypted)
        auditLogger.addStopEventLog(WRITE_TIME)


        assertEquals(
            "$HEADER$NEW_LINE$EXPECTED_START_LINE$NEW_LINE$EXPECTED_LOG_LINE$NEW_LINE$EXPECTED_STOP_LINE",
            auditLoggerFile.readText()
        )
    }

    companion object {
        private const val THIRTY_DAYS = 30 * 24 * 60 * 60L
        private const val WRITE_TIME = 9999999999L
        private const val OLD_WRITE_TIME = WRITE_TIME - THIRTY_DAYS - 1
        private const val LATE_WRITE_TIME = WRITE_TIME + 1
        private const val FROM = "testfrom@from.ch"
        private const val TELLTALE_RATING = "TELLTALE_RATING"
        private val EXPECTED_LOG_LINE =
            "$WRITE_TIME;$FROM;${PlanckUtils.ratingToString(Rating.pEpRatingUnencrypted)}"
        private const val EXPECTED_START_LINE = "$WRITE_TIME;AUDIT LOGGING START;"
        private const val EXPECTED_STOP_LINE = "$WRITE_TIME;AUDIT LOGGING STOP;"
        private const val EXPECTED_MODIFIED_STOP_LINE = "$LATE_WRITE_TIME;AUDIT LOGGING STOP;"
    }
}