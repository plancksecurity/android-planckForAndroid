package com.fsck.k9.activity.compose

interface RecipientSelectViewContract {
    val isAlwaysUnsecure: Boolean
    val recipients: List<Recipient>

    fun hasRecipient(recipient: Recipient): Boolean

    fun removeRecipient(recipient: Recipient)

    fun showUncompletedError()

    fun hasUncompletedRecipients(): Boolean

    fun showNoRecipientsError()

    fun tryPerformCompletion(): Boolean

    fun addRecipient(recipient: Recipient)

    fun restoreFirstRecipientTruncation()

    fun resetCollapsedViewIfNeeded()

    fun updateRecipients(recipients: List<RatedRecipient>)

    fun showAlternatesPopup(data: List<RatedRecipient>)
}
