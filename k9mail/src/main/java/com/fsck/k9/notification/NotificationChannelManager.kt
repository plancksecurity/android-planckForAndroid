package com.fsck.k9.notification


import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Named

class NotificationChannelManager @Inject constructor(@Named("AppContext") private val context: Context, private val preferences: Preferences) {

    private val notificationResourceProvider = PlanckNotificationResourceProvider(context)

    enum class ChannelType {
        MESSAGES, MISCELLANEOUS
    }

    fun updateChannels() {

        Executors.newSingleThreadExecutor().execute {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            val accounts = preferences.accounts

            removeChannelsForNonExistingOrChangedAccounts(notificationManager, accounts)
            addChannelsForAccounts(notificationManager, accounts)
        }

    }

    private fun addChannelsForAccounts(
            notificationManager: NotificationManager, accounts: List<Account>) {
        for (account in accounts) {
            val groupId = account.uuid
            val group = NotificationChannelGroup(groupId, displayName(account))

            val channelMessages = getChannelMessages(account)
            val channelMiscellaneous = getChannelMiscellaneous(account)

            notificationManager.createNotificationChannelGroup(group)
            notificationManager.createNotificationChannel(channelMessages)
            notificationManager.createNotificationChannel(channelMiscellaneous)
        }
    }

    private fun removeChannelsForNonExistingOrChangedAccounts(
            notificationManager: NotificationManager, accounts: List<Account>) {
        val existingAccounts = HashMap<String, Account>()
        for (account in accounts) {
            existingAccounts[account.uuid] = account
        }

        val groups = notificationManager.notificationChannelGroups
        for (group in groups) {
            val groupId = group.id

            var shouldDelete = false
            if (!existingAccounts.containsKey(groupId)) {
                shouldDelete = true
            } else if (displayName(existingAccounts[groupId]) != group.name.toString()) {
                // There is no way to change group names. Deleting group, so it is re-generated.
                shouldDelete = true
            }

            if (shouldDelete) {
                notificationManager.deleteNotificationChannelGroup(groupId)
            }
        }
    }

    private fun getChannelMessages(account: Account): NotificationChannel {
        val channelName = notificationResourceProvider.messagesChannelName
        val channelDescription = notificationResourceProvider.messagesChannelDescription
        val channelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channelGroupId = account.uuid

        val messagesChannel = NotificationChannel(channelId, channelName, importance)
        messagesChannel.description = channelDescription
        messagesChannel.group = channelGroupId

        return messagesChannel
    }

    private fun getChannelMiscellaneous(account: Account): NotificationChannel {
        val channelName = notificationResourceProvider.miscellaneousChannelName
        val channelDescription = notificationResourceProvider.miscellaneousChannelDescription
        val channelId = getChannelIdFor(account, ChannelType.MISCELLANEOUS)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channelGroupId = account.uuid

        val miscellaneousChannel = NotificationChannel(channelId, channelName, importance)
        miscellaneousChannel.description = channelDescription
        miscellaneousChannel.group = channelGroupId

        return miscellaneousChannel
    }

    fun getChannelIdFor(account: Account, channelType: ChannelType): String {
        val accountUuid = account.uuid
        return when {
            channelType == ChannelType.MESSAGES && !K9.isQuietTime() ->
                "messages_channel_$accountUuid"
            else ->
                "miscellaneous_channel_$accountUuid"
        }
    }

    fun displayName(account: Account?): String {
        return account?.name + " (" + account?.email + ")"
    }
}