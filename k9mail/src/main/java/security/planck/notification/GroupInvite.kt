package security.planck.notification


import foundation.pEp.jniadapter.Identity

data class GroupMailInvite(
    val groupAddress: String,
    val senderAddress: String
) {
    companion object {
        fun fromIdentities(groupIdentity: Identity, managerIdentity: Identity): GroupMailInvite {
            return GroupMailInvite(groupIdentity.address, managerIdentity.address)
        }
    }
}