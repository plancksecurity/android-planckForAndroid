package security.planck.notification

import com.fsck.k9.Account
import foundation.pEp.jniadapter.Identity


class GroupMailSignal(
    val groupIdentity: Identity,
    val senderIdentity: Identity,
    val accountUuid: String,
) {
    fun toGroupInvite(): GroupMailInvite = GroupMailInvite(
        groupIdentity.address, senderIdentity.address, accountUuid)

    companion object {
        @JvmStatic
        fun fromSignal(
            groupIdentity: Identity,
            senderIdentity: Identity,
            account: Account
        ): GroupMailSignal = GroupMailSignal(
            groupIdentity,
            senderIdentity,
            account.uuid
        )
    }
}