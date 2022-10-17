package security.pEp.mdm

data class MailSettings @JvmOverloads constructor(
    var email: String? = null,
    var oAuthProvider: String? = null,
    var incoming: MailIncomingOutgoingSettings? = null,
    var outgoing: MailIncomingOutgoingSettings? = null,
)

data class MailIncomingOutgoingSettings @JvmOverloads constructor(
    var server: String? = null,
    var securityType: String? = null,
    var port: Int? = null,
    var userName: String? = null,
    var authType: String? = null,
)

data class FolderSettings @JvmOverloads constructor(
    var archiveFolder: String? = null,
    var draftsFolder: String? = null,
    var sentFolder: String? = null,
    var spamFolder: String? = null,
    var trashFolder: String? = null,
)

data class CompositionSettings @JvmOverloads constructor(
    var senderName: String? = null,
    var signature: String? = null,
    var useSignature: Boolean? = null,
    var signatureBefore: Boolean? = null,
)

data class TestMdmMediaKey @JvmOverloads constructor(
    var pattern: String? = null,
    var fpr: String? = null,
    var material: String? = null,
)

data class TestMdmExtraKey @JvmOverloads constructor(
    var fpr: String? = null,
    var material: String? = null,
)
