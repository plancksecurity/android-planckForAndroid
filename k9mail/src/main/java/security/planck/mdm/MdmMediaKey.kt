package security.planck.mdm

data class MdmMediaKey(
    val addressPattern: String,
    val fpr: String,
    val material: String,
) {
    fun toMediaKey(): MediaKey = MediaKey(addressPattern, fpr)
}
