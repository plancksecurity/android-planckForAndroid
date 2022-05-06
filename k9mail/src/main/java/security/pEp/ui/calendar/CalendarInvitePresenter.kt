package security.pEp.ui.calendar

import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.MessageViewInfo
import javax.inject.Inject

class CalendarInvitePresenter @Inject constructor() {

    private lateinit var view: CalendarInviteView
    private lateinit var messageViewInfo: MessageViewInfo
    private lateinit var calendarAttachment: AttachmentViewInfo

    fun initialize(
        view: CalendarInviteView,
        calendarAttachment: AttachmentViewInfo,
        messageViewInfo: MessageViewInfo,
    ) {
        this.view = view
        this.messageViewInfo = messageViewInfo
        this.calendarAttachment = calendarAttachment
    }

    fun openCalendarButtonClicked() {

    }
}
