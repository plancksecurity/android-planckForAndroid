package security.planck.passphrase

import security.planck.passphrase.extensions.isValidPassphrase
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

class PassphraseFormatValidator @Inject constructor() {
    fun validatePassphrase(text: String): TextFieldStateContract.ErrorStatus {
        return when {
            text.isValidPassphrase() -> {
                TextFieldStateContract.ErrorStatus.SUCCESS
            }

            text.isEmpty() -> {
                TextFieldStateContract.ErrorStatus.NONE
            }

            else -> {
                TextFieldStateContract.ErrorStatus.ERROR
            }
        }
    }

    fun validateNewPassphrase(text: String): TextFieldStateContract.ErrorStatus {
        return when {
            text.isEmpty() || text.isValidPassphrase() -> {
                TextFieldStateContract.ErrorStatus.SUCCESS
            }

            else -> {
                TextFieldStateContract.ErrorStatus.ERROR
            }
        }
    }

    fun verifyNewPassphrase(
        newPassphrase: String,
        newPassphraseVerification: String
    ): TextFieldStateContract.ErrorStatus {
        return when {
            newPassphraseVerification == newPassphrase -> {
                TextFieldStateContract.ErrorStatus.SUCCESS
            }

            else -> {
                TextFieldStateContract.ErrorStatus.ERROR
            }
        }
    }
}