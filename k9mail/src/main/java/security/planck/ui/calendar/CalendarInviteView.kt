package security.planck.ui.calendar

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

    fun setShortInvitees(firstInvitee: String, rest: Int)
    fun setLongInvitees(invitees: String)
    fun hideInvitees()

    fun showCalendarIcon()
    fun hideCalendarIcon()

    fun showNoCalendarApp()
    fun showErrorScreen()
}
