package com.fsck.k9.autodiscovery.advanced

import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.autodiscovery.thunderbird.ThunderbirdDiscovery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AdvancedSettingsDiscoveryTest {
    private val providersXmlDiscovery: ProvidersXmlDiscovery = mockk(relaxed = true)
    private val thunderbirdDiscovery: ThunderbirdDiscovery = mockk(relaxed = true)
    private val advancedSettingsDiscovery = AdvancedSettingsDiscovery(providersXmlDiscovery, thunderbirdDiscovery)

    @Test
    fun `AdvancedSettingsDiscovery sets provisioned settings using ProvidersXmlDiscovery`() {
        advancedSettingsDiscovery.setProvisionedSettings("it", "iu", "ot", "ou")

        verify { providersXmlDiscovery.setProvisionedSettings("it", "iu", "ot", "ou") }
    }

    @Test
    fun `AdvancedSettingsDiscovery uses ThunderbirdDiscovery if ProvidersXmlDiscovery fails`() {
        every { providersXmlDiscovery.discover(any()) }.returns(null)

        advancedSettingsDiscovery.discover("email")

        verify { providersXmlDiscovery.discover("email") }
        verify { thunderbirdDiscovery.discover("email") }
    }
}