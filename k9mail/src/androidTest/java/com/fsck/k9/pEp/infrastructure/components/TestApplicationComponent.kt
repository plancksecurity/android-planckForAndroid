package com.fsck.k9.pEp.infrastructure.components

import com.fsck.k9.pEp.infrastructure.modules.ApplicationModule
import com.fsck.k9.pEp.infrastructure.modules.TestRestrictionsProviderModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, TestRestrictionsProviderModule::class])
interface TestApplicationComponent : ApplicationComponent
