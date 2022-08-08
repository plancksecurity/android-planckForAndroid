package com.fsck.k9.pEp.infrastructure

import com.fsck.k9.K9
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent
import com.fsck.k9.pEp.infrastructure.components.DaggerTestApplicationComponent

class TestK9 : K9() {
    override fun createApplicationComponent(): ApplicationComponent {
        return DaggerTestApplicationComponent
            .factory()
            .create(this)
    }
}
