package com.fsck.k9.activity.setup

import android.app.Application
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationService
import net.openid.appauth.browser.BrowserDescriptor
import net.openid.appauth.browser.BrowserSelector
import javax.inject.Inject

private const val MICROSOFT_BROWSER = "microsoft"

class AuthServiceFactory @Inject constructor(private val application: Application) {
    fun create(allowMsBrowser: Boolean): AuthorizationService {
        val (unsuitable, suitable) =
            BrowserSelector.getAllBrowsers(application)
                .filter { it.packageName != null }
                .partition { !allowMsBrowser && it.isMicrosoftBrowser() }
        if (unsuitable.isNotEmpty() && suitable.isEmpty()) {
            throw UnsuitableBrowserFound()
        }
        return AuthorizationService(
            application,
            AppAuthConfiguration.Builder()
                .setBrowserMatcher { matcher -> matcher in suitable }
                .build()
        )
    }

    private fun BrowserDescriptor.isMicrosoftBrowser() =
        packageName.contains(MICROSOFT_BROWSER, ignoreCase = true)
}