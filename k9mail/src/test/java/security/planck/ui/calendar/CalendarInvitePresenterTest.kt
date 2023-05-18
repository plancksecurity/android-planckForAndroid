package security.planck.ui.calendar

import android.net.Uri
import biweekly.ICalendar
import com.fsck.k9.Account
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.*

@ExperimentalCoroutinesApi
class CalendarInvitePresenterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val view: CalendarInviteView = mockk(relaxed = true)
    private val viewDelegate: CalendarInviteViewDelegate = mockk(relaxed = true)
    private val account: Account = stubAccount()
    private lateinit var calendarAttachment: AttachmentViewInfo
    private val messageViewInfo: MessageViewInfo = stubMessageViewInfo()
    private lateinit var iCalendar: ICalendar
    private val presenter = CalendarInvitePresenter(
        coroutinesTestRule.testDispatcherProvider,
    )

    @Test
    fun `when presenter is initialized, view shows and hides loading`() = runTest {
        stubAttachment(TestICalendarCreator.getMacosCalendarInvite())


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()


        verify { view.showLoading() }
        verify { view.hideLoading() }
    }

    @Test
    fun `when presenter is initialized with a calendar from MacOS, populates view with calendar invite fields`() = runTest {
        val startDate = Date(Date().time + ONE_HOUR)
        val endDate = Date(startDate.time + ONE_HOUR)
        stubAttachment(TestICalendarCreator.getMacosCalendarInvite(startDate, endDate))


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()


        verify { view.setSummary(TestICalendarCreator.EVENT_SUMMARY) }
        verify {
            view.setShortInvitees(
                with(TestICalendarCreator) {
                    "$INVITEE_1_NAME ($INVITEE_1_EMAIL)"
                },
                2
            )
        }
        verify { view.setLocation(TestICalendarCreator.EVENT_LOCATION) }
        println("$startDate - $endDate")
        verify { view.setStartAndEndTime("$startDate - $endDate") }
    }

    @Test
    fun `when message content is not relevant, view changes it for event description`() = runTest {
        val startDate = Date(Date().time + ONE_HOUR)
        val endDate = Date(startDate.time + ONE_HOUR)
        stubAttachment(TestICalendarCreator.getMacosCalendarInvite(startDate, endDate))


        presenter.onHtmlSet("some content")
        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()


        verify { view.hideMessageContent() }
        verify { view.showDescription(iCalendar.events.first().description.value) }
    }

    @Test
    fun `when presenter is initialized with a calendar from Google, populates view with calendar invite fields`() = runTest {
        val startDate = Date(Date().time + ONE_HOUR)
        val endDate = Date(startDate.time + ONE_HOUR)
        stubAttachment(TestICalendarCreator.getGoogleCalendarRequest(startDate, endDate))


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()



        verify { view.setSummary(TestICalendarCreator.EVENT_SUMMARY) }
        verify {
            view.setShortInvitees(
                with(TestICalendarCreator) {
                    "$INVITEE_1_NAME ($INVITEE_1_EMAIL)"
                },
                2
            )
        }
        verify { view.setLocation(TestICalendarCreator.EVENT_LOCATION) }
        println("$startDate - $endDate")
        verify { view.setStartAndEndTime("$startDate - $endDate") }
    }

    @Test
    fun `if there was a problem parsing calendar invite, view shows error screen`() = runTest {
        val part: Part = mockk()
        every { part.body }.throws(RuntimeException())
        calendarAttachment = AttachmentViewInfo(
            "",
            "",
            0,
            Uri.EMPTY,
            false,
            part,
            true
        )


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()


        verify { view.showErrorScreen() }
    }

    @Test
    fun `on calendar button clicked view delegate opens calendar app`() = runTest {
        val invite = TestICalendarCreator.getMacosCalendarInvite()
        stubAttachment(invite)


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()
        presenter.openCalendar()


        verify { viewDelegate.openCalendarApp(calendarAttachment) }
    }

    @Test
    fun `if ViewDelegate has no calendar app, view_showNoCalendarApp() is called`() = runTest {
        val invite = TestICalendarCreator.getMacosCalendarInvite()
        stubAttachment(invite)
        every { viewDelegate.openCalendarApp(any()) }.returns(false)


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()
        presenter.openCalendar()


        verify { viewDelegate.openCalendarApp(calendarAttachment) }
        verify { view.showNoCalendarApp() }
    }

    @Test
    fun `showShortInvitees calls view_setShortInvitees`() = runTest {
        val invite = TestICalendarCreator.getMacosCalendarInvite()
        stubAttachment(invite)
        every { viewDelegate.openCalendarApp(any()) }.returns(false)


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()
        presenter.showShortInvitees()


        verify {
            view.setShortInvitees(
                with(TestICalendarCreator) {
                    "$INVITEE_1_NAME ($INVITEE_1_EMAIL)"
                },
                2
            )
        }
    }

    @Test
    fun `showLontInvitees calls view_setLongInvitees`() = runTest {
        val invite = TestICalendarCreator.getMacosCalendarInvite()
        stubAttachment(invite)
        every { viewDelegate.openCalendarApp(any()) }.returns(false)
        every { viewDelegate.getOrganizerTag() }.returns("Organizer")


        presenter.initialize(view, viewDelegate, calendarAttachment, messageViewInfo)
        advanceUntilIdle()
        presenter.showLongInvitees()


        verify {
            view.setLongInvitees(
                with(TestICalendarCreator) {
                    "$INVITEE_1_NAME ($INVITEE_1_EMAIL)" +
                            "\n$INVITEE_2_NAME ($INVITEE_2_EMAIL)" +
                            "\n$ORGANIZER_NAME ($ORGANIZER_EMAIL) [Organizer]"
                }
            )
        }
    }

    private fun stubAttachment(iCalendar: ICalendar) {
        this.iCalendar = iCalendar
        val data = this.iCalendar.write().toByteArray()
        val base64Calendar = Base64.encodeBase64(data)
        val body: BinaryMemoryBody = mockk()
        every { body.data }.returns(base64Calendar)
        val part: Part = mockk()
        every { part.body }.returns(body)

        calendarAttachment = AttachmentViewInfo(
            INVITE_CALENDAR_MIME_TYPE,
            INVITE_CALENDAR_FILE_NAME,
            MOCK_ATTACHMENT_SIZE,
            Uri.EMPTY,
            false,
            part,
            true
        )
    }

    private fun stubAccount(): Account {
        val account: Account = mockk()
        val identity: com.fsck.k9.Identity = com.fsck.k9.Identity().apply {
            this.email = TestICalendarCreator.INVITEE_1_EMAIL
            this.name = TestICalendarCreator.INVITEE_1_NAME
        }
        every { account.email }.returns(TestICalendarCreator.INVITEE_1_EMAIL)
        every { account.getIdentity(0) }.returns(identity)
        return account
    }

    private fun stubMessageViewInfo(): MessageViewInfo {
        val localMessage: LocalMessage = mockk()
        every { localMessage.account }.returns(account)
        val address: Address = mockk()
        every { address.personal }.returns(TestICalendarCreator.INVITEE_1_NAME)
        every { address.address }.returns(TestICalendarCreator.INVITEE_1_EMAIL)
        every { localMessage.getRecipients(Message.RecipientType.TO) }.returns(arrayOf(address))

        return MessageViewInfo(
            localMessage,
            false,
            mockk(),
            "",
            false,
            "",
            listOf(),
            mockk(),
            mockk(),
            "",
            listOf()
        )
    }

    companion object {
        private const val ONE_HOUR = 60 * 60 * 1000L
        private const val MOCK_ATTACHMENT_SIZE = 100L
        private const val INVITE_CALENDAR_MIME_TYPE = "text/calendar"
        private const val INVITE_CALENDAR_FILE_NAME = "invite.ics"
    }
}