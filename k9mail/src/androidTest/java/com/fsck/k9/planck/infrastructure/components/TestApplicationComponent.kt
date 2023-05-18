package com.fsck.k9.planck.infrastructure.components

import com.fsck.k9.K9
import com.fsck.k9.planck.infrastructure.modules.ApplicationModule
import com.fsck.k9.planck.infrastructure.modules.TestRestrictionsProviderModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, TestRestrictionsProviderModule::class])
interface TestApplicationComponent : ApplicationComponent {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: K9
        ): TestApplicationComponent
    }
}
