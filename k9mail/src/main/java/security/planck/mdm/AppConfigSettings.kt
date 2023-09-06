package security.planck.mdm


data class ManageableSettingMdmEntry<TYPE>(
    val value: TYPE,
    val locked: Boolean,
) {
    fun toManageableSetting(): ManageableSetting<TYPE> =
        ManageableSetting(value = value, locked = locked)
}
