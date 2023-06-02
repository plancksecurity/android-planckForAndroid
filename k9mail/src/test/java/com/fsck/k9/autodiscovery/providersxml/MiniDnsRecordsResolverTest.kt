package com.fsck.k9.autodiscovery.providersxml

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

    @Before
    fun setUp() {
        val resolverResult: ResolverResult<MX> = mockk()
        every { resolverResult.answersOrEmptySet }.returns(setOf(MX(0, MX_NAME)))
        mockkObject(DnssecResolverApi.INSTANCE)
        every { DnssecResolverApi.INSTANCE.resolve(DOMAIN, MX::class.java) }.returns(
            resolverResult
        )
    }

    @Test
    fun `MiniDnsRecordsResolver finds real domain using minidns library`() {
        val realDomain = dnsRecordsResolver.getRealDomain(DOMAIN)
        assertEquals(REAL_DOMAIN, realDomain)
    }

    @After
    fun tearDown() {
        unmockkObject(DnssecResolverApi.INSTANCE)
    }
}