package security.pEp.ui.calendar

import biweekly.Biweekly
import biweekly.ICalendar
import java.text.SimpleDateFormat
import java.util.*

object TestICalendarCreator {
    fun getMacosCalendarInvite(
        start: Date = Date(Date().time + ONE_HOUR),
        end: Date = Date(start.time + ONE_HOUR),
    ): ICalendar {
        val dateFormatter = getDateFormatter()
        val calendarText = """
                BEGIN:VCALENDAR
                METHOD:REQUEST
                PRODID:Microsoft Exchange Server 2010
                VERSION:2.0
                BEGIN:VTIMEZONE
                TZID:Romance Standard Time
                BEGIN:STANDARD
                DTSTART:16010101T030000
                TZOFFSETFROM:+0200
                TZOFFSETTO:+0100
                RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10
                END:STANDARD
                BEGIN:DAYLIGHT
                DTSTART:16010101T020000
                TZOFFSETFROM:+0100
                TZOFFSETTO:+0200
                RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3
                END:DAYLIGHT
                END:VTIMEZONE
                BEGIN:VEVENT
                ORGANIZER;CN=$ORGANIZER_NAME:MAILTO:$ORGANIZER_EMAIL
                ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=$INVITEE_1_NAME
                 :MAILTO:$INVITEE_1_EMAIL
                ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=$INVITEE_2_NAME
                 :MAILTO:$INVITEE_2_EMAIL
                DESCRIPTION;LANGUAGE=en-US:$EVENT_DESCRIPTION
                UID:$MACOS_EVENT_UID
                SUMMARY;LANGUAGE=en-US:$EVENT_SUMMARY
                DTSTART;TZID=Romance Standard Time:${dateFormatter.format(start)}
                DTEND;TZID=Romance Standard Time:${dateFormatter.format(end)}
                CLASS:PUBLIC
                PRIORITY:5
                DTSTAMP:20211112T035710Z
                TRANSP:OPAQUE
                STATUS:CONFIRMED
                SEQUENCE:0
                LOCATION;LANGUAGE=en-US:$EVENT_LOCATION
                X-MICROSOFT-CDO-APPT-SEQUENCE:0
                X-MICROSOFT-CDO-OWNERAPPTID:2119918498
                X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE
                X-MICROSOFT-CDO-INTENDEDSTATUS:BUSY
                X-MICROSOFT-CDO-ALLDAYEVENT:FALSE
                X-MICROSOFT-CDO-IMPORTANCE:1
                X-MICROSOFT-CDO-INSTTYPE:0
                X-MICROSOFT-DONOTFORWARDMEETING:FALSE
                X-MICROSOFT-DISALLOW-COUNTER:FALSE
                END:VEVENT
                END:VCALENDAR
                
            """.trimIndent()
        return Biweekly.parse(calendarText).first()
    }

    fun getGoogleCalendarRequest(
        start: Date = Date(Date().time + ONE_HOUR),
        end: Date = Date(start.time + ONE_HOUR),
    ): ICalendar {
        val dateFormatter = getDateFormatter().apply { timeZone = TimeZone.getTimeZone("UTC") }
        val calendarText = """
                BEGIN:VCALENDAR
                PRODID:-//Google Inc//Google Calendar 70.9054//EN
                VERSION:2.0
                CALSCALE:GREGORIAN
                METHOD:REQUEST
                BEGIN:VEVENT
                DTSTART:${dateFormatter.format(start)}Z
                DTEND:${dateFormatter.format(end)}Z
                DTSTAMP:20211111T170002Z
                ORGANIZER;CN=$ORGANIZER_NAME:mailto:$ORGANIZER_EMAIL
                UID:$GOOGLE_EVENT_UID
                ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=
                 TRUE;CN=$INVITEE_1_NAME;X-NUM-GUESTS=0:mailto:$INVITEE_1_EMAIL
                ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=
                 TRUE;CN=$INVITEE_2_NAME;X-NUM-GUESTS=0:mailto:$INVITEE_2_EMAIL
                ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;RSVP=TRUE
                 ;CN=$ORGANIZER_NAME;X-NUM-GUESTS=0:mailto:$ORGANIZER_EMAIL
                X-MICROSOFT-CDO-OWNERAPPTID:-54088282
                CREATED:20211111T170001Z
                DESCRIPTION:$EVENT_DESCRIPTION
                LAST-MODIFIED:20211111T170001Z
                LOCATION:$EVENT_LOCATION
                SEQUENCE:0
                STATUS:CONFIRMED
                SUMMARY:$EVENT_SUMMARY
                TRANSP:OPAQUE
                END:VEVENT
                END:VCALENDAR
                            
            """.trimIndent()
        return Biweekly.parse(calendarText).first()
    }

    fun getMacosRecurrentInvite(
        start: Date = Date(Date().time + ONE_HOUR),
        end: Date = Date(start.time + ONE_HOUR),
        rrule: String = "",
    ): ICalendar {
        val dateFormatter = getDateFormatter()
        val calendarText = """
            BEGIN:VCALENDAR
            METHOD:REQUEST
            PRODID:Microsoft Exchange Server 2010
            VERSION:2.0
            BEGIN:VTIMEZONE
            TZID:Romance Standard Time
            BEGIN:STANDARD
            DTSTART:16010101T030000
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10
            END:STANDARD
            BEGIN:DAYLIGHT
            DTSTART:16010101T020000
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3
            END:DAYLIGHT
            END:VTIMEZONE
            BEGIN:VEVENT
            ORGANIZER;CN=$ORGANIZER_NAME:MAILTO:$ORGANIZER_EMAIL
            ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=$INVITEE_1_NAME
             :MAILTO:$INVITEE_1_EMAIL
            ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=$INVITEE_2_NAME
             :MAILTO:$INVITEE_2_EMAIL
            ${if (rrule.isBlank()) "" else "RRULE:$rrule"}
            DESCRIPTION;LANGUAGE=en-US:$EVENT_DESCRIPTION
            UID:$MACOS_EVENT_UID
            SUMMARY;LANGUAGE=en-US:$EVENT_SUMMARY
            DTSTART;TZID=Romance Standard Time:${dateFormatter.format(start)}
            DTEND;TZID=Romance Standard Time:${dateFormatter.format(end)}
            CLASS:PUBLIC
            PRIORITY:5
            DTSTAMP:20211122T153726Z
            TRANSP:OPAQUE
            STATUS:CONFIRMED
            SEQUENCE:0
            LOCATION;LANGUAGE=en-US:$EVENT_LOCATION
            X-MICROSOFT-CDO-APPT-SEQUENCE:0
            X-MICROSOFT-CDO-OWNERAPPTID:2119938403
            X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE
            X-MICROSOFT-CDO-INTENDEDSTATUS:BUSY
            X-MICROSOFT-CDO-ALLDAYEVENT:FALSE
            X-MICROSOFT-CDO-IMPORTANCE:1
            X-MICROSOFT-CDO-INSTTYPE:1
            X-MICROSOFT-DONOTFORWARDMEETING:FALSE
            X-MICROSOFT-DISALLOW-COUNTER:FALSE
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        return Biweekly.parse(calendarText).first()
    }

    fun getGoogleRecurrentInvite(
        start: Date = Date(Date().time + ONE_HOUR),
        end: Date = Date(start.time + ONE_HOUR),
        rrule: String = "",
    ): ICalendar {
        val dateFormatter = getDateFormatter()
        val calendarText = """
            BEGIN:VCALENDAR
            PRODID:-//Google Inc//Google Calendar 70.9054//EN
            VERSION:2.0
            CALSCALE:GREGORIAN
            METHOD:REQUEST
            BEGIN:VTIMEZONE
            TZID:Europe/Madrid
            X-LIC-LOCATION:Europe/Madrid
            BEGIN:DAYLIGHT
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            TZNAME:CEST
            DTSTART:19700329T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
            END:DAYLIGHT
            BEGIN:STANDARD
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            TZNAME:CET
            DTSTART:19701025T030000
            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VEVENT
            DTSTART;TZID=Europe/Madrid:${dateFormatter.format(start)}
            DTEND;TZID=Europe/Madrid:${dateFormatter.format(end)}
            ${if (rrule.isBlank()) "" else "RRULE:$rrule"}
            DTSTAMP:20211122T162300Z
            ORGANIZER;CN=$ORGANIZER_NAME:mailto:$ORGANIZER_EMAIL
            UID:$GOOGLE_EVENT_UID
            ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=
             TRUE;CN=$INVITEE_1_NAME;X-NUM-GUESTS=0:mailto:$INVITEE_1_EMAIL
            ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=
             TRUE;CN=$INVITEE_2_NAME;X-NUM-GUESTS=0:mailto:$INVITEE_2_EMAIL
            ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;RSVP=TRUE
             ;CN=$ORGANIZER_NAME;X-NUM-GUESTS=0:mailto:$ORGANIZER_EMAIL
            X-MICROSOFT-CDO-OWNERAPPTID:-1994912402
            CREATED:20211122T162258Z
            DESCRIPTION:$EVENT_DESCRIPTION
            LAST-MODIFIED:20211122T162258Z
            LOCATION:$EVENT_LOCATION
            SEQUENCE:0
            STATUS:CONFIRMED
            SUMMARY:$EVENT_SUMMARY
            TRANSP:OPAQUE
            END:VEVENT
            END:VCALENDAR

        """.trimIndent()
        return Biweekly.parse(calendarText).first()
    }

    fun getDateFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())

    private const val ONE_HOUR = 60 * 60 * 1000L
    const val ORGANIZER_NAME = "organizer"
    const val ORGANIZER_EMAIL = "organizer@dev.test"
    const val INVITEE_1_NAME = "invitee1"
    const val INVITEE_1_EMAIL = "invitee1@dev.test"
    const val INVITEE_2_NAME = "invitee2"
    const val INVITEE_2_EMAIL = "invitee2@dev.test"
    const val EVENT_LOCATION = "Plaza de Cataluña, Barcelona"
    const val EVENT_SUMMARY = "This is a test event summary."
    const val EVENT_DESCRIPTION = "This is a test event description."
    const val MACOS_EVENT_UID = "47701D4F-F6D8-4E81-817F-B5462BB0C5FD"
    const val GOOGLE_EVENT_UID = "6lim8o9p69ij2bb56op3ib9kc4s3abb1ccq3ib9j6co6ac3461gmadpp74@google.com"
}