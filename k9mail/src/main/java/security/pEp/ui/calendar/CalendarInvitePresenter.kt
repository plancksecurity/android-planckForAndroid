package security.pEp.ui.calendar

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.FileBackedBody
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.view.MessageWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CalendarInvitePresenter @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
) : MessageWebView.OnHtmlSetListener {

    private lateinit var view: CalendarInviteView
    private lateinit var viewDelegate: CalendarInviteViewDelegate
    private lateinit var messageViewInfo: MessageViewInfo
    private lateinit var calendarAttachment: AttachmentViewInfo
    private lateinit var icalendar: ICalendar
    private val coroutineScope = CoroutineScope(
        dispatcherProvider.main() + SupervisorJob()
    )
    private var messageContent: String = ""

    fun initialize(
        view: CalendarInviteView,
        viewDelegate: CalendarInviteViewDelegate,
        calendarAttachment: AttachmentViewInfo,
        messageViewInfo: MessageViewInfo,
    ) {
        this.view = view
        this.viewDelegate = viewDelegate
        this.messageViewInfo = messageViewInfo
        this.calendarAttachment = calendarAttachment
        renderCalendarInvite()
    }

    fun openCalendar() {
        if (!viewDelegate.openCalendarApp(calendarAttachment)) {
            view.showNoCalendarApp()
        }
    }

    fun showLongInvitees() {
        val event = icalendar.events.first()
        val invitees = event.getInvitees()
        showText(
            invitees.joinToString("\n"),
            view::setLongInvitees,
            view::hideInvitees
        )
    }

    fun showShortInvitees() {
        val event = icalendar.events.first()
        showShortInvitees(event)
    }

    private fun VEvent.getInvitees(): List<String> {
        val invitees = attendees.mapNotNull { invitee ->
            val displayName = getInviteeDisplayName(invitee.commonName, invitee.email)
            if (organizer != null && invitee.email != null && invitee.email == organizer.email) {
                displayName?.plus(" [organizer]")
            } else displayName
        }
        organizer?.let { organizer ->
            if (attendees.find { it.email == organizer.email } == null) {
                getInviteeDisplayName(organizer.commonName, organizer.email)?.let {
                    return invitees + "$it [organizer]"
                }
            }
        }
        return invitees
    }

    private fun getInviteeDisplayName(name: String?, email: String?): String? {
        return if (name.isNullOrBlank()) {
            if (!email.isNullOrBlank()) email else null
        } else {
            if (email.isNullOrBlank() || name == email) {
                name
            } else {
                "$name ($email)"
            }
        }
    }

    private fun renderCalendarInvite() {
        coroutineScope.launch {
            view.showLoading()
            parseCalendarFile().onSuccess { calendar ->
                icalendar = calendar
                if (icalendar.events.isEmpty()) {
                    Timber.e("No events in calendar invite!")
                } else {
                    populateCalendarInvite()
                }
            }.onFailure {
                view.showErrorScreen()
            }
            view.hideLoading()
        }
    }

    private fun populateCalendarInvite() {
        val event = icalendar.events.first()
        showOrHideSummary(event)
        replaceMessageContentIfNeeded()
        showOrHideLocation(event)
        showOrHideDates(event)
        showShortInvitees(event)
    }

    private fun showOrHideSummary(event: VEvent) {
        val summary = event.summary?.value?.trim()
        showText(
            summary,
            view::setSummary,
            view::hideSummary
        )
    }

    private fun showOrHideLocation(event: VEvent) {
        val location = event.location?.value?.trim()
        showText(
            location,
            view::setLocation,
            view::hideLocation
        )
    }

    private fun showOrHideDates(event: VEvent) {
        val startTime = event.dateStart?.value?.let { Date(it.time) }
        val endTime = event.dateEnd?.value?.let { Date(it.time) }
        val timeText =
            if (startTime != null && endTime != null) {
                "$startTime - $endTime"
            } else {
                null
            }
        showText(
            timeText,
            view::setStartAndEndTime,
            view::hideStartAndEndTime
        )
    }

    private fun showShortInvitees(event: VEvent) {
        val invitees = event.getInvitees()
        if (invitees.isNotEmpty()) {
            val rest = invitees.size - 1
            view.setShortInvitees(invitees.first(), rest)
        } else {
            view.hideInvitees()
        }
    }

    private fun showText(
        text: String?,
        onAvailable: (String) -> Unit,
        onNotAvailable: () -> Unit
    ) {
        if (text.isNullOrBlank()) {
            onNotAvailable()
        } else {
            onAvailable(text)
        }
    }

    private suspend fun parseCalendarFile(): Result<ICalendar> {
        return withContext(dispatcherProvider.io()) {
            parseCalendarFileInternal()
        }
    }

    private fun parseCalendarFileInternal(): Result<ICalendar> {
        return kotlin.runCatching {
            val data = when (val body: Body = calendarAttachment.part.body) {
                is BinaryMemoryBody ->
                    body.data
                is FileBackedBody ->
                    body.inputStream.readBytes()
                else ->
                    throw IllegalStateException("Unexpected body type: $body")
            }
            val decodedData = Base64.decodeBase64(data)
            Biweekly.parse(decodedData.decodeToString()).first()
                ?: throw IllegalStateException("Failed parsing calendar")
        }.onFailure { Timber.e(it) }
    }

    override fun onHtmlSet(htmlText: String?) {
        messageContent = htmlText.orEmpty()
        if (::icalendar.isInitialized) {
            replaceMessageContentIfNeeded()
        }
    }

    private fun replaceMessageContentIfNeeded() {
        if (icalendar.events.isEmpty()) {
            return
        }
        val description = icalendar.events.first().description?.value
        if (
            !description.isNullOrBlank()
            && !messageContent.contains(description)
            && messageContent.length <= description.length
        ) {
            view.showDescription(description)
            view.hideMessageContent()
        }
    }
}
