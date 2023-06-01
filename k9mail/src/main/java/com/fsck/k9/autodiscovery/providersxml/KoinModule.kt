package com.fsck.k9.autodiscovery.providersxml

import org.koin.dsl.module.applicationContext

val autodiscoveryProvidersXmlModule = applicationContext {
    factory { ProvidersXmlProvider(context = get()) }
    factory { MiniDnsRecordsResolver() }
    factory { ProvidersXmlDiscovery(xmlProvider = get(), oAuthConfigurationProvider = get(), dnsRecordsResolver = get()) }
}
