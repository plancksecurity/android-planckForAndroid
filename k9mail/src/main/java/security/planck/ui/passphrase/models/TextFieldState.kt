package security.planck.ui.passphrase.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface TextFieldStateContract {
    var textState: String
    var errorState: ErrorStatus

    enum class ErrorStatus {
        NONE, ERROR, SUCCESS
    }
}

data class TextFieldState(
    private val text: String = "",
    private val errorStatus: TextFieldStateContract.ErrorStatus = TextFieldStateContract.ErrorStatus.NONE,
): TextFieldStateContract {
    override var textState by mutableStateOf(text)
    override var errorState by mutableStateOf(errorStatus)
}

data class AccountTextFieldState(
    val email: String,
    private val text: String = "",
    private val errorStatus: TextFieldStateContract.ErrorStatus = TextFieldStateContract.ErrorStatus.NONE,
): TextFieldStateContract {
    override var textState by mutableStateOf(text)
    override var errorState by mutableStateOf(errorStatus)
}