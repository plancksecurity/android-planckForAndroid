package com.fsck.k9.autodiscovery.dnsrecords

import org.koin.dsl.module.applicationContext

val dnsRecordsResolverModule = applicationContext {
    factory<DnsRecordsResolver> { MiniDnsRecordsResolver() }
}
