package security.planck.ui.calendar

import android.content.Context
import android.content.Intent
import com.fsck.k9.R
import com.fsck.k9.mailstore.AttachmentViewInfo
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class CalendarInviteViewDelegateAndroid @Inject constructor(
    @ActivityContext private val context: Context,
    private val intentCreator: CalendarInviteIntentCreator,
) : CalendarInviteViewDelegate {

    override fun openCalendarApp(attachment: AttachmentViewInfo): Boolean {
        return launchCalendarIntent(intentCreator.getOpenCalendarIntent(context, attachment))
    }

    override fun getOrganizerTag(): String {
        return context.getString(R.string.organizer_tag)
    }

    private fun launchCalendarIntent(intent: Intent): Boolean {
        return if (intent.resolveActivity(context.applicationContext.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }
}
