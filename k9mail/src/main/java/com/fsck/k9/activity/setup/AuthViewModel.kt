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
import com.fsck.k9.Preferences
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import timber.log.Timber

private const val KEY_AUTHORIZATION = "app.k9mail_auth"

class AuthViewModel(
    application: Application,
    private val accountManager: Preferences,
    private val oAuthConfigurationProvider: OAuthConfigurationProvider
) : AndroidViewModel(application) {
    private var authService: AuthorizationService? = null
    private val authState = AuthState()

    private var account: Account? = null

    private lateinit var resultObserver: AppAuthResultObserver

    private val _uiState = MutableStateFlow<AuthFlowState>(AuthFlowState.Idle)
    val uiState: StateFlow<AuthFlowState> = _uiState.asStateFlow()

    @Synchronized
    private fun getAuthService(): AuthorizationService {
        return authService ?: AuthorizationService(getApplication<Application>()).also { authService = it }
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
        val incomingSettings = RemoteStore.decodeStoreUri(account.storeUri)
        return oAuthConfigurationProvider.isGoogle(incomingSettings.host!!)
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
        val authRequestIntent = withContext(Dispatchers.IO) {
            createAuthorizationRequestIntent(account.email, config)
        }

        resultObserver.login(authRequestIntent)
    }

    private fun createAuthorizationRequestIntent(email: String, config: OAuthConfiguration): Intent {
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
        val incomingSettings = RemoteStore.decodeStoreUri(account.storeUri)
        return oAuthConfigurationProvider.getConfiguration(incomingSettings.host!!)
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
            _uiState.value =  AuthFlowState.Failed(
                errorCode = authorizationException.error,
                errorMessage = authorizationException.errorDescription
            )
        }
    }

    private fun exchangeToken(response: AuthorizationResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            val authService = getAuthService()

            val tokenRequest = response.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { tokenResponse, authorizationException ->
                authState.update(tokenResponse, authorizationException)

                val account = account!!
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
                } else {
                    _uiState.value = AuthFlowState.Success
                }
            }
        }
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
        override fun toString(): String {
            return listOfNotNull(errorCode, errorMessage).joinToString(separator = " - ")
        }
    }
}