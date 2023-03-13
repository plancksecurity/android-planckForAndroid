package com.fsck.k9.activity.setup

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.auth.JwtTokenDecoder
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.auth.createOAuthConfigurationProvider
import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.pEp.infrastructure.livedata.Event
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.fsck.k9.pEp.ui.ConnectionSettings
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.browser.BrowserDescriptor
import net.openid.appauth.browser.BrowserSelector
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

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val preferences: Preferences = mockk()
    private val oAuthConfigurationProvider = spyk(createOAuthConfigurationProvider())
    private val jwtTokenDecoder: JwtTokenDecoder = mockk()
    private val discovery: ProvidersXmlDiscovery = mockk()
    private lateinit var activityResultRegistry: ActivityResultRegistry
    private var launcher: ActivityResultLauncher<Intent>? = null
    private val lifecycle: LifecycleRegistry = spyk(LifecycleRegistry(mockk()))
    private val authState: AuthState = mockk()
    private val account: Account = mockk()
    private val receivedDiscoveredSettings = mutableListOf<Event<ConnectionSettings?>>()
    private val receivedUiStates = mutableListOf<AuthFlowState>()

    private val viewModel = AuthViewModel(
        app,
        preferences,
        oAuthConfigurationProvider,
        jwtTokenDecoder,
        discovery,
        coroutinesTestRule.testDispatcherProvider,
    )

    private val testConnectionSettings: ConnectionSettings = ConnectionSettings(
        incoming = ServerSettings(
            ServerSettings.Type.IMAP,
            TEST_HOST,
            TEST_PORT,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            TEST_USERNAME,
            null,
            null
        ),
        outgoing = ServerSettings(
            ServerSettings.Type.SMTP,
            TEST_HOST,
            TEST_PORT,
            ConnectionSecurity.NONE,
            AuthType.PLAIN,
            TEST_USERNAME,
            null,
            null
        )
    )

    private val testOAuthConfig = OAuthConfiguration(
        CLIENT_ID,
        SCOPES,
        AUTH_ENDPOINT,
        TOKEN_ENDPOINT,
        REDIRECT_URI
    )

    @Before
    fun setUp() {
        receivedDiscoveredSettings.clear()
        receivedUiStates.clear()
        stubAuthResultRegistry()
        every { account.email }.returns(EMAIL)
        observeViewModel(viewModel)
        assertEquals(AuthFlowState.Idle, viewModel.uiState.value)
        assertFalse(viewModel.needsMailSettingsDiscovery)
        assertEquals(Event<ConnectionSettings?>(null, false), viewModel.connectionSettings.value)
        mockkStatic(AuthState::class)
        mockkStatic(RemoteStore::class)
        mockkStatic(net.openid.appauth.browser.BrowserSelector::class)
        stubBrowserAvailability()
    }

    @After
    fun tearDown() {
        unmockkStatic(AuthState::class)
        unmockkStatic(RemoteStore::class)
        unmockkStatic(net.openid.appauth.browser.BrowserSelector::class)
        launcher = null
    }

    private fun getTestServerSettings(
        host: String? = TEST_HOST
    ) = ServerSettings(
        ServerSettings.Type.IMAP,
        host,
        TEST_PORT,
        ConnectionSecurity.NONE,
        AuthType.PLAIN,
        TEST_USERNAME,
        null,
        null
    )

    private fun getTestDiscoveryResults(
        username: String? = TEST_USERNAME,
        authType: AuthType? = AuthType.PLAIN,
        incomingEmpty: Boolean = false,
        outgoingEmpty: Boolean = false,
    ): DiscoveryResults {
        return DiscoveryResults(
            if (incomingEmpty) emptyList()
            else listOf(
                DiscoveredServerSettings(
                    ServerSettings.Type.IMAP,
                    TEST_HOST,
                    TEST_PORT,
                    ConnectionSecurity.NONE,
                    authType,
                    username
                )
            ),
            if (outgoingEmpty) emptyList()
            else listOf(
                DiscoveredServerSettings(
                    ServerSettings.Type.SMTP,
                    TEST_HOST,
                    TEST_PORT,
                    ConnectionSecurity.NONE,
                    authType,
                    username
                )
            )
        )
    }

    private fun stubBrowserAvailability(
        descriptor: BrowserDescriptor? = BrowserDescriptor(
            PackageInfo().apply { signatures = emptyArray() },
            false
        )
    ) {
        every { BrowserSelector.select(any(), any()) }.returns(descriptor)
    }

    private fun stubAuthResultRegistry(
        authResultCode: Int = Activity.RESULT_OK,
        authResultData: Intent? = Intent().apply {
            data = AUTH_INTENT_DATA.toUri()
            putExtra(AuthorizationResponse.EXTRA_RESPONSE, AUTH_INTENT_RESPONSE)
        }
    ) {
        val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, authResultCode, authResultData)
            }
        }
        this.activityResultRegistry = spyk(activityResultRegistry)
        stubActivityResultLauncher()
    }

    @Test
    fun `isAuthorized returns false if Account's oAuthState is not authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(authState)
        every { authState.isAuthorized }.returns(false)
        every { account.oAuthState }.returns(AUTH_STATE)


        assertFalse(viewModel.isAuthorized(account))


        verify { account.oAuthState }
        verify { AuthState.jsonDeserialize(AUTH_STATE) }
    }

    @Test
    fun `isAuthorized returns true if Account's oAuthState is authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(authState)
        every { authState.isAuthorized }.returns(true)
        every { account.oAuthState }.returns(AUTH_STATE)


        assertTrue(viewModel.isAuthorized(account))


        verify { account.oAuthState }
        verify { AuthState.jsonDeserialize(AUTH_STATE) }
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
        every { account.storeUri }.returns(STORE_URI)
        val settings = getTestServerSettings()
        every { RemoteStore.decodeStoreUri(STORE_URI) }.returns(settings)
        every { oAuthConfigurationProvider.isGoogle(settings.host) }.returns(true)


        assertTrue(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri(STORE_URI) }
        verify { oAuthConfigurationProvider.isGoogle(settings.host) }
    }

    @Test
    fun `isUsingGoogle returns false if Account's mandatoryProviderType is null and server settings host is not Google`() {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns(STORE_URI)
        val settings = getTestServerSettings()
        every { RemoteStore.decodeStoreUri(STORE_URI) }.returns(settings)
        every { oAuthConfigurationProvider.isGoogle(settings.host) }.returns(false)


        assertFalse(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri(STORE_URI) }
        verify { oAuthConfigurationProvider.isGoogle(settings.host) }
    }

    @Test
    fun `isUsingGoogle returns false if Account's mandatoryProviderType is null and server settings host is null`() {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns(STORE_URI)
        val settings = getTestServerSettings(host = null)
        every { RemoteStore.decodeStoreUri(STORE_URI) }.returns(settings)


        assertFalse(viewModel.isUsingGoogle(account))
        verify { account.mandatoryOAuthProviderType }
        verify { RemoteStore.decodeStoreUri(STORE_URI) }
        verify { oAuthConfigurationProvider.wasNot(called) }
    }

    @Test
    fun `discoverMailSettingsAsync uses discovery to discover Account mail settings`() = runTest {
        coEvery { discovery.discover(any(), any()) }.returns(null)


        viewModel.discoverMailSettingsAsync(EMAIL, null)
        advanceUntilIdle()


        coVerify { discovery.discover(EMAIL, null) }
    }

    @Test
    fun `discoverMailSettingsAsync sets connectionSettings LiveData with discovery result`() =
        runTest {
            coEvery { discovery.discover(any(), any()) }.returns(
                getTestDiscoveryResults()
            )


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
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


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
            assertFalse(viewModel.needsMailSettingsDiscovery)
        }

    @Test
    fun `discoverMailSettingsAsync returns null if discovered settings are null`() = runTest {
        coEvery { discovery.discover(any(), any()) }.returns(null)


        viewModel.discoverMailSettingsAsync(EMAIL, null)
        advanceUntilIdle()


        coVerify { discovery.discover(EMAIL, null) }
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


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
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


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
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


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
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


            viewModel.discoverMailSettingsAsync(EMAIL, null)
            advanceUntilIdle()


            coVerify { discovery.discover(EMAIL, null) }
            assertReceivedDiscoveredSettings(
                listOf(
                    Event(null, false),
                    Event(null, true)
                )
            )
        }

    @Test
    fun `login() starts OAuth login if Account's mandatoryOAuthProviderType is Google`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        verify { account.email }
        verify { launcher!!.launch(any()) }
    }

    @Test
    fun `login() starts OAuth login if Account's mandatoryOAuthProviderType is Microsoft`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            verify { account.email }
            verify { launcher!!.launch(any()) }
        }

    @Test
    fun `login() sets state NotSupported if OAuth configuration for account cannot be found`() =
        runTest {
            stubOAuthConfigurationWithoutMandatoryOAuthProvider(configurationFound = false)


            viewModel.login(account)
            advanceUntilIdle()


            verifyOAuthConfigurationRetrievalWithoutMandatoryOAuthProvider()
            assertEquals(
                listOf(AuthFlowState.Idle, AuthFlowState.NotSupported),
                receivedUiStates
            )
        }

    @Test
    fun `login() does not continue if Account's mandatoryOAuthProviderType is null and Account incoming settings host is null`() =
        runTest {
            stubOAuthConfigurationWithoutMandatoryOAuthProvider(incomingSettingsHost = null)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verifyOAuthConfigurationRetrievalWithoutMandatoryOAuthProvider(incomingSettingsHost = null)
            verify(exactly = 0) { account.email }
            verify(exactly = 0) { launcher!!.launch(any()) }
            verify(exactly = 0) { activityResultRegistry.dispatchResult(any(), any(), any()) }
        }

    private fun stubActivityResultLauncher() {
        every {
            activityResultRegistry.register(
                any(),
                any<ActivityResultContract<Intent, Any>>(),
                any<ActivityResultCallback<Any>>()
            )
        }.answers { callOriginal().also { launcher = it } }
    }

    @Test
    fun `login() starts OAuth login if Account's mandatoryOAuthProviderType is null and OAuth configuration for Account is found`() =
        runTest {
            stubOAuthConfigurationWithoutMandatoryOAuthProvider()
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verifyOAuthConfigurationRetrievalWithoutMandatoryOAuthProvider()
            verify { account.email }
            verify { launcher!!.launch(any()) }
            verify { activityResultRegistry.dispatchResult(any(), any(), any()) }
        }

    @Test
    fun `login() starts login and sets state to Canceled if OAuth result intent is null`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
            stubOnCreateEvent()
            stubAuthResultRegistry(authResultData = null)


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
            verify { account.email }
            verify { launcher!!.launch(any()) }
            assertEquals(
                listOf(AuthFlowState.Idle, AuthFlowState.Canceled),
                receivedUiStates
            )
        }

    @Test
    fun `login() starts login with expected intent`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        verify { account.email }
        val intentSlot = slot<Intent>()
        verify { launcher!!.launch(capture(intentSlot)) }
        val intent = intentSlot.captured
        val request = AuthorizationRequest.jsonDeserialize(
            intent.getStringExtra(EXTRA_AUTH_REQUEST)!!
        )
        assertAuthorizationRequest(request)
    }

    @Test
    fun `login() starts login success path`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        verify { account.email }
        verify { launcher!!.launch(any()) }
        verify { activityResultRegistry.dispatchResult(any(), any(), any()) }
    }

    private fun assertAuthorizationRequest(request: AuthorizationRequest) {
        assertEquals(REDIRECT_URI, request.redirectUri.toString())
        assertEquals(EMAIL, request.loginHint)
        assertEquals(SCOPES.joinToString(" ") + " openid email", request.scope)
        assertEquals(CLIENT_ID, request.clientId)
        assertEquals(AUTH_ENDPOINT, request.configuration.authorizationEndpoint.toString())
        assertEquals(TOKEN_ENDPOINT, request.configuration.tokenEndpoint.toString())
    }

    private fun stubOAuthConfigurationWithMandatoryOAuthProvider(
        oAuthProviderType: OAuthProviderType,
    ) {
        every { account.mandatoryOAuthProviderType }.returns(oAuthProviderType)
        when (oAuthProviderType) {
            OAuthProviderType.GOOGLE ->
                every { oAuthConfigurationProvider.googleConfiguration }
                    .returns(testOAuthConfig)
            OAuthProviderType.MICROSOFT ->
                every { oAuthConfigurationProvider.microsoftConfiguration }
                    .returns(testOAuthConfig)
        }
    }

    private fun stubOAuthConfigurationWithoutMandatoryOAuthProvider(
        accountStoreUri: String? = STORE_URI,
        configurationFound: Boolean = true,
        incomingSettingsHost: String? = ""
    ) {
        every { account.mandatoryOAuthProviderType }.returns(null)
        every { account.storeUri }.returns(accountStoreUri)
        every { RemoteStore.decodeStoreUri(any()) }
            .returns(getTestServerSettings(host = incomingSettingsHost))
        val foundConfiguration = if (configurationFound) testOAuthConfig else null
        every { oAuthConfigurationProvider.getConfiguration(any()) }
            .returns(foundConfiguration)
    }

    private fun verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(
        oAuthProviderType: OAuthProviderType,
    ) {
        verify { account.mandatoryOAuthProviderType }
        verify(exactly = 0) { account.storeUri }
        verify(exactly = 0) { RemoteStore.decodeStoreUri(any()) }
        verify(exactly = 0) {
            oAuthConfigurationProvider.getConfiguration(getTestServerSettings().host)
        }
        if (oAuthProviderType == OAuthProviderType.GOOGLE) {
            verify { oAuthConfigurationProvider.googleConfiguration }
        } else {
            verify { oAuthConfigurationProvider.microsoftConfiguration }
        }
    }

    private fun verifyOAuthConfigurationRetrievalWithoutMandatoryOAuthProvider(
        accountStoreUri: String? = STORE_URI,
        incomingSettingsHost: String? = "",
    ) {
        verify { account.mandatoryOAuthProviderType }
        verify { account.mandatoryOAuthProviderType }
        verify { account.storeUri }
        if (accountStoreUri != null) {
            verify { RemoteStore.decodeStoreUri(STORE_URI) }
            if (incomingSettingsHost != null) {
                verify { oAuthConfigurationProvider.getConfiguration(incomingSettingsHost) }
            } else {
                verify { oAuthConfigurationProvider.wasNot(called) }
            }
        } else {
            verify(exactly = 0) { RemoteStore.decodeStoreUri(STORE_URI) }
        }
    }

    private fun stubOnCreateEvent() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) // triggers AuthViewModel.AppAuthResultObserver onCreate()
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
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.uiState.collect {
                receivedUiStates.add(it)
            }
        }
    }

    companion object {
        private const val STORE_URI = "storeUri"
        private const val AUTH_STATE = "authstate"
        private const val EMAIL = "email@test.ch"
        private const val TEST_HOST = "testhost"
        private const val TEST_PORT = 0
        private const val TEST_USERNAME = "testUsername"
        private const val CLIENT_ID = "clientId"
        private val SCOPES = listOf("scope1", "scope2")
        private const val AUTH_ENDPOINT = "authEndpoint"
        private const val TOKEN_ENDPOINT = "tokenEndpoint"
        private const val REDIRECT_URI = "redirectUri"
        private const val AUTH_INTENT_DATA =
            "security.pep.debug:/oauth2redirect?state=hqZkZ0zfa2hV-vprWPvNNA&code=4/0AWtgzh71Xwhvk6W3EkDAeOvjSgsu9sxt7hoi-t1K1VsB-YQp07U4NPIEDvc_LR20NcgClg&scope=email%20https://mail.google.com/%20openid%20https://www.googleapis.com/auth/userinfo.email&authuser=0&prompt=consent"
        private const val AUTH_INTENT_RESPONSE =
            "{\"request\":{\"configuration\":{\"authorizationEndpoint\":\"https:\\/\\/accounts.google.com\\/o\\/oauth2\\/v2\\/auth\",\"tokenEndpoint\":\"https:\\/\\/oauth2.googleapis.com\\/token\"},\"clientId\":\"690214278127-jqoa776om9fq731beiqap4h52b1hu3bs.apps.googleusercontent.com\",\"responseType\":\"code\",\"redirectUri\":\"security.pEp.debug:\\/oauth2redirect\",\"scope\":\"https:\\/\\/mail.google.com\\/ openid email\",\"state\":\"t9DxRl_orZxifx3b10Eupg\",\"nonce\":\"ciS0Fun9G7SEArbh5rn5eA\",\"codeVerifier\":\"8pjq6JAYeApubQmFzx-X1uM2OuVrY3IrE5i4wd1RkXcItRuDks6cE_nDJsuKQSHy62QIPNFW7YmfVXr8eDkFzA\",\"codeVerifierChallenge\":\"Uqg821TVglCYP7Mp5wn5ciWyP9Q6ZT5gbOh9LcMDgRc\",\"codeVerifierChallengeMethod\":\"S256\",\"additionalParameters\":{}},\"state\":\"t9DxRl_orZxifx3b10Eupg\",\"code\":\"4\\/0AWtgzh6vLRIfdoxzKwGObXCp0mTf1VF97HztjjeIwgN8kNVAoBpZBKza4mABfSeyalRd6Q\",\"scope\":\"email https:\\/\\/mail.google.com\\/ openid https:\\/\\/www.googleapis.com\\/auth\\/userinfo.email\",\"additional_parameters\":{\"authuser\":\"0\",\"prompt\":\"consent\"}}"
        private const val EXTRA_AUTH_REQUEST = "authRequest"
    }

}