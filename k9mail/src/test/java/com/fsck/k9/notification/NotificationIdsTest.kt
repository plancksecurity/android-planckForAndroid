package com.fsck.k9.notification

import com.fsck.k9.Account
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NotificationIdsTest {

    @Test
    fun `all notification IDs of an account are unique`() {
        val account = createAccount(0)

        val notificationIds = getAccountNotificationIds(account)

        assertThat(notificationIds).containsNoDuplicates()
    }

    @Test
    fun `notification IDs of adjacent accounts do not overlap`() {
        val account1 = createAccount(0)
        val account2 = createAccount(1)

        val notificationIds1 = getAccountNotificationIds(account1)
        val notificationIds2 = getAccountNotificationIds(account2)

        assertWithMessage("Reused notification IDs").that(notificationIds1 intersect notificationIds2)
            .isEmpty()
    }

    @Test
    // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
    fun `no gaps in notification IDs of an account`() {
        val account = createAccount(0)

        val notificationIds = getAccountNotificationIds(account)

        val minNotificationId = requireNotNull(notificationIds.minOrNull())
        val maxNotificationId = requireNotNull(notificationIds.maxOrNull())
        val notificationIdRange = (minNotificationId..maxNotificationId)
        assertWithMessage("Skipped notification IDs").that(notificationIdRange - notificationIds)
            .isEmpty()
    }

    @Test
    // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
    fun `no gap between notification IDs of adjacent accounts`() {
        val account1 = createAccount(1)
        val account2 = createAccount(2)

        val notificationIds1 = getAccountNotificationIds(account1)
        val notificationIds2 = getAccountNotificationIds(account2)

        val maxNotificationId1 = requireNotNull(notificationIds1.maxOrNull())
        val minNotificationId2 = requireNotNull(notificationIds2.minOrNull())
        assertThat(maxNotificationId1 + 1).isEqualTo(minNotificationId2)
    }

    @Test
    fun `all message notification IDs`() {
        val account = createAccount(1)

        val notificationIds =
            NotificationIds.getAllGroupedNotificationIds(account, NotificationGroupType.NEW_MAIL)

        assertThat(notificationIds).containsExactlyElementsIn(
            getNewMailNotificationIds(account) + NotificationIds.getSummaryGroupedNotificationId(
                account,
                NotificationGroupType.NEW_MAIL
            )
        )
    }

    private fun getAccountNotificationIds(account: Account): List<Int> {
        return listOf(
            NotificationIds.getSendFailedNotificationId(account),
            NotificationIds.getCertificateErrorNotificationId(account, true),
            NotificationIds.getCertificateErrorNotificationId(account, false),
            NotificationIds.getAuthenticationErrorNotificationId(account, true),
            NotificationIds.getAuthenticationErrorNotificationId(account, false),
            NotificationIds.getFetchingMailNotificationId(account),
            NotificationIds.getSummaryGroupedNotificationId(
                account,
                NotificationGroupType.NEW_MAIL
            ),
            NotificationIds.getSummaryGroupedNotificationId(
                account,
                NotificationGroupType.GROUP_MAIL
            ),
        ) + getGroupedNotificationIds(account)
    }

    private fun getGroupedNotificationIds(account: Account): List<Int> {
        return getNewMailNotificationIds(account) + getGroupMailNotificationIds(account)
    }

    private fun getNewMailNotificationIds(account: Account): List<Int> {
        return (0 until MAX_NUMBER_OF_STACKED_NOTIFICATIONS).map { index ->
            NotificationIds.getSingleGroupedNotificationId(
                account,
                index,
                NotificationGroupType.NEW_MAIL
            )
        }
    }

    private fun getGroupMailNotificationIds(account: Account): List<Int> {
        return (0 until MAX_NUMBER_OF_STACKED_NOTIFICATIONS).map { index ->
            NotificationIds.getSingleGroupedNotificationId(
                account,
                index,
                NotificationGroupType.GROUP_MAIL
            )
        }
    }

    private fun createAccount(accountNumber: Int): Account {
        return mock {
            on { this.accountNumber } doReturn accountNumber
        }
    }
}
