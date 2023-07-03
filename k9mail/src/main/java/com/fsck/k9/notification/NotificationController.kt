package com.fsck.k9.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mail.Folder
import com.fsck.k9.mailstore.LocalMessage
import security.planck.notification.GroupMailInvite

class NotificationController internal constructor(
    context: Context,
    notificationManager: NotificationManagerCompat
) {
    private val certificateErrorNotificationController: CertificateErrorNotificationController
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController
    private val syncNotificationController: SyncNotificationController
    private val sendFailedNotificationController: SendFailedNotificationController
    private val newMailNotificationController: GroupedNotificationController<MessageReference, NewMailNotificationContent>
    private val groupMailNotificationController: GroupedNotificationController<GroupMailInvite, GroupMailNotificationContent>
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
        newMailNotificationController = initializeNewMailNotificationController(
            context,
            notificationResourceProvider,
            notificationHelper,
            actionBuilder
        )
        groupMailNotificationController = initializeGroupMailNotificationController(
            context,
            notificationResourceProvider,
            notificationHelper,
            actionBuilder
        )
    }

    private fun initializeGroupMailNotificationController(
        context: Context,
        notificationResourceProvider: NotificationResourceProvider,
        notificationHelper: NotificationHelper,
        actionBuilder: NotificationActionCreator
    ): GroupedNotificationController<GroupMailInvite, GroupMailNotificationContent> {
        val notificationContentCreator =
            NotificationContentCreator(context, notificationResourceProvider)
        val notificationRepository = NotificationRepository<GroupMailInvite, GroupMailNotificationContent>()
        val baseNotificationDataCreator = BaseNotificationDataCreator<GroupMailInvite, GroupMailNotificationContent>()
        val singleMessageNotificationDataCreator = SingleGroupedNotificationDataCreator<GroupMailInvite, GroupMailNotificationContent>()
        val summaryNotificationDataCreator =
            SummaryGroupedNotificationDataCreator(singleMessageNotificationDataCreator)
        val notificationManager = GroupMailNotificationManager(
            notificationContentCreator,
            notificationRepository,
            baseNotificationDataCreator,
            singleMessageNotificationDataCreator,
            summaryNotificationDataCreator,
            Clock.INSTANCE
        )
        val lockScreenNotificationCreator = LockScreenNotificationCreator(
            notificationHelper, notificationResourceProvider
        )
        val singleNotificationCreator = SingleGroupMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            notificationResourceProvider,
            lockScreenNotificationCreator
        )
        val summaryNotificationCreator = SummaryGroupMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            lockScreenNotificationCreator,
            singleNotificationCreator,
            notificationResourceProvider
        )
        return GroupedNotificationController(
            notificationHelper,
            notificationManager,
            summaryNotificationCreator,
            singleNotificationCreator
        )
    }

    private fun initializeNewMailNotificationController(
        context: Context,
        notificationResourceProvider: NotificationResourceProvider,
        notificationHelper: NotificationHelper,
        actionBuilder: NotificationActionCreator
    ): GroupedNotificationController<MessageReference, NewMailNotificationContent> {
        val notificationContentCreator =
            NotificationContentCreator(context, notificationResourceProvider)
        val notificationRepository: NotificationRepository<MessageReference, NewMailNotificationContent> =
            NotificationRepository()
        val baseNotificationDataCreator = BaseNotificationDataCreator<MessageReference, NewMailNotificationContent>()
        val singleMessageNotificationDataCreator = SingleGroupedNotificationDataCreator<MessageReference, NewMailNotificationContent>()
        val summaryNotificationDataCreator =
            SummaryGroupedNotificationDataCreator(singleMessageNotificationDataCreator)
        val newMailNotificationManager  =
            NewMailNotificationManager(
                notificationContentCreator,
                notificationRepository,
                baseNotificationDataCreator,
                singleMessageNotificationDataCreator,
                summaryNotificationDataCreator,
                Clock.INSTANCE
            )
        val lockScreenNotificationCreator = LockScreenNotificationCreator(
            notificationHelper, notificationResourceProvider
        )
        val singleNotificationCreator = SingleMessageNotificationCreator(
                notificationHelper,
                actionBuilder,
                notificationResourceProvider,
                lockScreenNotificationCreator
            )
        val summaryNotificationCreator = SummaryNewMailNotificationCreator(
            notificationHelper,
            actionBuilder,
            lockScreenNotificationCreator,
            singleNotificationCreator,
            notificationResourceProvider
        )
        return GroupedNotificationController(
            notificationHelper,
            newMailNotificationManager,
            summaryNotificationCreator,
            singleNotificationCreator
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
        newMailNotificationController.addNewMailsNotification(account, messages)
    }

    fun clearNewMailNotifications(account: Account, folderName: String) {
        newMailNotificationController.removeNewMailNotifications(account) {
            it.filter { messageReference -> messageReference.folderName == folderName }
        }
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        newMailNotificationController.removeNewMailNotifications(account) {
            it.filter { reference -> reference == messageReference }
        }
    }

    fun clearNewMailNotifications(account: Account?) {
        newMailNotificationController.clearNewMailNotifications(account!!)
    }

    fun addGroupMailNotification(account: Account, groupMailInvite: GroupMailInvite) {
        groupMailNotificationController.addNewMailNotification(
            account,
            groupMailInvite,
            false
        )
    }

    fun removeGroupMailNotification(account: Account?, groupMailInvite: GroupMailInvite?) {
        groupMailNotificationController.removeGroupMailNotification(account!!, groupMailInvite!!)
    }

    fun clearGroupMailNotifications(account: Account?) {
        groupMailNotificationController.clearGroupMailNotifications(account!!)
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