package com.fsck.k9

import androidx.lifecycle.LifecycleOwner
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import org.junit.Test
import org.koin.Koin
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext
import org.koin.test.dryRun
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@Config(sdk = [30])
class DependencyInjectionTest : K9RobolectricTest() {
    val lifecycleOwner = mock<LifecycleOwner> {
        on { lifecycle } doReturn mock()
    }

    @Test
    fun testDependencyTree() {
        StandAloneContext.startKoin(emptyList())
        Koin.logger = PrintLogger()

        dryRun {
            mapOf(
                    "lifecycleOwner" to lifecycleOwner,
                    "autocryptTransferView" to mock<AutocryptKeyTransferActivity>()
            )
        }
    }
}
