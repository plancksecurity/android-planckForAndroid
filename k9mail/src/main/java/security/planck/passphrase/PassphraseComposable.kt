package security.planck.passphrase

import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsck.k9.R

@Composable
fun PassphraseManagementDialogContent(
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
    finishApp: () -> Unit,
) {
    val minWidth = dimensionResource(id = R.dimen.key_import_floating_width)
    val paddingHorizontal = 24.dp
    val paddingTop = 24.dp
    val paddingBottom = 8.dp
    val viewModelState = viewModel.state.observeAsState()

    Column(
        //horizontalAlignment = Alignment.CenterHorizontally,
        //verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .widthIn(min = minWidth)
            .padding(horizontal = paddingHorizontal, vertical = 0.dp)
            .padding(top = paddingTop, bottom = paddingBottom)
    ) {
        WizardToolbar(
            title = stringResource(
                id =
                if (viewModel.mode == PassphraseDialogMode.MANAGE)
                    R.string.passphrase_management_dialog_title
                else
                    R.string.passphrase_unlock_dialog_title
            )
        )

        when (val state = viewModelState.value) {
            PassphraseMgmtState.Idle -> {
                CenteredCircularProgressIndicatorWithText(text = stringResource(id = R.string.message_list_loading))
            }

            is PassphraseMgmtState.CoreError -> {}
            PassphraseMgmtState.Loading -> {
                CenteredCircularProgressIndicatorWithText(text = stringResource(id = R.string.message_list_loading))
            }

            PassphraseMgmtState.TooManyFailedAttempts -> {
                Text(
                    text = stringResource(id = R.string.passphrase_unlock_dialog_too_many_failed_attempts),
                    fontFamily = FontFamily.SansSerif,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                    modifier = Modifier.padding(vertical = 32.dp)
                )
                // buttons at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextActionButton(
                        text = stringResource(id = R.string.close),
                        textColor = colorResource(
                            id = R.color.colorAccent
                        ),
                        enabled = true,
                        onClick = finishApp,
                    )
                }
            }

            is PassphraseMgmtState.UnlockingPassphrases -> {
                if (state.loading.value == null) {
                    PassphraseUnlockingList(
                        viewModel = viewModel,
                        passwordStates = state.passwordStates
                    )
                    state.errorType.value?.let { errorType ->
                        val string = when (errorType) {
                            PassphraseUnlockErrorType.WRONG_FORMAT -> R.string.passphrase_wrong_input_feedback
                            PassphraseUnlockErrorType.WRONG_PASSPHRASE -> R.string.passhphrase_body_wrong_passphrase
                            PassphraseUnlockErrorType.CORE_ERROR -> R.string.error_happened_restart_app
                        }
                        Text(
                            text = stringResource(id = string),
                            fontFamily = FontFamily.Default,
                            color = colorResource(id = R.color.error_text_color),
                            style = MaterialTheme.typography.caption,
                        )
                    }

                    // buttons at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        //TextActionButton(
                        //    text = stringResource(id = R.string.cancel_action),
                        //    textColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                        //    onClick = onCancel
                        //)
                        TextActionButton(
                            text = stringResource(id = R.string.pep_confirm_trustwords),
                            textColor = colorResource(
                                id = R.color.colorAccent
                            ),
                            enabled = state.errorType.value == null
                        ) {
                            viewModel.unlockKeysWithPassphrase(state.passwordStates.toList())
                        }
                    }
                } else {
                    val string = when (val loadingState = state.loading.value!!) {
                        PassphraseUnlockLoading.Processing -> stringResource(id = R.string.message_list_loading)
                        is PassphraseUnlockLoading.WaitAfterFailedAttempt -> stringResource(
                            id = R.string.passphrase_unlock_dialog_wait_after_failed_attempt,
                            loadingState.seconds
                        )
                    }
                    CenteredCircularProgressIndicatorWithText(text = string)
                }
            }

            PassphraseMgmtState.Dismiss -> {
                SideEffect {
                    dismiss()
                }
            }

            else -> {

            }
        }
    }
}

@Composable
fun WizardToolbar(title: String) {
    val textSize = 24.sp
    val fontWeight = FontWeight.Bold

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = textSize,
                color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                fontWeight = fontWeight,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
            )
        },
        backgroundColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // ?attr/actionBarSize default height
    )
}

@Composable
fun PassphraseManagementList(accountUsesPassphraseList: List<AccountUsesPassphrase>) {
    LazyColumn {
        itemsIndexed(accountUsesPassphraseList) { index, accountUsesPassphrase ->
            Column {
                var switchState: Boolean by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = accountUsesPassphrase.account.email)
                    Switch(
                        checked = accountUsesPassphrase.usesPassphrase,
                        onCheckedChange = { newState -> switchState = newState }
                    )
                }
                if (accountUsesPassphrase.usesPassphrase) {
                    //PasswordInputField()
                    if (index == 0) {
                        var additionalSwitchState by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Use this passphrase for all accounts")
                            Switch(
                                checked = additionalSwitchState,
                                onCheckedChange = { additionalSwitchState = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassphraseUnlockingList(
    viewModel: PassphraseManagementViewModel,
    passwordStates: List<TextFieldState>,
) {

    LazyColumn {
        itemsIndexed(passwordStates) { index, state ->
            Column {
                Text(
                    text = state.email,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
                )
                PasswordInputField(passwordStates[index]) { viewModel.validateInput(it) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PasswordInputField(
    passwordState: TextFieldState,
    evaluateError: (TextFieldState) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val defaultColor = getColorFromAttr(
        colorRes = R.attr.defaultColorOnBackground
    )
    val errorColor = colorResource(id = R.color.error_text_color)
    val successColor = getColorFromAttr(
        colorRes = R.attr.colorAccent
    )
    val textColor = when (passwordState.errorState) {
        TextFieldState.ErrorStatus.NONE -> defaultColor
        TextFieldState.ErrorStatus.ERROR -> errorColor
        TextFieldState.ErrorStatus.SUCCESS -> successColor
    }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
fun TextActionButton(
    text: String,
    textColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonColors = ButtonDefaults.textButtonColors(
        contentColor = if (enabled) textColor else Color.Gray,
        disabledContentColor = Color.Gray
    )

    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(top = 8.dp)
    ) {
        Text(
            text = text, fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp
        )
    }
}

@Composable
fun CenteredCircularProgressIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = colorResource(id = R.color.colorAccent),
            strokeWidth = 4.dp,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
fun CenteredCircularProgressIndicatorWithText(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = colorResource(id = R.color.colorAccent),
            strokeWidth = 4.dp,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = text,
            fontFamily = FontFamily.SansSerif,
            color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun getColorFromAttr(@AttrRes colorRes: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    val theme = context.theme

    // Retrieve the text color from the XML style
    theme.resolveAttribute(colorRes, typedValue, true)
    return if (typedValue.resourceId != 0) {
        // Attribute is a reference to a color resource
        colorResource(id = typedValue.resourceId)
    } else {
        // Attribute is a direct color value
        Color(typedValue.data)
    }
}
