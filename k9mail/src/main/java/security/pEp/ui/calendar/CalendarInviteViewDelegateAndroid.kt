package security.pEp.ui.calendar

import android.content.Context
import android.content.Intent
import com.fsck.k9.mailstore.AttachmentViewInfo
import javax.inject.Inject
import javax.inject.Named

class CalendarInviteViewDelegateAndroid @Inject constructor(
    @Named("ActivityContext") private val context: Context,
    private val intentCreator: CalendarInviteIntentCreator,
) : CalendarInviteViewDelegate {

    override fun openCalendarApp(attachment: AttachmentViewInfo): Boolean {
        return launchCalendarIntent(intentCreator.getOpenCalendarIntent(context, attachment))
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
