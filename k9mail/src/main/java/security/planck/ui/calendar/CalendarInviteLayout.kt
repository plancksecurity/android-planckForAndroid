package security.planck.ui.calendar

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import com.fsck.k9.R
import com.fsck.k9.activity.MessageList
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.pEp.PepActivity
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.view.MessageWebView
import javax.inject.Inject

private const val ARROW_ORIGINAL_ROTATION = 0f
private const val ARROW_FINAL_ROTATION = 180f
private const val ARROW_ROTATION_TIME = 200L

class CalendarInviteLayout(
    context: Context,
    attrs: AttributeSet?
) : FrameLayout(context, attrs), CalendarInviteView {
    private lateinit var openCalendarButton: ImageButton
    private lateinit var eventSummaryText: TextView
    private lateinit var eventDescriptionText: TextView
    private lateinit var messageContent: MessageWebView
    private lateinit var eventLocationText: TextView
    private lateinit var eventTimeText: TextView
    private lateinit var eventInviteesText: TextSwitcher
    private lateinit var showAllInviteesButton: ImageButton
    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var layout: View

    private val messageListRootView: View
        get() = (context as MessageList).rootView

    @Inject
    lateinit var presenter: CalendarInvitePresenter

    @Inject
    lateinit var viewDelegate: CalendarInviteViewDelegateAndroid

    init {
        inflate(context, R.layout.calendar_invite, this)
        setupViews()
        initializeInjector()
    }

    fun initialize(
        calendarAttachment: AttachmentViewInfo,
        messageViewInfo: MessageViewInfo
    ) {
        messageContent.setOnHtmlSetListener(presenter)
        presenter.initialize(this, viewDelegate, calendarAttachment, messageViewInfo)
    }

    override fun showLoading() {
        layout.visibility = View.INVISIBLE
        progressBar.show()
    }

    override fun hideLoading() {
        layout.visibility = View.VISIBLE
        progressBar.hide()
    }

    override fun setSummary(summary: String) {
        eventSummaryText.text = summary
    }

    override fun hideSummary() {
        eventSummaryText.visibility = View.INVISIBLE
    }

    override fun showDescription(description: String) {
        eventDescriptionText.visibility = View.VISIBLE
        eventDescriptionText.text = description
    }

    override fun hideMessageContent() {
        messageContent.visibility = View.GONE
    }

    override fun setLocation(location: String) {
        eventLocationText.text = location
    }

    override fun hideLocation() {
        eventLocationText.visibility = View.GONE
    }

    override fun setStartAndEndTime(timeText: String) {
        eventTimeText.text = timeText
    }

    override fun hideStartAndEndTime() {
        eventTimeText.visibility = View.GONE
    }

    override fun setShortInvitees(firstInvitee: String, rest: Int) {
        showAllInviteesButton.setOnClickListener(null)
        eventInviteesText.setText(
            context.getString(
                R.string.calendar_invite_short_invitees, firstInvitee, rest
            )
        )
        showAllInviteesButton.animate().rotation(
            ARROW_ORIGINAL_ROTATION
        ).withEndAction {
            showAllInviteesButton.setOnClickListener {
                presenter.showLongInvitees()
            }
        }.duration = ARROW_ROTATION_TIME
    }

    override fun setLongInvitees(invitees: String) {
        showAllInviteesButton.setOnClickListener(null)
        eventInviteesText.setText(invitees)
        showAllInviteesButton.animate().rotation(
            ARROW_FINAL_ROTATION
        ).withEndAction {
            showAllInviteesButton.setOnClickListener {
                presenter.showShortInvitees()
            }
        }.duration = ARROW_ROTATION_TIME
    }

    override fun hideInvitees() {
        eventInviteesText.visibility = View.GONE
        showAllInviteesButton.visibility = View.GONE
    }

    override fun showCalendarIcon() {
        openCalendarButton.visibility = View.VISIBLE
    }

    override fun hideCalendarIcon() {
        openCalendarButton.visibility = View.GONE
    }

    override fun showErrorScreen() {
        hideCalendarIcon()
        eventSummaryText.setText(R.string.calendar_invite_error_title)
        eventLocationText.visibility = View.GONE
        eventTimeText.visibility = View.GONE
        eventInviteesText.visibility = View.GONE
        showAllInviteesButton.visibility = View.GONE
    }

    override fun showNoCalendarApp() {
        FeedbackTools.showLongFeedback(
            messageListRootView,
            context.getString(R.string.no_calendar_app_found_msg)
        )
    }

    private fun setupViews() {
        openCalendarButton = findViewById(R.id.openCalendarImg)
        eventSummaryText = findViewById(R.id.eventSummary)
        eventDescriptionText = findViewById(R.id.eventDescription)
        messageContent = findViewById(R.id.message_content)
        eventLocationText = findViewById(R.id.eventLocation)
        eventTimeText = findViewById(R.id.eventTime)
        eventInviteesText = findViewById(R.id.eventInvitees)
        showAllInviteesButton = findViewById(R.id.showAllInvitees)
        progressBar = findViewById(R.id.calendarInviteProgressBar)
        layout = findViewById(R.id.calendarInviteLayout)

        openCalendarButton.setOnClickListener {
            presenter.openCalendar()
        }
        eventInviteesText.setInAnimation(context, android.R.anim.slide_in_left)
        eventInviteesText.setOutAnimation(context, android.R.anim.slide_out_right)


        layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    private fun initializeInjector() {
        (context as PepActivity).getpEpComponent().inject(this)
    }
}
