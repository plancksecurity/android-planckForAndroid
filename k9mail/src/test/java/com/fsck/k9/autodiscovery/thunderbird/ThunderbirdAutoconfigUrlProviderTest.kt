package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.dnsrecords.DnsRecordsResolver
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class ThunderbirdAutoconfigUrlProviderTest {
    private val dnsRecordsResolver: DnsRecordsResolver = mockk()
    private val urlProvider = ThunderbirdAutoconfigUrlProvider(dnsRecordsResolver)

    @Test
    fun `getAutoconfigUrls with ASCII email address`() {
        every { dnsRecordsResolver.getRealDomain("domain.example") }.returns("domain.example")
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
            "http://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
            "https://autoconfig.thunderbird.net/v1.1/domain.example"
        )
    }
}
