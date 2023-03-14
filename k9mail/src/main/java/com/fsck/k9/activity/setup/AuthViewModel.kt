package com.fsck.k9.activity.setup

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.auth.JwtTokenDecoder
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider
import com.fsck.k9.pEp.DefaultDispatcherProvider
import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.pEp.infrastructure.livedata.Event
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.fragments.toServerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import timber.log.Timber

private const val KEY_AUTHORIZATION = "app.pep_auth"
private const val ACCESS_DENIED_BY_USER = "access_denied"

class AuthViewModel(
    application: Application,
    private val accountManager: Preferences,
    private val oAuthConfigurationProvider: OAuthConfigurationProvider,
    private val jwtTokenDecoder: JwtTokenDecoder,
    private val mailSettingsDiscovery: ProvidersXmlDiscovery,
    private val authServiceFactory: AuthServiceFactory = AuthServiceFactory(application),
    private val authState: AuthState = AuthState(),
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : AndroidViewModel(application) {
    private var authService: AuthorizationService? = null

    private var account: Account? = null

    private lateinit var resultObserver: AppAuthResultObserver

    private val _uiState = MutableStateFlow<AuthFlowState>(AuthFlowState.Idle)
    val uiState: StateFlow<AuthFlowState> = _uiState.asStateFlow()

    var needsMailSettingsDiscovery = false
        private set

    private val _connectionSettings =
        MutableLiveData<Event<ConnectionSettings?>>(Event(null, false))
    val connectionSettings: LiveData<Event<ConnectionSettings?>> =
        _connectionSettings

    fun discoverMailSettingsAsync(email: String, oAuthProviderType: OAuthProviderType? = null) {
        viewModelScope.launch {
            discoverMailSettings(email, oAuthProviderType)
                .also { settings ->
                    if (settings != null) {
                        discoverMailSettingsSuccess()
                    }
                    _connectionSettings.value = Event(settings)
                }
        }
    }

    private fun discoverMailSettingsSuccess() {
        needsMailSettingsDiscovery = false
    }

    private suspend fun discoverMailSettings(
        email: String,
        oAuthProviderType: OAuthProviderType? = null
    ): ConnectionSettings? = withContext(dispatcherProvider.io()) {
        val discoveryResults = mailSettingsDiscovery.discover(
            email,
            oAuthProviderType
        )
        if (discoveryResults == null || discoveryResults.incoming.isEmpty() || discoveryResults.outgoing.isEmpty()) {
            return@withContext null
        }

        val incomingServerSettings =
            discoveryResults.incoming.first().toServerSettings() ?: return@withContext null
        val outgoingServerSettings =
            discoveryResults.outgoing.first().toServerSettings() ?: return@withContext null

        return@withContext ConnectionSettings(incomingServerSettings, outgoingServerSettings)
    }

    @Synchronized
    private fun getAuthService(): AuthorizationService {
        return authService ?: authServiceFactory.create().also { authService = it }
    }

    fun init(activityResultRegistry: ActivityResultRegistry, lifecycle: Lifecycle) {
        resultObserver = AppAuthResultObserver(activityResultRegistry)
        lifecycle.addObserver(resultObserver)
    }

    fun authResultConsumed() {
        _uiState.value = AuthFlowState.Idle
    }

    fun isAuthorized(account: Account): Boolean {
        val authState = getOrCreateAuthState(account)
        return authState.isAuthorized
    }

    fun isUsingGoogle(account: Account): Boolean {
        return if (account.mandatoryOAuthProviderType != null) {
            account.mandatoryOAuthProviderType == OAuthProviderType.GOOGLE
        } else {
            val incomingSettings = RemoteStore.decodeStoreUri(account.storeUri)
            incomingSettings.host?.let { oAuthConfigurationProvider.isGoogle(it) } ?: false
        }
    }

    private fun getOrCreateAuthState(account: Account): AuthState {
        return try {
            account.oAuthState?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
        } catch (e: Exception) {
            Timber.e(e, "Error deserializing AuthState")
            AuthState()
        }
    }

    fun login(account: Account) {
        this.account = account

        viewModelScope.launch {
            val config = findOAuthConfiguration(account)
            if (config == null) {
                _uiState.value = AuthFlowState.NotSupported
                return@launch
            }

            try {
                startLogin(account, config)
            } catch (e: ActivityNotFoundException) {
                _uiState.value = AuthFlowState.BrowserNotFound
            }
        }
    }

    private suspend fun startLogin(account: Account, config: OAuthConfiguration) {
        val authRequestIntent = withContext(dispatcherProvider.io()) {
            createAuthorizationRequestIntent(account.email, config)
        }

        resultObserver.login(authRequestIntent)
    }

    private fun createAuthorizationRequestIntent(email: String?, config: OAuthConfiguration): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            config.authorizationEndpoint.toUri(),
            config.tokenEndpoint.toUri()
        )

        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            config.clientId,
            ResponseTypeValues.CODE,
            config.redirectUri.toUri()
        )

        val scopeString = config.scopes.joinToString(separator = " ")
        val authRequest = authRequestBuilder
            .setScope(scopeString)
            .setLoginHint(email)
            .build()

        val authService = getAuthService()

        return authService.getAuthorizationRequestIntent(authRequest)
    }

    private fun findOAuthConfiguration(account: Account): OAuthConfiguration? {
        return when (account.mandatoryOAuthProviderType) {
            null -> {
                val incomingSettings = account.storeUri?.let { RemoteStore.decodeStoreUri(it) }
                oAuthConfigurationProvider.getConfiguration(
                    incomingSettings?.host ?: error("account not initialized here!")
                )
            }
            OAuthProviderType.GOOGLE -> oAuthConfigurationProvider.googleConfiguration
            OAuthProviderType.MICROSOFT -> oAuthConfigurationProvider.microsoftConfiguration
        }
    }

    private fun onLoginResult(authorizationResult: AuthorizationResult?) {
        if (authorizationResult == null) {
            _uiState.value = AuthFlowState.Canceled
            return
        }

        authorizationResult.response?.let { response ->
            authState.update(authorizationResult.response, authorizationResult.exception)
            exchangeToken(response)
        }

        authorizationResult.exception?.let { authorizationException ->
            val nextState = if (authorizationException.error == ACCESS_DENIED_BY_USER) {
                AuthFlowState.Canceled
            } else {
                AuthFlowState.Failed(
                    errorCode = authorizationException.error,
                    errorMessage = authorizationException.errorDescription
                )
            }
            _uiState.value =  nextState
        }
    }

    private fun exchangeToken(response: AuthorizationResponse) {
        viewModelScope.launch(dispatcherProvider.io()) {
            val authService = getAuthService()

            val tokenRequest = response.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { tokenResponse, authorizationException ->
                authState.update(tokenResponse, authorizationException)

                val account = account!!
                var failureUpdatingEmail: Throwable? = null
                tokenResponse?.idToken?.let {
                    failureUpdatingEmail = updateEmailAddressFromOAuthToken(it)
                }
                account.oAuthState = authState.jsonSerializeString()

                if (account.setupState == Account.SetupState.READY) {
                    viewModelScope.launch(Dispatchers.IO) {
                        account.save(accountManager)
                    }
                }

                if (authorizationException != null) {
                    _uiState.value = AuthFlowState.Failed(
                        errorCode = authorizationException.error,
                        errorMessage = authorizationException.errorDescription
                    )
                } else if (failureUpdatingEmail != null) {
                    val error = failureUpdatingEmail!!
                    if (error is WrongEmailAddressException) {
                        _uiState.value = AuthFlowState.WrongEmailAddress(error)
                    } else {
                        _uiState.value = AuthFlowState.Failed(error)
                    }
                } else {
                    _uiState.value = AuthFlowState.Success
                }
            }
        }
    }

    private fun updateEmailAddressFromOAuthToken(token: String): Throwable? {
        var error: Throwable? = null
        jwtTokenDecoder.getEmail(token).onSuccess { newEmail ->
            newEmail?.let {
                val account = account!!
                if (account.email != newEmail) {
                    if (getApplication<K9>().isRunningOnWorkProfile) {
                        error = WrongEmailAddressException(account.email, newEmail)
                    } else {
                        account.email = newEmail
                        needsMailSettingsDiscovery = true
                    }
                }
            } ?: let {
                error = IllegalStateException("Could not retrieve email address from login response") // not localized, context is an application error, not user error
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
            error = throwable
        }
        return error
    }

    @Synchronized
    override fun onCleared() {
        authService?.dispose()
        authService = null
    }

    inner class AppAuthResultObserver(private val registry: ActivityResultRegistry) : LifecycleEventObserver {
        private var authorizationLauncher: ActivityResultLauncher<Intent>? = null
        private var authRequestIntent: Intent? = null

        fun onCreate() {
            authorizationLauncher = registry.register(KEY_AUTHORIZATION, AuthorizationContract(), ::onLoginResult)
            authRequestIntent?.let { intent ->
                authRequestIntent = null
                login(intent)
            }
        }

        fun onDestroy() {
            authorizationLauncher = null
        }

        fun login(authRequestIntent: Intent) {
            val launcher = authorizationLauncher
            if (launcher != null) {
                launcher.launch(authRequestIntent)
            } else {
                this.authRequestIntent = authRequestIntent
            }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event.targetState) {
                Lifecycle.State.CREATED -> onCreate()
                Lifecycle.State.DESTROYED -> onDestroy()
                else -> {}
            }
        }
    }
}

private class AuthorizationContract : ActivityResultContract<Intent, AuthorizationResult?>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AuthorizationResult? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            AuthorizationResult(
                response = AuthorizationResponse.fromIntent(intent),
                exception = AuthorizationException.fromIntent(intent)
            )
        } else {
            null
        }
    }
}

private data class AuthorizationResult(
    val response: AuthorizationResponse?,
    val exception: AuthorizationException?
)

sealed interface AuthFlowState {
    object Idle : AuthFlowState

    object Success : AuthFlowState

    object NotSupported : AuthFlowState

    object BrowserNotFound : AuthFlowState

    object Canceled : AuthFlowState

    data class Failed(val errorCode: String?, val errorMessage: String?) : AuthFlowState {

        constructor(throwable: Throwable): this(
            errorCode = null,
            errorMessage = if (BuildConfig.DEBUG) throwable.stackTraceToString()
            else throwable.message
        )

        override fun toString(): String {
            return listOfNotNull(errorCode, errorMessage).joinToString(separator = " - ")
        }
    }

    data class WrongEmailAddress(
        val adminEmail: String,
        val userWrongEmail: String
        ): AuthFlowState {
            constructor(exception: WrongEmailAddressException) : this(
                exception.adminEmail,
                exception.userWrongEmail
            )
        }
}

class WrongEmailAddressException(val adminEmail: String, val userWrongEmail: String): Exception()

class AuthServiceFactory(private val application: Application) {
    fun create(): AuthorizationService = AuthorizationService(application)
}
