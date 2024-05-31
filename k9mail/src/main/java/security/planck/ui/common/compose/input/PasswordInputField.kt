package security.planck.ui.common.compose.input

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.passphrase.TextFieldState

@Composable
fun PasswordInputField(
    passwordState: TextFieldState,
    textColor: Color,
    errorColor: Color,
    defaultColor: Color,
    evaluateError: (TextFieldState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = passwordState.textState,
        onValueChange = {
            passwordState.textState = it
            evaluateError(passwordState)
        },
        label = { Text(stringResource(id = R.string.passhphrase_input_hint)) },
        isError = passwordState.errorState == TextFieldState.ErrorStatus.ERROR,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            val description =
                if (passwordVisible) R.string.passphrase_unlock_dialog_hide_password else R.string.passphrase_unlock_dialog_show_password

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, stringResource(id = description), tint = defaultColor)
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = textColor,
            errorBorderColor = errorColor,
            errorLabelColor = errorColor,
            errorCursorColor = errorColor,
            unfocusedBorderColor = textColor,
            textColor = textColor,
            cursorColor = textColor,
            focusedLabelColor = textColor,
            unfocusedLabelColor = textColor,
        ),
        modifier = modifier
            .padding(vertical = 8.dp),
    )
}