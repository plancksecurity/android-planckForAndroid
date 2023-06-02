package security.planck.mdm

import com.fsck.k9.mail.AuthType

enum class AuthType {
    PLAIN,
    CRAM_MD5,
    EXTERNAL,
    XOAUTH2;

    fun toAppAuthType(): AuthType = AuthType.valueOf(this.toString())
}

fun AuthType.toMdmAuthType(): security.planck.mdm.AuthType =
    security.planck.mdm.AuthType.valueOf(this.toString())
