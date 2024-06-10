package security.planck.ui.passphrase.models

interface TextFieldStateContract {
    val text: String
    val errorStatus: ErrorStatus
    fun copyWith(newText: String = this.text, errorStatus: ErrorStatus = this.errorStatus): TextFieldStateContract {
        return when(this) {
            is TextFieldState -> copy(text = newText, errorStatus = errorStatus)
            is AccountTextFieldState -> copy(text = newText, errorStatus = errorStatus)
            else -> error("unknown type")
        }
    }

    enum class ErrorStatus {
        NONE, ERROR, SUCCESS
    }
}

data class TextFieldState(
    override val text: String = "",
    override val errorStatus: TextFieldStateContract.ErrorStatus = TextFieldStateContract.ErrorStatus.NONE,
): TextFieldStateContract

data class AccountTextFieldState(
    val email: String,
    override val text: String = "",
    override val errorStatus: TextFieldStateContract.ErrorStatus = TextFieldStateContract.ErrorStatus.NONE,
): TextFieldStateContract