package security.pEp.mdm

import foundation.pEp.jniadapter.Pair

data class MediaKey(
    val addressPattern: String,
    val fpr: String,
) {
    fun toPair(): Pair<String, String> = Pair(addressPattern, fpr)
}
