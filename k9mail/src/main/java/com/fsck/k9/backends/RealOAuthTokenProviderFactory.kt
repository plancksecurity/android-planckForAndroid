package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuthTokenProviderFactory
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mail.store.StoreConfig

class RealOAuthTokenProviderFactory(
    private val context: Context,
    private val preferences: Preferences,
) : OAuthTokenProviderFactory {
    override fun create(storeConfig: StoreConfig): OAuth2TokenProvider? {
        val settings = RemoteStore.decodeStoreUri(storeConfig.storeUri)
        return if (settings.authenticationType == AuthType.XOAUTH2) {
            RealOAuth2TokenProvider(context, preferences, storeConfig as Account)
        } else null
    }
}