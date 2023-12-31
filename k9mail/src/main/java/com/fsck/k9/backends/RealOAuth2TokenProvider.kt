package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.LOGIN_REQUIRED
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationException.AuthorizationRequestErrors
import net.openid.appauth.AuthorizationException.GeneralErrors
import net.openid.appauth.AuthorizationService
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RealOAuth2TokenProvider(
    context: Context,
    private val preferences: Preferences,
    private val account: Account
) : OAuth2TokenProvider {
    private val authService = AuthorizationService(context)
    private var requestFreshToken = false

    override fun getToken(timeoutMillis: Long): String {
        val latch = CountDownLatch(1)
        var token: String? = null
        var exception: AuthorizationException? = null

        val authState = account.oAuthState?.let { AuthState.jsonDeserialize(it) }
            ?: throw AuthenticationFailedException(LOGIN_REQUIRED)

        if (requestFreshToken) {
            authState.needsTokenRefresh = true
        }

        val oldAccessToken = authState.accessToken

        authState.performActionWithFreshTokens(authService) { accessToken: String?, _, authException: AuthorizationException? ->
            token = accessToken
            exception = authException

            latch.countDown()
        }

        latch.await(timeoutMillis, TimeUnit.MILLISECONDS)

        val authException = exception
        if (authException == GeneralErrors.NETWORK_ERROR ||
            authException == GeneralErrors.SERVER_ERROR ||
            authException == AuthorizationRequestErrors.SERVER_ERROR ||
            authException == AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE
        ) {
            throw IOException("Error while fetching an access token", authException)
        } else if (authException != null) {
            account.oAuthState = null
            if (account.setupState == Account.SetupState.READY) {
                account.save(preferences)
            }

            throw AuthenticationFailedException(
                message = "Failed to fetch an access token",
                throwable = authException,
                messageFromServer = authException.error
            )
        } else if (token != oldAccessToken) {
            requestFreshToken = false
            account.oAuthState = authState.jsonSerializeString()
            if (account.setupState == Account.SetupState.READY) {
                account.save(preferences)
            }
        }

        return token ?: throw AuthenticationFailedException("Failed to fetch an access token")
    }

    override fun invalidateToken() {
        requestFreshToken = true
    }


}
