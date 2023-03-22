package com.fsck.k9.auth

import com.fsck.k9.BuildConfig
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider

const val SCOPE_OPENID = "openid"
const val SCOPE_EMAIL = "email"

fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
    val redirectUriSlash = BuildConfig.OAUTH_APP_ID + ":/oauth2redirect"
    val commonScopes = listOf(SCOPE_OPENID, SCOPE_EMAIL)

    val googleConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_GMAIL_CLIENT_ID,
        scopes = listOf("https://mail.google.com/") + commonScopes,
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        redirectUri = redirectUriSlash
    )

    val microsoftConfig = OAuthConfiguration(
        clientId = BuildConfig.OAUTH_MICROSOFT_CLIENT_ID,
        scopes = listOf(
            "https://outlook.office.com/IMAP.AccessAsUser.All",
            "https://outlook.office.com/SMTP.Send",
            "offline_access"
        ) + commonScopes,
        authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        redirectUri = BuildConfig.OAUTH_MICROSOFT_REDIRECT_URI
    )

    return OAuthConfigurationProvider(
        configurations = mapOf(
            listOf(
                "imap.gmail.com",
                "imap.googlemail.com",
                "smtp.gmail.com",
                "smtp.googlemail.com"
            ) to googleConfig,
            listOf("outlook.office365.com", "smtp.office365.com") to microsoftConfig,
        ),
        googleConfiguration = googleConfig,
        microsoftConfiguration = microsoftConfig
    )
}
