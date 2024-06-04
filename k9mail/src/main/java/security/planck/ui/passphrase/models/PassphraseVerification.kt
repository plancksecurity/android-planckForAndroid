package security.planck.ui.passphrase.models

enum class PassphraseVerificationStatus {
    WRONG_FORMAT, WRONG_PASSPHRASE, NEW_PASSPHRASE_DOES_NOT_MATCH, CORE_ERROR, NONE, SUCCESS;

    val isError: Boolean get() = this != NONE && this != SUCCESS
    val isPersistentError: Boolean get() = this == CORE_ERROR
}

enum class PassphraseConfirmationStatus {
    SUCCESS, MISMATCH;
}