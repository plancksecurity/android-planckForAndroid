package com.fsck.k9.activity.compose

interface RecipientSelectViewContract {
    val isAlwaysUnsecure: Boolean

    fun hasRecipient(recipient: Recipient): Boolean
}
