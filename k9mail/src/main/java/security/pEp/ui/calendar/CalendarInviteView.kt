package security.pEp.ui.calendar

interface CalendarInviteView {
    fun showLoading()
    fun hideLoading()

    fun setSummary(summary: String)
    fun hideSummary()

    fun showDescription(description: String)

    fun hideMessageContent()

    fun setLocation(location: String)
    fun hideLocation()

    fun setStartAndEndTime(timeText: String)
    fun hideStartAndEndTime()

    fun setInvitees(invitees: String)
    fun hideInvitees()

    fun showCalendarIcon()
    fun hideCalendarIcon()

    fun showNoCalendarApp()
    fun showErrorScreen()
}
