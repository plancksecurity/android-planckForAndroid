package com.fsck.k9.mail.oauth

import com.fsck.k9.mail.store.StoreConfig

interface OAuthTokenProviderFactory {
    fun create(storeConfig: StoreConfig): OAuth2TokenProvider?
}