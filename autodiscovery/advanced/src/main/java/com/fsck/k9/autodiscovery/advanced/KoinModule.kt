package com.fsck.k9.autodiscovery.advanced

import com.fsck.k9.autodiscovery.providersxml.autodiscoveryProvidersXmlModule
import com.fsck.k9.autodiscovery.thunderbird.autodiscoveryThunderbirdModules
import org.koin.dsl.module.applicationContext

private val autodiscoveryAdvancedModule = applicationContext {
    factory { AdvancedSettingsDiscovery(
        providersXmlDiscovery = get(),
        thunderbirdDiscovery = get(),
    ) }
}

val autodiscoveryAdvancedModules = listOf(
    autodiscoveryAdvancedModule,
    autodiscoveryProvidersXmlModule,
) + autodiscoveryThunderbirdModules // TODO: replace with includes() when koin is updated
