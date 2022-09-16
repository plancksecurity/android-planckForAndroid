package security.pEp.mdm

import foundation.pEp.jniadapter.Pair

data class MdmMediaKey(
    val addressPattern: String,
    val fpr: String,
) {
    fun toPair(): Pair<String, String> = Pair(addressPattern, fpr)
}
