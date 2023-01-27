package com.fsck.k9.autodiscovery.providersxml

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.EmptyApplication
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.oauth.OAuthConfiguration
import com.fsck.k9.oauth.OAuthConfigurationProvider
import com.fsck.k9.oauth.discover
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import security.pEp.provisioning.SimpleMailSettings

class ProvidersXmlDiscoveryTest : RobolectricTest() {
    private val xmlProvider = ProvidersXmlProvider(ApplicationProvider.getApplicationContext())
    private val providersXmlDiscovery = ProvidersXmlDiscovery(xmlProvider)
    private val oAuthConfigurationProvider = createOAuthConfigurationProvider()

    @Test
    fun `discover for Google domains should always return settings with XOAUTH2`() {
        val connectionSettings =
            providersXmlDiscovery.discover("user@gmail.com", oAuthConfigurationProvider, null)

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
            oAuthConfigurationProvider,
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
            providersXmlDiscovery.discover("user@outlook.com", oAuthConfigurationProvider, null)

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
            oAuthConfigurationProvider,
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
        providersXmlDiscovery.setProvisionedSettings(
            incomingUriTemplate = sampleSettings.toSeverUriTemplate(outgoing = false),
            incomingUsername = sampleSettings.userName!!,
            outgoingUriTemplate = sampleSettings.toSeverUriTemplate(outgoing = true),
            outgoingUsername = sampleSettings.userName!!
        )
        val connectionSettings =
            providersXmlDiscovery.discover("user@unknown.com", oAuthConfigurationProvider, null)

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
    fun discover_withUnknownDomain_shouldReturnNull() {
        val connectionSettings = providersXmlDiscovery.discover(
            "user@not.present.in.providers.xml.example"
        )

        assertThat(connectionSettings).isNull()
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
        security.pEp.mdm.AuthType.PLAIN
    )
}
