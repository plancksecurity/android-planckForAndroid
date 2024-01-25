package security.planck.ui.mdm

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import security.planck.provisioning.toSimpleMailSettings
import timber.log.Timber
import javax.inject.Inject

class MdmSettingsFeedbackPresenter @Inject constructor(
    private val preferences: Preferences,
    private val k9: K9,
) {
    private lateinit var view: MdmSettingsFeedbackView

    fun initialize(view: MdmSettingsFeedbackView) {
        this.view = view
    }

    fun displaySettings() {
        view.displaySettings(getSettingsText())
    }

    private fun getSettingsText(): String {
        val account = preferences.accounts.first()
        val incomingMailSettings = kotlin.runCatching {
            RemoteStore.decodeStoreUri(account.storeUri).toSimpleMailSettings()
        }.onFailure { Timber.e(it) }.getOrNull()
        val outgoingMailSettings = kotlin.runCatching {
            Transport.decodeTransportUri(account.transportUri).toSimpleMailSettings()
        }.onFailure { Timber.e(it) }.getOrNull()

        return """
            <p><b>MDM enabled:</b> ${k9.isRunningOnWorkProfile}</p>
            <p><b>planck privacy protection:</b> ${account.isPlanckPrivacyProtected()}</p>
            <br/>
            <b>Extra keys:</b><p> 
            ${
            if (K9.getMasterKeys().isEmpty())
                "<p>&emsp;No keys</p>"
            else
                K9.getMasterKeys()
                    .joinToString("\n")
                    { "<p>&emsp;<b>Fingerprint:</b> $it</p>" }
        }
            <br/>
            <p><b>Media keys:</b></p>
            ${
            if (K9.getMediaKeys() == null)
                "<p>&emsp;No keys</p>"
            else
                K9.getMediaKeys().mapIndexed { index, mediaKey ->
                    "<p><b>&emsp;Media key ${index + 1}:</b></p>" +
                            "\n<p>&emsp;&emsp;<b>Address pattern:</b> ${mediaKey.addressPattern}</p>" +
                            "\n<p>&emsp;&emsp;<b>Fingerprint:</b> ${mediaKey.fpr}</p>"
                }.joinToString("\n")
        }
            <br/>
            <p><b>Unsecure delivery warning:</b> ${K9.isPlanckForwardWarningEnabled()}</p>
            <p><b>PlanckProvider sync folder:</b> ${K9.isUsingpEpSyncFolder()}</p>
            <p><b>Enable debug logging:</b> ${K9.isDebug()}</p>
            <p><b>Enable echo protocol:</b> ${K9.isEchoProtocolEnabled()}</p>
            <p><b>Local folder size:</b> ${account.displayCount}</p>
            <p><b>Max folders to check with push:</b> ${account.maxPushFolders}</p>
            <p><b>Account name:</b> ${account.description}</p>
            <br/>
            <p><b>Composition defaults:</b></p>
                <p><b>&emsp;Sender name:</b> ${account.name}</p>
                <p><b>&emsp;Use Signature:</b> ${account.signatureUse}</p>
                <p><b>&emsp;Signature:</b> ${account.signature}</p>
                <p><b>&emsp;Before quoted message:</b> ${account.isSignatureBeforeQuotedText}</p>
            <br/>
            <p><b>Quote message when replying:</b> ${account.isDefaultQuotedTextShown}</p>
            <br/>
            <p><b>Default folders:</b></p>
                <p><b>&emsp;Archive folder:</b> ${account.archiveFolderName}</p>
                <p><b>&emsp;Drafts folder:</b> ${account.draftsFolderName}</p>
                <p><b>&emsp;Sent folder:</b> ${account.sentFolderName}</p>
                <p><b>&emsp;Spam folder:</b> ${account.spamFolderName}</p>
                <p><b>&emsp;Trash folder:</b> ${account.trashFolderName}</p>
            <br/>
            <p><b>Enable server search:</b> ${account.allowRemoteSearch()}</p>
            <p><b>Server search limit:</b> ${account.remoteSearchNumResults}</p>
            <p><b>PlanckProvider Sync this account:</b> ${account.isPlanckSyncEnabled}</p>
            <br/>
            <p><b>Mail settings:</b></p>
                <p><b>&emsp;Email address:</b> ${account.email}</p>
                <p><b>&emsp;OAuth Provider:</b> ${account.mandatoryOAuthProviderType}</p>
                <p><b>&emsp;Incoming server:</b></p>
                    <p><b>&emsp;&emsp;IMAP server:</b> ${incomingMailSettings?.server}</p>
                    <p><b>&emsp;&emsp;Security:</b> ${incomingMailSettings?.connectionSecurity}</p>
                    <p><b>&emsp;&emsp;Port:</b> ${incomingMailSettings?.port}</p>
                    <p><b>&emsp;&emsp;Username:</b> ${incomingMailSettings?.userName}</p>
                    <p><b>&emsp;&emsp;Authentication type:</b> ${incomingMailSettings?.authType}</p>                
                <p><b>&emsp;Outgoing server:</b></p>
                    <p><b>&emsp;&emsp;SMTP server:</b> ${outgoingMailSettings?.server}</p>
                    <p><b>&emsp;&emsp;Security:</b> ${outgoingMailSettings?.connectionSecurity}</p>
                    <p><b>&emsp;&emsp;Port:</b> ${outgoingMailSettings?.port}</p>
                    <p><b>&emsp;&emsp;Username:</b> ${outgoingMailSettings?.userName}</p>
                    <p><b>&emsp;&emsp;Authentication type:</b> ${outgoingMailSettings?.authType}</p>
            <br/>
            <p><b>Audit log data time retention:</b> ${k9.auditLogDataTimeRetention.value / 24 / 3600} days</p>
        """.trimIndent()
    }
}
