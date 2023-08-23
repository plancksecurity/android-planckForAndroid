package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
import com.fsck.k9.K9
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
import com.fsck.k9.planck.infrastructure.livedata.Event
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.planck.ui.ConnectionSettings
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenRequest.GRANT_TYPE_PASSWORD
import net.openid.appauth.TokenResponse
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

    private val app: K9 = spyk(ApplicationProvider.getApplicationContext())
    private val preferences: Preferences = mockk()
    private val oAuthConfigurationProvider = spyk(createOAuthConfigurationProvider())
    private val jwtTokenDecoder: JwtTokenDecoder = mockk()
    private val discovery: ProvidersXmlDiscovery = mockk()
    private val authState: AuthState = mockk(relaxed = true)
    private val authService: AuthorizationService = mockk(relaxed = true)
    private val authServiceFactory: AuthServiceFactory = mockk()
    private lateinit var activityResultRegistry: ActivityResultRegistry
    private var launcher: ActivityResultLauncher<Intent>? = null
    private val lifecycle: LifecycleRegistry = spyk(LifecycleRegistry(mockk()))
    private val accountAuthState: AuthState = mockk()
    private val account: Account = mockk(relaxed = true)
    private val receivedDiscoveredSettings = mutableListOf<Event<ConnectionSettings?>>()
    private val receivedUiStates = mutableListOf<AuthFlowState>()

    private val viewModel = AuthViewModel(
        app,
        preferences,
        oAuthConfigurationProvider,
        jwtTokenDecoder,
        discovery,
        authServiceFactory,
        authState,
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
        SCOPES.split(" "),
        AUTH_ENDPOINT,
        TOKEN_ENDPOINT,
        REDIRECT_URI
    )

    private fun getTestAuthRequest(): AuthorizationRequest {
        val config = AuthorizationServiceConfiguration(
            AUTH_ENDPOINT.toUri(),
            TOKEN_ENDPOINT.toUri()
        )
        return AuthorizationRequest.Builder(
            config,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            REDIRECT_URI.toUri()
        )
            .setScope(SCOPES)
            .setLoginHint(EMAIL)
            .build()
    }

    private fun getTestTokenRequest(): TokenRequest {
        val config = AuthorizationServiceConfiguration(
            AUTH_ENDPOINT.toUri(),
            TOKEN_ENDPOINT.toUri()
        )
        return TokenRequest.Builder(
            config,
            CLIENT_ID,
        )
            .setScope(SCOPES)
            .setGrantType(GRANT_TYPE_PASSWORD)
            .build()
    }

    @Before
    fun setUp() {
        every { authServiceFactory.create() }.returns(authService)
        receivedDiscoveredSettings.clear()
        receivedUiStates.clear()
        stubAuthResultRegistry() // simulates the test result arrives instantly
        every { account.email }.returns(EMAIL)
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.success(TOKEN_RESPONSE_EMAIL))
        // call AuthState real methods because they help by enforcing that either response or exception need to be null
        every {
            authState.update(any<AuthorizationResponse>(), any())
        }.answers { callOriginal() }
        every { authState.update(any<TokenResponse>(), any()) }
            .answers { callOriginal() }
        observeViewModel(viewModel)
        assertEquals(AuthFlowState.Idle, viewModel.uiState.value)
        assertFalse(viewModel.needsMailSettingsDiscovery)
        assertEquals(
            Event<ConnectionSettings?>(null, false),
            viewModel.connectionSettings.value
        )
        mockkStatic(AuthState::class)
        mockkStatic(RemoteStore::class)
        mockkStatic(AuthorizationResponse::class)
        mockkStatic(AuthorizationException::class)
        every { authService.getAuthorizationRequestIntent(any()) }.returns(mockk())
        stubAuthResult() // stubs authorization request result
        stubTokenRequest() // stubs token request result
    }

    @After
    fun tearDown() {
        unmockkStatic(AuthState::class)
        unmockkStatic(RemoteStore::class)
        unmockkStatic(BrowserSelector::class)
        unmockkStatic(AuthorizationResponse::class)
        unmockkStatic(AuthorizationException::class)
        launcher = null
    }

    private fun stubTokenRequest(
        tokenResponse: TokenResponse? = TokenResponse.Builder(getTestTokenRequest())
            .setIdToken(ID_TOKEN)
            .build(),
        authException: AuthorizationException? = null
    ) {
        val tokenRequestSlot = slot<TokenRequest>()
        val tokenResponseCallbackSlot = slot<AuthorizationService.TokenResponseCallback>()
        every {
            authService.performTokenRequest(
                capture(tokenRequestSlot),
                capture(tokenResponseCallbackSlot)
            )
        }.answers {
            tokenResponseCallbackSlot.captured.onTokenRequestCompleted(
                tokenResponse,
                authException
            )
        }
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

    private fun stubAuthResult(
        response: AuthorizationResponse? = defaultAuthResponse,
        authException: AuthorizationException? = null,
    ) {
        every { AuthorizationResponse.fromIntent(any()) }.returns(response)
        every { AuthorizationException.fromIntent(any()) }.returns(authException)
    }

    private val defaultAuthResponse: AuthorizationResponse =
        spyk(
            AuthorizationResponse.Builder(getTestAuthRequest())
                .setAuthorizationCode(AUTH_CODE)
                .build()
        ).also { every { it.createTokenExchangeRequest() }.returns(getTestTokenRequest()) }

    private fun stubAuthResultRegistry(
        authResultCode: Int = Activity.RESULT_OK,
        authResultData: Intent? = Intent()
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
    fun `isAuthorized returns false if Account's oAuthState is not authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(accountAuthState)
        every { accountAuthState.isAuthorized }.returns(false)
        every { account.oAuthState }.returns(AUTH_STATE)


        assertFalse(viewModel.isAuthorized(account))


        verify { account.oAuthState }
        verify { AuthState.jsonDeserialize(AUTH_STATE) }
    }

    @Test
    fun `isAuthorized returns true if Account's oAuthState is authorized`() {
        every { AuthState.jsonDeserialize(any<String>()) }.returns(accountAuthState)
        every { accountAuthState.isAuthorized }.returns(true)
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

    @Test
    fun `login() starts OAuth login if Account's mandatoryOAuthProviderType is null and OAuth configuration for Account is found`() =
        runTest {
            stubOAuthConfigurationWithoutMandatoryOAuthProvider()
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.getAuthorizationRequestIntent(any()) }
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


            verify { authService.getAuthorizationRequestIntent(any()) }
            verify { launcher!!.launch(any()) }
            assertEquals(
                listOf(AuthFlowState.Idle, AuthFlowState.Canceled),
                receivedUiStates
            )
        }

    @Test
    fun `login() starts login with expected authorization request`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        val authRequestSlot = slot<AuthorizationRequest>()
        verify { authService.getAuthorizationRequestIntent(capture(authRequestSlot)) }
        verify { launcher!!.launch(any()) }
        assertAuthorizationRequest(authRequestSlot.captured)
    }

    @Test
    fun `login() starts login with intent created by AuthService`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.GOOGLE)
        val mockAuthIntent: Intent = mockk()
        every { authService.getAuthorizationRequestIntent(any()) }.returns(mockAuthIntent)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.getAuthorizationRequestIntent(any()) }
        verify { launcher!!.launch(mockAuthIntent) }
    }

    @Test
    fun `login() sets state to BrowserNotFound if AuthService_getAuthorizationRequestIntent cannot find a browser`() =
        runTest {
            every { authService.getAuthorizationRequestIntent(any()) }
                .throws(ActivityNotFoundException())
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.getAuthorizationRequestIntent(any()) }
            verify(exactly = 0) { launcher!!.launch(any()) }
            verify(exactly = 0) { activityResultRegistry.dispatchResult(any(), any(), any()) }
            assertEquals(
                listOf(AuthFlowState.Idle, AuthFlowState.BrowserNotFound),
                receivedUiStates
            )
        }

    @Test
    fun `login() updates AuthState with authorization response`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { launcher!!.launch(any()) }
        verify { activityResultRegistry.dispatchResult(any(), any(), any()) }
        verify { authState.update(defaultAuthResponse, null) }
    }

    @Test
    fun `login() performs token request when authorization request was successful`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authState.update(any<AuthorizationResponse>(), null) }
        verify { authService.performTokenRequest(any(), any()) }
    }

    @Test
    fun `login() sets state to Canceled if authorization fails because of user denial of access`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            val authException = createAuthException(
                error = ACCESS_DENIED_BY_USER
            )
            stubAuthResult(
                response = null,
                authException = authException
            )
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { launcher!!.launch(any()) }
            verify { activityResultRegistry.dispatchResult(any(), any(), any()) }
            assertEquals(
                listOf(AuthFlowState.Idle, AuthFlowState.Canceled),
                receivedUiStates
            )
        }

    @Test
    fun `login() sets state to Failed if authorization fails`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        val authException = createAuthException()
        stubAuthResult(
            response = null,
            authException = authException
        )
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verifyOAuthConfigurationRetrievalWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        verify { account.email }
        verify { authServiceFactory.create() }
        verify { authService.getAuthorizationRequestIntent(any()) }
        verify { launcher!!.launch(any()) }
        verify { activityResultRegistry.dispatchResult(any(), any(), any()) }
        verify(exactly = 0) { authState.update(any<AuthorizationResponse>(), any()) }
        assertEquals(
            listOf(AuthFlowState.Idle, AuthFlowState.Failed(AUTH_ERROR, AUTH_ERROR_DESCRIPTION)),
            receivedUiStates
        )
    }

    @Test
    fun `login() does not update AuthState with authorization response if authorization fails`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            val authException = createAuthException()
            stubAuthResult(
                response = null,
                authException = authException
            )
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify(exactly = 0) {
                authState.update(
                    any<AuthorizationResponse>(),
                    any()
                )
            }
        }

    @Test
    fun `login() does not perform token request if authorization fails`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        val authException = createAuthException()
        stubAuthResult(
            response = null,
            authException = authException
        )
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify(exactly = 0) { authService.performTokenRequest(any(), any()) }
    }

    @Test
    fun `login() updates AuthState with token request result`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authState.update(any<AuthorizationResponse>(), null) }
        verify { authService.performTokenRequest(any(), any()) }
        verify { authState.update(any<TokenResponse>(), null) }
    }

    @Test
    fun `login() updates AuthState with failed token request result`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        val authException = createAuthException()
        stubTokenRequest(tokenResponse = null, authException = authException)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authState.update(any<AuthorizationResponse>(), null) }
        verify { authService.performTokenRequest(any(), any()) }
        verify { authState.update(null as TokenResponse?, authException) }
    }

    @Test
    fun `login() uses JwtTokenDecoder to update Account email address from token response if new email is different`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.performTokenRequest(any(), any()) }
            verify { jwtTokenDecoder.getEmail(ID_TOKEN) }
            verify { account.email = TOKEN_RESPONSE_EMAIL }
        }

    @Test
    fun `viewModel needs mail settings discovery after login() updates Account email address from token response`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.performTokenRequest(any(), any()) }
            assertTrue(viewModel.needsMailSettingsDiscovery)
        }

    @Test
    fun `login() does not update Account email address on token request result if request fails`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            val authException = createAuthException()
            stubTokenRequest(tokenResponse = null, authException = authException)
            stubOnCreateEvent()


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.performTokenRequest(any(), any()) }
            assertFalse(viewModel.needsMailSettingsDiscovery)
            verify { jwtTokenDecoder.wasNot(called) }
            verify(exactly = 0) { account.email = any() }
        }

    @Test
    fun `login() sets state to Failed on token request result if request fails`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        val authException = createAuthException()
        stubTokenRequest(tokenResponse = null, authException = authException)
        stubOnCreateEvent()


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        assertFalse(viewModel.needsMailSettingsDiscovery)
        assertEquals(
            listOf(AuthFlowState.Idle, AuthFlowState.Failed(AUTH_ERROR, AUTH_ERROR_DESCRIPTION)),
            receivedUiStates
        )
    }

    @Test
    fun `login() sets state to Failed if JwtTokenDecoder cannot not retrieve email`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.success(null))


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        verify(exactly = 0) { account.email = any() }
        assertFalse(viewModel.needsMailSettingsDiscovery)

        assertEquals(
            listOf(
                AuthFlowState.Idle,
                AuthFlowState.Failed(
                    IllegalStateException("Could not retrieve email address from login response")
                )
            ),
            receivedUiStates
        )
    }

    @Test
    fun `login() sets state to Failed if JwtTokenDecoder returns Result_failure`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.failure(RuntimeException(TEST_EXCEPTION_MESSAGE)))


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        verify(exactly = 0) { account.email = any() }
        assertFalse(viewModel.needsMailSettingsDiscovery)

        assertEquals(
            listOf(
                AuthFlowState.Idle,
                AuthFlowState.Failed(
                    RuntimeException(TEST_EXCEPTION_MESSAGE)
                )
            ),
            receivedUiStates
        )
    }

    @Test
    fun `login() sets state to WrongEmailAddress if JwtTokenDecoder returns Result_success and app is running under MDM`() =
        runTest {
            stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
            stubOnCreateEvent()
            every { jwtTokenDecoder.getEmail(any()) }
                .returns(Result.success(TOKEN_RESPONSE_EMAIL))
            every { app.isRunningOnWorkProfile }.returns(true)


            viewModel.init(activityResultRegistry, lifecycle)
            viewModel.login(account)
            advanceUntilIdle()


            verify { authService.performTokenRequest(any(), any()) }
            verify(exactly = 0) { account.email = any() }
            assertFalse(viewModel.needsMailSettingsDiscovery)

            assertEquals(
                listOf(
                    AuthFlowState.Idle,
                    AuthFlowState.WrongEmailAddress(EMAIL, TOKEN_RESPONSE_EMAIL)
                ),
                receivedUiStates
            )
        }

    @Test
    fun `login() saves AuthState into Account on token response`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.success(TOKEN_RESPONSE_EMAIL))
        every { authState.jsonSerializeString() }.returns(SERIALIZED_AUTH_STATE)


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        verify { account.oAuthState = SERIALIZED_AUTH_STATE }
        verify { authState.jsonSerializeString() }
    }

    @Test
    fun `login() saves Account on token response if Account is in READY setup state`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.success(TOKEN_RESPONSE_EMAIL))
        every { authState.jsonSerializeString() }.returns(SERIALIZED_AUTH_STATE)
        every { account.setupState }.returns(Account.SetupState.READY)


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        verify { account.save(preferences) }
    }

    @Test
    fun `login() sets state to Success on token response if there is no error`() = runTest {
        stubOAuthConfigurationWithMandatoryOAuthProvider(OAuthProviderType.MICROSOFT)
        stubOnCreateEvent()
        every { jwtTokenDecoder.getEmail(any()) }
            .returns(Result.success(TOKEN_RESPONSE_EMAIL))


        viewModel.init(activityResultRegistry, lifecycle)
        viewModel.login(account)
        advanceUntilIdle()


        verify { authService.performTokenRequest(any(), any()) }
        assertEquals(
            listOf(AuthFlowState.Idle, AuthFlowState.Success),
            receivedUiStates
        )
    }

    private fun createAuthException(
        error: String = AUTH_ERROR,
    ): AuthorizationException {
        return AuthorizationException(0, 0, error, AUTH_ERROR_DESCRIPTION, null, null)
    }

    private fun assertAuthorizationRequest(request: AuthorizationRequest) {
        assertEquals(REDIRECT_URI, request.redirectUri.toString())
        assertEquals(EMAIL, request.loginHint)
        assertEquals(SCOPES, request.scope)
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
        CoroutineScope(UnconfinedTestDispatcher()).launch {
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
        private const val SCOPES = "scope1 scope2 scope3 scope4"
        private const val AUTH_ENDPOINT = "authEndpoint"
        private const val TOKEN_ENDPOINT = "tokenEndpoint"
        private const val REDIRECT_URI = "redirectUri"
        private const val ID_TOKEN = "idToken"
        private const val TOKEN_RESPONSE_EMAIL = "tokenresponse@mail.es"
        private const val SERIALIZED_AUTH_STATE = "serializedAuthState"
        private const val TEST_EXCEPTION_MESSAGE = "test"
        private const val ACCESS_DENIED_BY_USER = "access_denied"
        private const val AUTH_ERROR = "authError"
        private const val AUTH_ERROR_DESCRIPTION = "authErrorDescription"
        private const val AUTH_CODE = "authCode"
    }

}