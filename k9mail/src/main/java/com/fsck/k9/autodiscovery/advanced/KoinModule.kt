package com.fsck.k9.autodiscovery.advanced

import org.koin.dsl.module.applicationContext

val autodiscoveryAdvancedModule = applicationContext {
    factory { AdvancedSettingsDiscovery(
        providersXmlDiscovery = get(),
        thunderbirdDiscovery = get(),
    ) }
}
