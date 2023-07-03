package com.fsck.k9.notification

import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

internal sealed interface NotificationContent<out Reference: NotificationReference> {
    val sender: String
    val subject: String
    val summary: CharSequence

    val reference: Reference
}

internal interface NotificationReference

internal data class NewMailNotificationContent constructor(
    override val sender: String,
    override val subject: String,
    val preview: CharSequence,
    override val summary: CharSequence,
    override val reference: MessageReference,
): NotificationContent<MessageReference>

internal data class GroupMailNotificationContent constructor(
    override val sender: String,
    override val subject: String,
    override val summary: CharSequence,
    override val reference: GroupMailInvite,
): NotificationContent<GroupMailInvite>


