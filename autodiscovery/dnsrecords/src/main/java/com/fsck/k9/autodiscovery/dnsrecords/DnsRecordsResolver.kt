package com.fsck.k9.autodiscovery.dnsrecords

interface DnsRecordsResolver {
    fun getRealDomain(domain: String): String
}
