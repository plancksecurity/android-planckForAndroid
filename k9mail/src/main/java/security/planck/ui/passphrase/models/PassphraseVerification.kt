package security.planck.ui.passphrase.models

enum class PassphraseVerificationStatus {
    WRONG_FORMAT, WRONG_PASSPHRASE, NEW_PASSPHRASE_DOES_NOT_MATCH, CORE_ERROR, NONE, SUCCESS;

    val isError: Boolean get() = this != NONE && this != SUCCESS

    /**
     * itemError is not fatal, and it's an error per account/mail address.
     */
    val isItemError: Boolean get() = this == WRONG_FORMAT || this == WRONG_PASSPHRASE || this == NEW_PASSPHRASE_DOES_NOT_MATCH
}

enum class PassphraseConfirmationStatus {
    SUCCESS, MISMATCH;
}