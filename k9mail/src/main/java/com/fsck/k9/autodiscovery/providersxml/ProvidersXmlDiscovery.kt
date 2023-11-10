package com.fsck.k9.autodiscovery.providersxml

import android.content.res.XmlResourceParser
import android.net.Uri
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.autodiscovery.api.ConnectionSettingsDiscovery
import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.oauth.OAuthConfigurationProvider
import org.xmlpull.v1.XmlPullParser
import security.planck.provisioning.ProvisioningSettings
import timber.log.Timber
import javax.inject.Inject

class ProvidersXmlDiscovery @Inject constructor(
    private val xmlProvider: ProvidersXmlProvider,
    private val oAuthConfigurationProvider: OAuthConfigurationProvider,
    private val dnsRecordsResolver: MiniDnsRecordsResolver,
    private val provisioningSettings: ProvisioningSettings,
) : ConnectionSettingsDiscovery {

    private val provisionedProvider: Provider?
        get() = provisioningSettings.accountsProvisionList.firstOrNull()?.provisionedMailSettings?.let { mailSettings ->
            Provider(
                incomingUriTemplate = mailSettings.incoming.toSeverUriTemplate(outgoing = false),
                incomingUsernameTemplate = mailSettings.incoming.userName!!,
                outgoingUriTemplate = mailSettings.outgoing.toSeverUriTemplate(outgoing = true),
                outgoingUsernameTemplate = mailSettings.outgoing.userName!!
            )
        }

    override fun discover(email: String, oAuthProviderType: OAuthProviderType?): DiscoveryResults? {
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null

        val provider = provisionedProvider
            ?: findProviderForDomain(domain)
            ?: findProviderForDomain(dnsRecordsResolver.getRealOrFallbackDomain(domain, oAuthProviderType))
            ?: return null

        val incomingSettings = provider.toIncomingServerSettings(email) ?: return null
        val outgoingSettings = provider.toOutgoingServerSettings(email) ?: return null
        return DiscoveryResults(listOf(incomingSettings), listOf(outgoingSettings))
            .addOAuthIfPossible(oAuthProviderType)
    }

    private fun findProviderForDomain(domain: String): Provider? {
        return try {
            xmlProvider.getXml().use { xml ->
                parseProviders(xml, domain)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while trying to load provider settings.")
            null
        }
    }

    private fun parseProviders(xml: XmlResourceParser, domain: String): Provider? {
        do {
            val xmlEventType = xml.next()
            if (xmlEventType == XmlPullParser.START_TAG && xml.name == "provider") {
                val providerDomain = xml.getAttributeValue(null, "domain")
                if (domain.equals(providerDomain, ignoreCase = true)) {
                    val provider = parseProvider(xml)
                    if (provider != null) return provider
                }
            }
        } while (xmlEventType != XmlPullParser.END_DOCUMENT)

        return null
    }

    private fun parseProvider(xml: XmlResourceParser): Provider? {
        var incomingUriTemplate: String? = null
        var incomingUsernameTemplate: String? = null
        var outgoingUriTemplate: String? = null
        var outgoingUsernameTemplate: String? = null

        do {
            val xmlEventType = xml.next()
            if (xmlEventType == XmlPullParser.START_TAG) {
                when (xml.name) {
                    "incoming" -> {
                        incomingUriTemplate = xml.getAttributeValue(null, "uri")
                        incomingUsernameTemplate = xml.getAttributeValue(null, "username")
                    }
                    "outgoing" -> {
                        outgoingUriTemplate = xml.getAttributeValue(null, "uri")
                        outgoingUsernameTemplate = xml.getAttributeValue(null, "username")
                    }
                }
            }
        } while (!(xmlEventType == XmlPullParser.END_TAG && xml.name == "provider"))

        return if (incomingUriTemplate != null && incomingUsernameTemplate != null && outgoingUriTemplate != null &&
            outgoingUsernameTemplate != null
        ) {
            Provider(incomingUriTemplate, incomingUsernameTemplate, outgoingUriTemplate, outgoingUsernameTemplate)
        } else {
            null
        }
    }

    private fun Provider.toIncomingServerSettings(email: String): DiscoveredServerSettings? {
        val user = EmailHelper.getLocalPartFromEmailAddress(email) ?: return null
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null

        val username = incomingUsernameTemplate.fillInUsernameTemplate(email, user, domain)

        val security = when {
            incomingUriTemplate.startsWith("imap+ssl") -> ConnectionSecurity.SSL_TLS_REQUIRED
            incomingUriTemplate.startsWith("imap+tls") -> ConnectionSecurity.STARTTLS_REQUIRED
            else -> error("Connection security required")
        }

        val uri = Uri.parse(incomingUriTemplate)
        val host = uri.host ?: error("Host name required")
        val port = if (uri.port == -1) {
            if (security == ConnectionSecurity.STARTTLS_REQUIRED) 143 else 993
        } else {
            uri.port
        }

        return DiscoveredServerSettings(ServerSettings.Type.IMAP, host, port, security, AuthType.PLAIN, username)
    }

    private fun Provider.toOutgoingServerSettings(email: String): DiscoveredServerSettings? {
        val user = EmailHelper.getLocalPartFromEmailAddress(email) ?: return null
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null

        val username = outgoingUsernameTemplate.fillInUsernameTemplate(email, user, domain)

        val security = when {
            outgoingUriTemplate.startsWith("smtp+ssl") -> ConnectionSecurity.SSL_TLS_REQUIRED
            outgoingUriTemplate.startsWith("smtp+tls") -> ConnectionSecurity.STARTTLS_REQUIRED
            else -> error("Connection security required")
        }

        val uri = Uri.parse(outgoingUriTemplate)
        val host = uri.host ?: error("Host name required")
        val port = if (uri.port == -1) {
            if (security == ConnectionSecurity.STARTTLS_REQUIRED) 587 else 465
        } else {
            uri.port
        }

        return DiscoveredServerSettings(ServerSettings.Type.SMTP, host, port, security, AuthType.PLAIN, username)
    }

    private fun String.fillInUsernameTemplate(email: String, user: String, domain: String): String {
        return this.replace("\$email", email).replace("\$user", user).replace("\$domain", domain)
    }

    private fun DiscoveryResults.addOAuthIfPossible(
        oAuthProviderType: OAuthProviderType?,
    ): DiscoveryResults {
        return DiscoveryResults(
            this.incoming.map { it.addOAuthIfPossible(oAuthProviderType) },
            this.outgoing.map { it.addOAuthIfPossible(oAuthProviderType) }
        )
    }

    private fun DiscoveredServerSettings.addOAuthIfPossible(
        oAuthProviderType: OAuthProviderType?,
    ): DiscoveredServerSettings {
        val authType = if (oAuthProviderType != null || oAuthConfigurationProvider.isGoogle(host)) {
            AuthType.XOAUTH2
        } else {
            AuthType.PLAIN
        }
        return this.copy(authType = authType)
    }

    internal data class Provider(
        val incomingUriTemplate: String,
        val incomingUsernameTemplate: String,
        val outgoingUriTemplate: String,
        val outgoingUsernameTemplate: String
    )
}
