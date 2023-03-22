package security.pEp.ui.calendar

import android.content.Context
import android.content.Intent
import com.fsck.k9.R
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.pEp.launchIntent
import javax.inject.Inject
import javax.inject.Named

class CalendarInviteViewDelegateAndroid @Inject constructor(
    @Named("ActivityContext") private val context: Context,
    private val intentCreator: CalendarInviteIntentCreator,
) : CalendarInviteViewDelegate {

    override fun openCalendarApp(attachment: AttachmentViewInfo): Boolean {
        return launchCalendarIntent(intentCreator.getOpenCalendarIntent(context, attachment))
    }

    override fun getOrganizerTag(): String {
        return context.getString(R.string.organizer_tag)
    }

    private fun launchCalendarIntent(intent: Intent): Boolean {
        return context.launchIntent(intent)
    }
}
