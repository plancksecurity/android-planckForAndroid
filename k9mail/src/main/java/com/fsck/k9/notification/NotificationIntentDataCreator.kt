package com.fsck.k9.notification

import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.RealClock
import com.fsck.k9.activity.MessageReference
import security.planck.notification.GroupMailInvite

class NotificationIntentDataCreator(
    private val clock: Clock = RealClock()
) {
    fun getDismissAllMessagesData(account: Account): Uri {
        return Uri.parse("data:,dismissAllMessage/${account.uuid}/${clock.time}")
    }

    fun getDismissMessageData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,dismissMessage/${messageReference.toIdentityString()}")
    }

    fun getDismissAllGroupMailData(account: Account): Uri {
        return Uri.parse("data:,dismissAllGroupMail/${account.uuid}/${clock.time}")
    }

    fun getDismissGroupMailData(groupMailInvite: GroupMailInvite): Uri {
        return Uri.parse("data:,dismissGroupMail/${groupMailInvite}")
    }

    fun getReplyMessageData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,reply/${messageReference.toIdentityString()}")
    }

    fun getMarkMessageAsReadData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,markAsRead/${messageReference.toIdentityString()}")
    }

    fun getMarkAllMessagesAsReadData(account: Account): Uri {
        return Uri.parse("data:,markAllAsRead/${account.uuid}/${clock.time}")
    }

    fun getDeleteMessageData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,delete/${messageReference.toIdentityString()}")
    }

    fun getDeleteMessageConfirmationData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,deleteConfirmation/${messageReference.toIdentityString()}")
    }

    fun getDeleteAllMessagesData(account: Account): Uri {
        return Uri.parse("data:,deleteAllMessages/${account.uuid}/${clock.time}")
    }

    fun getDeleteAllMessageConfirmationData(): Uri {
        return Uri.parse("data:,deleteAllMessagesConfirmation/${clock.time}")
    }

    fun getArchiveMessageData(messageReference: MessageReference): Uri {
        return Uri.parse("data:,archive/${messageReference.toIdentityString()}")
    }

    fun getArchiveAllMessagesData(account: Account): Uri {
        return Uri.parse("data:,archiveAll/${account.uuid}/${System.currentTimeMillis()}")
    }
}