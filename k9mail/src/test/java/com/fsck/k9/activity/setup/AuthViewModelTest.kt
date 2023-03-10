package com.fsck.k9.activity.setup

import android.app.Application
import androidx.activity.result.ActivityResultRegistry
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.auth.JwtTokenDecoder
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.oauth.OAuthConfigurationProvider
import com.fsck.k9.pEp.infrastructure.livedata.Event
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.fsck.k9.pEp.ui.ConnectionSettings
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class AuthViewModelTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val app: Application = mockk()
    private val preferences: Preferences = mockk()
    private val oAuthConfigurationProvider: OAuthConfigurationProvider = mockk()
    private val jwtTokenDecoder: JwtTokenDecoder = mockk()
    private val discovery: ProvidersXmlDiscovery = mockk()
    private val viewModel = AuthViewModel(
        app,
        preferences,
        oAuthConfigurationProvider,
        jwtTokenDecoder,
        discovery,
        coroutinesTestRule.testDispatcherProvider
    )
    private val activityResultRegistry: ActivityResultRegistry = mockk()
    private val lifecycle: Lifecycle = mockk(relaxed = true)
    private val authState: AuthState = mockk()
    private val account: Account = mockk()

    private val receivedDiscoveredSettings = mutableListOf<Event<ConnectionSettings?>>()

    private fun getTestDiscoveryResults(
        username: String? = "",
        authType: AuthType? = AuthType.PLAIN,
        incomingEmpty: Boolean = false,
        outgoingEmpty: Boolean = false,
    ): DiscoveryResults {
        return DiscoveryResults(
            if (incomingEmpty) emptyList()
            else listOf(
                DiscoveredServerSettings(
                    ServerSettings.Type.IMAP,
                    "",
                    0,
                    ConnectionSecurity.NONE,
                    authType,
                    username
                )
            ),
            if (outgoingEmpty) emptyList()
            else listOf(
                DiscoveredServerSettings(
                    ServerSettings.Type.SMTP,
                    "",
                    0,
                    ConnectionSecurity.NONE,
                    authType,
                    username
                )
            )
        )
    }

    private val testConnectionSettings: ConnectionSettings = ConnectionSettings(
        incoming = ServerSettings(
            ServerSettings.Type.IMAP,
            "",
            0,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "",
            null,
            null
        ),
        outgoing = ServerSettings(
            ServerSettings.Type.SMTP,
            "",
            0,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "",
            null,
            null
        )
    )

    @Before
    fun setUp() {
        receivedDiscoveredSettings.clear()
        observeViewModel(viewModel)
        assertEquals(AuthFlowState.Idle, viewModel.uiState.value)
        assertFalse(viewModel.needsMailSettingsDiscovery)
        assertEquals(Event<ConnectionSettings?>(null, false), viewModel.connectionSettings.value)
        mockkStatic(AuthState::class)
        mockkStatic(RemoteStore::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(AuthState::class)
        unmockkStatic(RemoteStore::class)
    }

    @Test
    fun `isAuthorized returns false if Account's oAuthState is not authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(authState)
        every { authState.isAuthorized }.returns(false)
        every { account.oAuthState }.returns("authstate")


        assertFalse(viewModel.isAuthorized(account))


        verify { account.oAuthState }
        verify { AuthState.jsonDeserialize("authstate") }
    }

    @Test
    fun `isAuthorized returns true if Account's oAuthState is authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(authState)
        every { authState.isAuthorized }.returns(true)
        every { account.oAuthState }.returns("authstate")


        assertTrue(viewModel.isAuthorized(account))


        verify { account.oAuthState }
        verify { AuthState.jsonDeserialize("authstate") }
    }

    @Test
    fun `init() adds result observer to lifecycle`() {
        viewModel.init(activityResultRegistry, lifecycle)


        verify { lifecycle.addObserver(any()) }
    }

    @Test
    fun `isUsingGoogle returns true if Account's mandatoryProviderType is GOOGLE`() {
        every { account.mandatoryOAuthProviderType }.returns(OAuthProviderType.GOOGLE)


        assertTrue(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
    }

    @Test
    fun `isUsingGoogle returns false if Account's mandatoryProviderType is not GOOGLE`() {
        every { account.mandatoryOAuthProviderType }.returns(OAuthProviderType.MICROSOFT)


        assertFalse(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
    }

    @Test
    fun `isUsingGoogle returns true if Account's mandatoryProviderType is null and server settings host is Google`() {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns("storeUri")
        val settings = ServerSettings(
            ServerSettings.Type.IMAP,
            "testhost",
            0,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "",
            "",
            ""
        )
        every { RemoteStore.decodeStoreUri("storeUri") }.returns(settings)
        every { oAuthConfigurationProvider.isGoogle(settings.host) }.returns(true)


        assertTrue(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri("storeUri") }
        verify { oAuthConfigurationProvider.isGoogle(settings.host) }
    }

    @Test
    fun `isUsingGoogle returns false if Account's mandatoryProviderType is null and server settings host is not Google`() {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns("storeUri")
        val settings = ServerSettings(
            ServerSettings.Type.IMAP,
            "testhost",
            0,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "",
            "",
            ""
        )
        every { RemoteStore.decodeStoreUri("storeUri") }.returns(settings)
        every { oAuthConfigurationProvider.isGoogle(settings.host) }.returns(false)


        assertFalse(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri("storeUri") }
        verify { oAuthConfigurationProvider.isGoogle(settings.host) }
    }

    @Test
    fun `isUsingGoogle returns false if Account's mandatoryProviderType is null and server settings host is null`() {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns("storeUri")
        val settings = ServerSettings(
            ServerSettings.Type.IMAP,
            null,
            0,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            "",
            "",
            ""
        )
        every { RemoteStore.decodeStoreUri("storeUri") }.returns(settings)


        assertFalse(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri("storeUri") }
        verify { oAuthConfigurationProvider.wasNot(called) }
    }

    @Test
    fun `discoverMailSettingsAsync uses discovery to discover Account mail settings`() = runTest {
        coEvery { discovery.discover(any(), any()) }.returns(null)


        viewModel.discoverMailSettingsAsync("email", null)
        advanceUntilIdle()


        coVerify { discovery.discover("email", null) }
    }

    @Test
    fun `discoverMailSettingsAsync sets connectionSettings LiveData with discovery result`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults()
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(
                        testConnectionSettings,
                        true
                    )
                )
            )
        }

    @Test
    fun `ViewModel does not need discovery once discoverMailSettingsAsync is successful`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults()
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertFalse(viewModel.needsMailSettingsDiscovery)
        }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered settings are null`() = runTest {
        coEvery { discovery.discover(any(), any()) }.returns(null)


        viewModel.discoverMailSettingsAsync("email", null)
        advanceUntilIdle()


        coVerify { discovery.discover("email", null) }
        assertReceivedDiscoveredSettings(
            listOf(
                Event(null, false),
                Event(null, true)
            )
        )
    }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered incoming settings are empty`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults(incomingEmpty = true)
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(null, true)
                )
            )
        }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered outgoing settings are empty`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults(outgoingEmpty = true)
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(null, true)
                )
            )
        }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered settings username is null`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults(username = null)
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(null, true)
                )
            )
        }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered settings auth type is null`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults(authType = null)
            )


            viewModel.discoverMailSettingsAsync("email", null)
            advanceUntilIdle()


            coVerify { discovery.discover("email", null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(null, true)
                )
            )
        }

    private fun assertReceivedDiscoveredSettings(expected: List<Event<ConnectionSettings?>>) {
        assertEquals(expected.size, receivedDiscoveredSettings.size)
        receivedDiscoveredSettings.forEachIndexed() { index, event ->
            val expectedEvent = expected[index]
            assertEquals(expectedEvent.isReady, event.isReady)
            assertEquals(expectedEvent.hasBeenHandled, event.hasBeenHandled)
            val settings = event.peekContent()
            val expectedSettings = expectedEvent.peekContent()
            if (expectedSettings == null) {
                assertEquals(expectedEvent, event)
            } else {
                val incoming = settings!!.incoming
                val outgoing = settings.outgoing
                val expectedIncoming = expectedSettings.incoming
                val expectedOutgoing = expectedSettings.outgoing
                assertSettingsEqual(expectedIncoming, incoming)
                assertSettingsEqual(expectedOutgoing, outgoing)
            }
        }
    }

    private fun assertSettingsEqual(expected: ServerSettings, actual: ServerSettings) {
        assertEquals(expected.host, actual.host)
        assertEquals(expected.authenticationType, actual.authenticationType)
        assertEquals(expected.port, actual.port)
        assertEquals(expected.clientCertificateAlias, actual.clientCertificateAlias)
        assertEquals(expected.connectionSecurity, actual.connectionSecurity)
        assertEquals(expected.password, actual.password)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.username, actual.username)
    }

    private fun observeViewModel(viewModel: AuthViewModel) {
        viewModel.connectionSettings.observeForever {
            receivedDiscoveredSettings.add(it)
        }
    }

}