package security.pEp.mdm

import com.fsck.k9.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Deprecated("Only left in case we need to lock managed settings visible in UI")
data class ManageableSettingMdmEntry<TYPE>(
        @SerialName("value") val value: TYPE,
        @SerialName("locked") val locked: Boolean = BuildConfig.IS_ENTERPRISE,
) {
    fun toManageableSetting(): ManageableSetting<TYPE> =
        ManageableSetting(value = value, locked = locked)
}
