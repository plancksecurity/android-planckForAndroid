package security.planck.serversettings

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.planck.infrastructure.exceptions.DeviceOfflineException
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServerSettingsCheckerTest {
    private val preferences: Preferences = mockk()
    private val controller: MessagingController = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val account: Account = mockk(relaxed = true)

    private val checker = ServerSettingsChecker(controller, preferences)

    @Before
    fun setUp() {
        mockkStatic(K9::class)
        every { K9.setServicesEnabled(any()) }.answers { }
        mockkStatic(Utility::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(Utility::class)
    }

    @Test
    fun `checkServerSettings() uses MessagingController to clear certificate error notifications`() {
        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            false
        )


        verify { controller.clearCertificateErrorNotifications(account, CheckDirection.INCOMING) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `checkServerSettings() uses MessagingController to check incoming server settings`() {
        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            false
        )


        verify { controller.checkIncomingServerSettings(account) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `checkServerSettings() uses MessagingController to check outgoing server settings`() {
        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.OUTGOING,
            false
        )


        verify { controller.checkOutgoingServerSettings(account) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `checkServerSettings() saves account when called with edit option`() {
        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        verify { account.save(preferences) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `checkServerSettings() restart services when called with edit option`() {
        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        verify { K9.setServicesEnabled(context) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `checkServerSettings() returns Result_Failure if an exception is thrown`() {
        every { controller.clearCertificateErrorNotifications(any(), any()) }
            .throws(IllegalStateException("test"))


        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        verify(exactly = 0) { K9.setServicesEnabled(context) }
        verify(exactly = 0) { controller.checkIncomingServerSettings(any()) }
        assertTrue(result.isFailure)
        val throwable = result.exceptionOrNull()!!
        assertTrue(throwable is IllegalStateException)
        assertEquals("test", throwable.message)
    }

    @Test
    fun `checkServerSettings() returns Result_Failure with AuthenticationFailedException as it is`() {
        every { controller.clearCertificateErrorNotifications(any(), any()) }
            .throws(AuthenticationFailedException("test"))


        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        assertTrue(result.isFailure)
        val throwable = result.exceptionOrNull()!!
        assertTrue(throwable is AuthenticationFailedException)
        assertEquals("test", throwable.message)
    }

    @Test
    fun `checkServerSettings() returns Result_Failure with CertificateValidationException as it is`() {
        every { controller.clearCertificateErrorNotifications(any(), any()) }
            .throws(CertificateValidationException("test"))


        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        assertTrue(result.isFailure)
        val throwable = result.exceptionOrNull()!!
        assertTrue(throwable is CertificateValidationException)
        assertEquals("test", throwable.message)
    }

    @Test
    fun `checkServerSettings() returns Result_Failure with DeviceOfflineException if a MessagingException is thrown and device has no connectivity`() {
        every { controller.clearCertificateErrorNotifications(any(), any()) }
            .throws(MessagingException("test"))
        every { Utility.hasConnectivity(any()) }.returns(false)


        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        assertTrue(result.isFailure)
        val throwable = result.exceptionOrNull()!!
        assertTrue(throwable is DeviceOfflineException)
    }

    @Test
    fun `checkServerSettings() returns Result_Failure with MessagingException if device has connectivity`() {
        every { controller.clearCertificateErrorNotifications(any(), any()) }
            .throws(MessagingException("test"))
        every { Utility.hasConnectivity(any()) }.returns(true)


        val result = checker.checkServerSettings(
            context,
            account,
            CheckDirection.INCOMING,
            true
        )


        assertTrue(result.isFailure)
        val throwable = result.exceptionOrNull()!!
        assertTrue(throwable is MessagingException)
        assertEquals("test", throwable.message)
    }
}