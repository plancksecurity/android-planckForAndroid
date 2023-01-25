package com.fsck.k9.autodiscovery.thunderbird

import okhttp3.OkHttpClient
import org.koin.dsl.module.applicationContext

val autodiscoveryThunderbirdModule = applicationContext {
    factory { ThunderbirdAutoconfigUrlProvider() }
    factory { OkHttpClient() }
    factory { ThunderbirdAutoconfigFetcher(get()) }
    factory { ThunderbirdAutoconfigParser() }
    factory { ThunderbirdDiscovery(
        urlProvider = get(),
        fetcher = get(),
        parser = get(),
        oAuthConfigurationProvider = get()
    ) }
}
