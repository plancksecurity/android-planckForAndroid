package com.fsck.k9.autodiscovery.dnsrecords

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

class MiniDnsRecordsResolverTest {
    private val dnsRecordsResolver: DnsRecordsResolver = MiniDnsRecordsResolver()

    @Before
    fun setUp() {
        val resolverResult: ResolverResult<MX> = mockk()
        every { resolverResult.answersOrEmptySet }.returns(setOf(MX(0, "realhost.realdomain")))
        mockkObject(DnssecResolverApi.INSTANCE)
        every { DnssecResolverApi.INSTANCE.resolve("domain", MX::class.java) }.returns(
            resolverResult
        )
    }

    @Test
    fun `MiniDnsRecordsResolver finds real domain using minidns library`() {
        val realDomain = dnsRecordsResolver.getRealDomain("domain")
        assertEquals("realdomain", realDomain)
    }

    @After
    fun tearDown() {
        unmockkObject(DnssecResolverApi.INSTANCE)
    }
}