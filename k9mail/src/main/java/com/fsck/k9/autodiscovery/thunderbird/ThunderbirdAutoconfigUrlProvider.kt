package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.helper.EmailHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.minidns.hla.DnssecResolverApi
import org.minidns.record.MX
import timber.log.Timber

class ThunderbirdAutoconfigUrlProvider {
    fun getAutoconfigUrls(email: String): List<HttpUrl> {
        var domain = EmailHelper.getDomainFromEmailAddress(email)

        requireNotNull(domain) { "Couldn't extract domain from email address: $email" }
        domain = getRealDomain(domain)

        return listOf(
            createProviderUrl(domain, email),
            createDomainUrl(scheme = "https", domain),
            createDomainUrl(scheme = "http", domain),
            createIspDbUrl(domain)
        )
    }

    private fun createProviderUrl(domain: String?, email: String): HttpUrl {
        // https://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme("https")
            .host("autoconfig.$domain")
            .addEncodedPathSegments("mail/config-v1.1.xml")
            .addQueryParameter("emailaddress", email)
            .build()
    }

    private fun createDomainUrl(scheme: String, domain: String): HttpUrl {
        // https://{domain}/.well-known/autoconfig/mail/config-v1.1.xml
        // http://{domain}/.well-known/autoconfig/mail/config-v1.1.xml
        return HttpUrl.Builder()
            .scheme(scheme)
            .host(domain)
            .addEncodedPathSegments(".well-known/autoconfig/mail/config-v1.1.xml")
            .build()
    }

    private fun createIspDbUrl(domain: String): HttpUrl {
        // https://autoconfig.thunderbird.net/v1.1/{domain}
        return "https://autoconfig.thunderbird.net/v1.1/".toHttpUrl()
            .newBuilder()
            .addPathSegment(domain)
            .build()
    }

    private fun getRealDomain(domain: String): String = runBlocking(Dispatchers.IO) {
        var realDomain = domain
        kotlin.runCatching { DnssecResolverApi.INSTANCE.resolve(domain, MX::class.java) }
            .onSuccess {
                val realDomains = it.answersOrEmptySet
                if (realDomains.isNotEmpty()) {
                    realDomain = realDomains.first().target.domainpart
                }
            }
            .onFailure { Timber.e(it) }
        realDomain
    }
}
