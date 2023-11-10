package com.fsck.k9.autodiscovery.providersxml

import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import security.planck.network.UrlChecker
import security.planck.provisioning.AccountMailSettingsProvision
import security.planck.provisioning.ProvisioningSettings
import security.planck.provisioning.SimpleMailSettings

private const val OUTLOOK_PROTECTION_DOMAIN = "mail.protection.outlook.com"

class ProvidersXmlDiscoveryTest : RobolectricTest() {
    private val xmlProvider = ProvidersXmlProvider(ApplicationProvider.getApplicationContext())
    private val oAuthConfigurationProvider = createOAuthConfigurationProvider()
    private val dnsRecordsResolver: MiniDnsRecordsResolver = mockk(relaxed = true)
    private val preferences: Preferences = mockk()
    private val urlChecker: UrlChecker = mockk()
    private val provisioningSettings = ProvisioningSettings(preferences, urlChecker)
    private val providersXmlDiscovery = ProvidersXmlDiscovery(xmlProvider, oAuthConfigurationProvider, dnsRecordsResolver, provisioningSettings)

    @Test
    fun `discover for Google domains should always return settings with XOAUTH2`() {
        val connectionSettings =
            providersXmlDiscovery.discover("user@gmail.com", null)

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("imap.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
    }

    @Test
    fun `discover with mandatory oAuthProviderType as GOOGLE Should return settings with XOAUTH2`() {
        val connectionSettings = providersXmlDiscovery.discover(
            "user@gmail.com",
            OAuthProviderType.GOOGLE
        )

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("imap.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.gmail.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@gmail.com")
        }
    }

    @Test
    fun `discover for Microsoft domains should return settings with PLAIN if oAuthProviderType is null`() {
        val connectionSettings =
            providersXmlDiscovery.discover("user@outlook.com", null)

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("outlook.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@outlook.com")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.STARTTLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@outlook.com")
        }
    }

    @Test
    fun `discover for Microsoft domains should return settings with XOAUTH2 if oAuthProviderType is MICROSOFT`() {
        val connectionSettings = providersXmlDiscovery.discover(
            "user@outlook.com",
            OAuthProviderType.MICROSOFT
        )

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("outlook.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@outlook.com")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.STARTTLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.XOAUTH2)
            assertThat(username).isEqualTo("user@outlook.com")
        }
    }

    @Test
    fun `discover should take into account the provisioned settings`() {
        provisioningSettings.modifyOrAddAccountSettingsByAddress("user@unknown.com" ) {
            it.provisionedMailSettings = AccountMailSettingsProvision(sampleSettings, sampleSettings)
        }
        val connectionSettings =
            providersXmlDiscovery.discover("user@unknown.com", null)

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo(sampleSettings.server)
            assertThat(security).isEqualTo(sampleSettings.connectionSecurity)
            assertThat(authType).isEqualTo(sampleSettings.authType!!.toAppAuthType())
            assertThat(username).isEqualTo(sampleSettings.userName)
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo(sampleSettings.server)
            assertThat(security).isEqualTo(sampleSettings.connectionSecurity)
            assertThat(authType).isEqualTo(sampleSettings.authType!!.toAppAuthType())
            assertThat(username).isEqualTo(sampleSettings.userName)
        }
    }

    @Test
    fun `discover with unknownDomain should return null when dns discovery fails`() {
        stubDnsRecordsResolver(true)
        val connectionSettings = providersXmlDiscovery.discover(
            "user@not.present.in.providers.xml.example",
            null
        )

        assertThat(connectionSettings).isNull()
    }

    @Test
    fun `discover with unknownDomain should return Microsoft mail settings when dns returns Outlook protection domain`() {
        stubDnsRecordsResolver(false)
        val connectionSettings = providersXmlDiscovery.discover(
            "user@not.present.in.providers.xml.example",
            null
        )

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming.first()) {
            assertThat(host).isEqualTo("outlook.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@not.present.in.providers.xml.example")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.office365.com")
            assertThat(security).isEqualTo(ConnectionSecurity.STARTTLS_REQUIRED)
            assertThat(authType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@not.present.in.providers.xml.example")
        }
    }

    private fun stubDnsRecordsResolver(shouldFail: Boolean) {
        if (shouldFail) {
            val slot = slot<String>()
            every { dnsRecordsResolver.getRealOrFallbackDomain(capture(slot), any()) }.answers { slot.captured }
        } else {
            every { dnsRecordsResolver.getRealOrFallbackDomain(any(), any()) }.returns(OUTLOOK_PROTECTION_DOMAIN)
        }
    }

    private fun createOAuthConfigurationProvider(): OAuthConfigurationProvider {
        val googleConfig = OAuthConfiguration(
            clientId = "irrelevant",
            scopes = listOf("irrelevant"),
            authorizationEndpoint = "irrelevant",
            tokenEndpoint = "irrelevant",
            redirectUri = "irrelevant"
        )

        val microsoftConfig = OAuthConfiguration(
            clientId = "irrelevant",
            scopes = listOf("irrelevant"),
            authorizationEndpoint = "irrelevant",
            tokenEndpoint = "irrelevant",
            redirectUri = "irrelevant"
        )

        return OAuthConfigurationProvider(
            configurations = mapOf(
                listOf("imap.gmail.com", "smtp.gmail.com") to googleConfig,
            ),
            googleConfiguration = googleConfig,
            microsoftConfiguration = microsoftConfig
        )
    }

    private val sampleSettings = SimpleMailSettings(
        333,
        "server",
        ConnectionSecurity.STARTTLS_REQUIRED,
        "username",
        security.planck.mdm.AuthType.PLAIN
    )

}
