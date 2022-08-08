package security.pEp.mdm

data class MailSettings @JvmOverloads constructor(
    val email: String? = null,
    val incoming: MailIncomingOutgoingSettings? = null,
    val outgoing: MailIncomingOutgoingSettings? = null,
)

data class MailIncomingOutgoingSettings @JvmOverloads constructor(
    val server: String? = null,
    val securityType: String? = null,
    val port: Int? = null,
    val userName: String? = null,
)

data class FolderSettings @JvmOverloads constructor(
    val archiveFolder: String? = null,
    val draftsFolder: String? = null,
    val sentFolder: String? = null,
    val spamFolder: String? = null,
    val trashFolder: String? = null,
)

data class CompositionSettings @JvmOverloads constructor(
    val senderName: String? = null,
    val signature: String? = null,
    val useSignature: Boolean? = null,
    val signatureBefore: Boolean? = null,
)
