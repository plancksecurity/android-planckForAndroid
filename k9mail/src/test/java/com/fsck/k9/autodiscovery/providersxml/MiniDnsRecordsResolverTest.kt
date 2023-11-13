package com.fsck.k9.autodiscovery.providersxml

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.providersxml.MiniDnsRecordsResolver.Companion.GOOGLE_FALLBACK_DOMAIN
import com.fsck.k9.autodiscovery.providersxml.MiniDnsRecordsResolver.Companion.MICROSOFT_FALLBACK_DOMAIN
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.minidns.hla.DnssecResolverApi
import org.minidns.hla.ResolverResult
import org.minidns.record.MX

private const val MX_NAME = "realhost.realdomain"
private const val REAL_DOMAIN = "realdomain"
private const val DOMAIN = "domain"

class MiniDnsRecordsResolverTest {
    private val dnsRecordsResolver = MiniDnsRecordsResolver()
    private val resolverResult: ResolverResult<MX> = mockk()

    @Before
    fun setUp() {
        every { resolverResult.answersOrEmptySet }.returns(setOf(MX(0, MX_NAME)))
        mockkObject(DnssecResolverApi.INSTANCE)
        every { DnssecResolverApi.INSTANCE.resolve(DOMAIN, MX::class.java) }.returns(
            resolverResult
        )
    }

    @Test
    fun `getRealOrFallbackDomain() finds real domain using minidns library`() {
        val realDomain = dnsRecordsResolver.getRealOrFallbackDomain(DOMAIN, null)
        assertEquals(REAL_DOMAIN, realDomain)
    }

    @Test
    fun `getRealOrFallbackDomain() returns Microsoft fallback domain if it cannot find real domain and throws error`() {
        every {
            DnssecResolverApi.INSTANCE.resolve(
                DOMAIN,
                MX::class.java
            )
        }.throws(RuntimeException("test"))


        val realDomain =
            dnsRecordsResolver.getRealOrFallbackDomain(DOMAIN, OAuthProviderType.MICROSOFT)


        assertEquals(MICROSOFT_FALLBACK_DOMAIN, realDomain)
    }

    @Test
    fun `getRealOrFallbackDomain() returns Microsoft fallback domain if it cannot find real domain and returns empty`() {
        every { resolverResult.answersOrEmptySet }.returns(emptySet())


        val realDomain =
            dnsRecordsResolver.getRealOrFallbackDomain(DOMAIN, OAuthProviderType.MICROSOFT)


        assertEquals(MICROSOFT_FALLBACK_DOMAIN, realDomain)
    }

    @Test
    fun `getRealOrFallbackDomain() returns Google fallback domain if it cannot find real domain`() {
        every {
            DnssecResolverApi.INSTANCE.resolve(
                DOMAIN,
                MX::class.java
            )
        }.throws(RuntimeException("test"))


        val realDomain =
            dnsRecordsResolver.getRealOrFallbackDomain(DOMAIN, OAuthProviderType.GOOGLE)


        assertEquals(GOOGLE_FALLBACK_DOMAIN, realDomain)
    }

    @After
    fun tearDown() {
        unmockkObject(DnssecResolverApi.INSTANCE)
    }
}