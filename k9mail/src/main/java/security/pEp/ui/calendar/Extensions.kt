package security.pEp.ui.calendar

import biweekly.component.VEvent

internal fun VEvent.getInvitees(): List<String> {
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
