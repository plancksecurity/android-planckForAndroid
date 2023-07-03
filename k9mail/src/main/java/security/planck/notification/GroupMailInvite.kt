package security.planck.notification


import android.os.Parcelable
import com.fsck.k9.Account
import com.fsck.k9.notification.NotificationReference
import foundation.pEp.jniadapter.Identity
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupMailInvite(
    val groupAddress: String,
    val senderAddress: String,
    val accountUuid: String,
) : NotificationReference, Parcelable {
    companion object {
        @JvmStatic
        fun fromSignal(
            groupIdentity: Identity,
            senderIdentity: Identity,
            account: Account
        ): GroupMailInvite = GroupMailInvite(
            groupIdentity.address,
            senderIdentity.address,
            account.uuid
        )
    }
}