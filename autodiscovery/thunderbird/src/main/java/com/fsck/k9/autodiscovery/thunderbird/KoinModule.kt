package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.dnsrecords.dnsRecordsResolverModule
import okhttp3.OkHttpClient
import org.koin.dsl.module.applicationContext

private val autodiscoveryThunderbirdModule = applicationContext {
    factory { ThunderbirdAutoconfigUrlProvider(get()) }
    factory { OkHttpClient() }
    factory { ThunderbirdAutoconfigFetcher(get()) }
    factory { ThunderbirdAutoconfigParser() }
    factory { ThunderbirdDiscovery(
        urlProvider = get(),
        fetcher = get(),
        parser = get(),
    ) }
}

val autodiscoveryThunderbirdModules = listOf(
    autodiscoveryThunderbirdModule,
    dnsRecordsResolverModule
) // TODO: replace with includes() when koin is updated
