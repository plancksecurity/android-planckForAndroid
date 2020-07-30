package com.fsck.k9.ui.contacts

import com.fsck.k9.mail.Address
import java.util.Locale
import javax.inject.Inject

private val EXTRACT_LETTER_PATTERN = Regex("\\p{L}\\p{M}*")
private const val FALLBACK_CONTACT_LETTER = "?"

class ContactLetterExtractor @Inject constructor(){
    fun extractContactLetter(address: Address): String {
        val displayName = address.personal ?: address.address

        val matchResult = EXTRACT_LETTER_PATTERN.find(displayName)
        val result = matchResult?.value?.toUpperCase(Locale.ROOT)

        return result ?: FALLBACK_CONTACT_LETTER
    }
}
