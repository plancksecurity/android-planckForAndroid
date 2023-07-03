package security.planck.notification


import com.fsck.k9.notification.NotificationReference

data class GroupMailInvite(
    val groupAddress: String,
    val senderAddress: String,
    val accountUuid: String,
): NotificationReference