package security.pEp.ui.calendar

import com.fsck.k9.mailstore.AttachmentViewInfo

interface CalendarInviteViewDelegate {
    fun openCalendarApp(attachment: AttachmentViewInfo): Boolean

    fun getOrganizerTag(): String
}
