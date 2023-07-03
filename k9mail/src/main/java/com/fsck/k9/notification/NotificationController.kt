package com.fsck.k9.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mail.Folder
import com.fsck.k9.mailstore.LocalMessage
import org.jetbrains.anko.coroutines.experimental.asReference
import security.planck.notification.GroupMailInvite

class NotificationController internal constructor(
    context: Context,
    notificationManager: NotificationManagerCompat
) {
    private val certificateErrorNotificationController: CertificateErrorNotificationController
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController
    private val syncNotificationController: SyncNotificationController
    private val sendFailedNotificationController: SendFailedNotificationController
    private val groupedNotificationController: GroupedNotificationController
    //private val groupMailNotificationController: GroupedNotificationController<GroupMailInvite, GroupMailNotificationContent>
    private val channelUtils: NotificationChannelManager

    init {
        channelUtils = NotificationChannelManager(context, Preferences.getPreferences(context))
        val notificationResourceProvider: NotificationResourceProvider =
            PlanckNotificationResourceProvider(context)
        val notificationHelper = NotificationHelper(
            context,
            notificationManager,
            channelUtils,
            notificationResourceProvider
        )
        val actionBuilder = NotificationActionCreator(context)
        certificateErrorNotificationController = CertificateErrorNotificationController(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider
        )
        authenticationErrorNotificationController = AuthenticationErrorNotificationController(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider
        )
        syncNotificationController = SyncNotificationController(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider
        )
        sendFailedNotificationController = SendFailedNotificationController(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider
        )
        groupedNotificationController = initializeGroupedNotificationController(
            context,
            notificationResourceProvider,
            notificationHelper,
            actionBuilder
        )
    }

    private fun initializeGroupedNotificationController(
        context: Context,
        notificationResourceProvider: NotificationResourceProvider,
        notificationHelper: NotificationHelper,
        actionBuilder: NotificationActionCreator
    ): GroupedNotificationController {
        val singleMessageNotificationDataCreator = SingleGroupedNotificationDataCreator()
        val summaryNotificationDataCreator =
            SummaryGroupedNotificationDataCreator(singleMessageNotificationDataCreator)
        val groupedNotificationManager  =
            GroupedNotificationManager(
                contentCreator = NotificationContentCreator(context, notificationResourceProvider),
                newMailNotificationRepository = NotificationRepository(NewMailNotificationDataStore()),
                groupMailNotificationRepository = NotificationRepository(GroupMailNotificationDataStore()),
                baseNotificationDataCreator = BaseNotificationDataCreator(),
                singleMessageNotificationDataCreator = singleMessageNotificationDataCreator,
                summaryNotificationDataCreator = summaryNotificationDataCreator,
                clock = Clock.INSTANCE
            )
        val lockScreenNotificationCreator = LockScreenNotificationCreator(
            notificationHelper, notificationResourceProvider
        )
        val singleNewMailNotificationCreator = SingleMessageNotificationCreator(
                notificationHelper,
                actionBuilder,
                notificationResourceProvider,
                lockScreenNotificationCreator
            )
        val singleGroupMailNotificationCreator = SingleGroupMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider,
            lockScreenNotificationCreator
        )
        val summaryNewMailNotificationCreator = SummaryNewMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            lockScreenNotificationCreator,
            singleNewMailNotificationCreator,
            notificationResourceProvider
        )
        val summaryGroupMailNotificationCreator = SummaryGroupMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            lockScreenNotificationCreator,
            singleGroupMailNotificationCreator,
            notificationResourceProvider
        )
        return GroupedNotificationController(
            notificationHelper = notificationHelper,
            groupedNotificationManager = groupedNotificationManager,
            newMailSummaryNotificationCreator = summaryNewMailNotificationCreator,
            newMailSingleNotificationCreator = singleNewMailNotificationCreator,
            groupMailSingleNotificationCreator = singleGroupMailNotificationCreator,
            groupMailSummaryNotificationCreator = summaryGroupMailNotificationCreator
        )
    }

    fun showCertificateErrorNotification(account: Account?, incoming: Boolean) {
        certificateErrorNotificationController.showCertificateErrorNotification(account!!, incoming)
    }

    fun clearCertificateErrorNotifications(account: Account?, incoming: Boolean) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(
            account!!,
            incoming
        )
    }

    fun showAuthenticationErrorNotification(account: Account?, incoming: Boolean) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(
            account!!,
            incoming
        )
    }

    fun clearAuthenticationErrorNotification(account: Account?, incoming: Boolean) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(
            account!!,
            incoming
        )
    }

    fun showSendingNotification(account: Account?) {
        syncNotificationController.showSendingNotification(account!!)
    }

    fun clearSendingNotification(account: Account?) {
        syncNotificationController.clearSendingNotification(account!!)
    }

    fun showSendFailedNotification(account: Account?, exception: Exception?) {
        sendFailedNotificationController.showSendFailedNotification(account!!, exception!!)
    }

    fun clearSendFailedNotification(account: Account?) {
        sendFailedNotificationController.clearSendFailedNotification(account!!)
    }

    fun showFetchingMailNotification(account: Account?, folder: Folder<*>?) {
        syncNotificationController.showFetchingMailNotification(account!!, folder!!)
    }

    fun clearFetchingMailNotification(account: Account?) {
        syncNotificationController.clearFetchingMailNotification(account!!)
    }

    fun addNewMailsNotification(
        account: Account,
        messages: List<LocalMessage>,
        previousUnreadMessageCount: Int
    ) {
        groupedNotificationController.addNewMailsNotification(account, messages)
    }

    fun clearNewMailNotifications(account: Account, folderName: String) {
        groupedNotificationController.removeNewMailNotifications(account) {
            it.filter { messageReference -> messageReference.folderName == folderName }
        }
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        groupedNotificationController.removeNewMailNotifications(account) {
            it.filter { reference -> reference == messageReference }
        }
    }

    fun clearNewMailNotifications(account: Account) {
        groupedNotificationController.clearNewMailNotifications(account)
    }

    fun addGroupMailNotification(account: Account, groupMailInvite: GroupMailInvite) {
        groupedNotificationController.addGroupMailNotification(
            account,
            groupMailInvite,
            false
        )
    }

    fun removeGroupMailNotification(account: Account, groupMailInvite: GroupMailInvite) {
        groupedNotificationController.removeGroupMailNotifications(account) {
            it.filter { reference -> reference == groupMailInvite }
        }
    }

    fun clearGroupMailNotifications(account: Account) {
        groupedNotificationController.clearGroupMailNotifications(account)
    }

    fun updateChannels() {
        channelUtils.updateChannels()
    }

    companion object {
        @JvmStatic
        fun newInstance(context: Context): NotificationController {
            val appContext = context.applicationContext
            val notificationManager = NotificationManagerCompat.from(appContext)
            return NotificationController(appContext, notificationManager)
        }
    }
}