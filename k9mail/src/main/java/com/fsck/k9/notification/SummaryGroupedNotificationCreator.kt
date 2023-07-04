package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import timber.log.Timber
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal abstract class SummaryGroupedNotificationCreator<in Reference : NotificationReference>(
    protected val notificationHelper: NotificationHelper,
    protected val actionCreator: NotificationActionCreator,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator,
    private val singleNotificationCreator: SingleGroupedNotificationCreator<Reference>,
    protected val resourceProvider: NotificationResourceProvider
) {
    fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData<Reference>
    ) {
        when (summaryNotificationData) {
            is SummaryInboxNotificationData -> {
                createInboxStyleSummaryNotification(baseNotificationData, summaryNotificationData)
            }

            is SummarySingleNotificationData<Reference> -> {
                createSingleMessageNotification(
                    baseNotificationData,
                    summaryNotificationData
                )
            }
        }
    }

    private fun createSingleMessageNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SummarySingleNotificationData<Reference>
    ) {
        singleNotificationCreator.createSingleNotification(
            baseNotificationData,
            singleNotificationData.singleNotificationData,
            isGroupSummary = true
        )
    }

    private fun createInboxStyleSummaryNotification(
        baseNotificationData: BaseNotificationData,
        notificationData: SummaryInboxNotificationData<Reference>
    ) {
        val account = baseNotificationData.account
        val accountName = baseNotificationData.accountName
        val summary = buildInboxSummaryText(baseNotificationData.accountName, notificationData)

        val notification =
            getNotificationBuilder(account, notificationData, baseNotificationData, summary)
                .setGroup(baseNotificationData.groupKey)
                .setGroupSummary(true)
                .setColor(baseNotificationData.color)
                .setWhen(notificationData.timestamp)
                .setNumber(notificationData.nonVisibleNotificationsCount)
                .setTicker(notificationData.content.firstOrNull())
                .setSubText(accountName)
                .setAppearance(notificationData.isSilent, baseNotificationData.appearance)
                .setLockScreenNotification(baseNotificationData)
                .build()

        Timber.v(
            "Creating inbox-style summary notification (silent=%b): %s",
            notificationData.isSilent,
            notification
        )
        notificationHelper.notify(account, notificationData.notificationId, notification)
    }

    protected abstract fun getNotificationBuilder(
        account: Account,
        notificationData: SummaryInboxNotificationData<Reference>,
        baseNotificationData: BaseNotificationData,
        summary: String,
    ): NotificationCompat.Builder

    private fun buildInboxSummaryText(
        accountName: String,
        notificationData: SummaryInboxNotificationData<Reference>
    ): String {
        return if (notificationData.nonVisibleNotificationsCount > 0) {
            getInboxSummaryText(accountName, notificationData.nonVisibleNotificationsCount)
        } else {
            accountName
        }
    }

    protected abstract fun getInboxSummaryText(accountName: String, count: Int): String

    protected fun NotificationBuilder.setInboxStyle(
        title: String,
        summary: String,
        contentLines: List<CharSequence>
    ) = apply {
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summary)

        for (line in contentLines) {
            style.addLine(line)
        }

        setStyle(style)
    }

    private fun NotificationBuilder.setLockScreenNotification(notificationData: BaseNotificationData) =
        apply {
            lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
        }
}
