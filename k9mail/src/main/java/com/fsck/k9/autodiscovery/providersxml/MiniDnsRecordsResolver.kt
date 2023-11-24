package com.fsck.k9.autodiscovery.providersxml

import androidx.annotation.WorkerThread
import com.fsck.k9.auth.OAuthProviderType
import org.minidns.hla.DnssecResolverApi
import org.minidns.record.MX
import timber.log.Timber
import javax.inject.Inject


class MiniDnsRecordsResolver @Inject constructor() {
    @WorkerThread
    fun getRealOrFallbackDomain(domain: String, oAuthProviderType: OAuthProviderType?): String {
        return when (oAuthProviderType) {
            OAuthProviderType.MICROSOFT -> MICROSOFT_FALLBACK_DOMAIN
            OAuthProviderType.GOOGLE -> GOOGLE_FALLBACK_DOMAIN
            else -> kotlin.runCatching {
                DnssecResolverApi.INSTANCE.resolve(domain, MX::class.java)
                    .answersOrEmptySet.first().target.domainpart
            }.onFailure { Timber.e(it) }.getOrNull() ?: domain
        }
    }

    companion object {
        internal const val MICROSOFT_FALLBACK_DOMAIN = "mail.protection.outlook.com"
        internal const val GOOGLE_FALLBACK_DOMAIN = "gmail.com"
    }
}
