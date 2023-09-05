package security.planck.mdm

import com.fsck.k9.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ManageableSettingMdmEntry<TYPE>(
        @SerialName("value") val value: TYPE,
        @SerialName("locked") val locked: Boolean = BuildConfig.IS_ENTERPRISE,
) {
    fun toManageableSetting(): ManageableSetting<TYPE> =
        ManageableSetting(value = value, locked = locked)
}
