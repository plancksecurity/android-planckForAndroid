package security.planck.passphrase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.common.compose.input.PasswordInputField
import security.planck.ui.common.compose.progress.CenteredCircularProgressIndicatorWithText
import security.planck.ui.common.compose.toolbar.WizardToolbar

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
                    val defaultColor = getColorFromAttr(
                        colorRes = R.attr.defaultColorOnBackground
                    )
                    val errorColor = colorResource(id = R.color.error_text_color)
                    val successColor = getColorFromAttr(
                        colorRes = R.attr.colorAccent
                    )
                    PassphraseUnlockingList(
                        viewModel = viewModel,
                        passwordStates = state.passwordStates,
                        defaultColor, successColor, errorColor
                    )
                    val errorType = state.status.value
                    if (errorType.isError) {
                        val string = when (errorType) {
                            PassphraseUnlockStatus.WRONG_FORMAT -> R.string.passphrase_wrong_input_feedback
                            PassphraseUnlockStatus.WRONG_PASSPHRASE -> R.string.passhphrase_body_wrong_passphrase
                            PassphraseUnlockStatus.CORE_ERROR -> R.string.error_happened_restart_app
                            else -> 0
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
                        TextActionButton(
                            text = stringResource(id = R.string.pep_confirm_trustwords),
                            textColor = colorResource(
                                id = R.color.colorAccent
                            ),
                            enabled = state.status.value == PassphraseUnlockStatus.SUCCESS
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
    defaultColor: Color,
    successColor: Color,
    errorColor: Color,
) {

    LazyColumn {
        itemsIndexed(passwordStates) { index, state ->
            Column {
                Text(
                    text = state.email,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
                )

                val passwordState = passwordStates[index]
                val textColor = when (passwordState.errorState) {
                    TextFieldState.ErrorStatus.NONE -> defaultColor
                    TextFieldState.ErrorStatus.ERROR -> errorColor
                    TextFieldState.ErrorStatus.SUCCESS -> successColor
                }
                val statusIcon = when (passwordState.errorState) {
                    TextFieldState.ErrorStatus.NONE -> Icons.Filled.Close
                    TextFieldState.ErrorStatus.ERROR -> Icons.Filled.Close
                    TextFieldState.ErrorStatus.SUCCESS -> Icons.Filled.CheckBox
                }
                val statusIconDescription = when (passwordState.errorState) {
                    TextFieldState.ErrorStatus.NONE -> 0
                    TextFieldState.ErrorStatus.ERROR -> R.string.passphrase_unlock_dialog_wrong_passhprase_status_desc
                    TextFieldState.ErrorStatus.SUCCESS -> R.string.passphrase_unlock_dialog_correct_passphrase_status_desc
                }
                PassphraseUnlockRow(
                    passwordState = passwordState,
                    textColor = textColor,
                    errorColor = errorColor,
                    defaultColor = defaultColor,
                    statusIcon = statusIcon,
                    statusIconDescription = statusIconDescription,
                    validateInput = viewModel::validateInput
                )
            }
        }
    }
}

@Composable
private fun PassphraseUnlockRow(
    passwordState: TextFieldState,
    textColor: Color,
    errorColor: Color,
    defaultColor: Color,
    validateInput: (TextFieldState) -> Unit,
    statusIcon: ImageVector,
    statusIconDescription: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        PasswordInputField(
            passwordState,
            textColor,
            errorColor,
            defaultColor,
            validateInput,
            modifier = Modifier.weight(1f)
        )
        if (passwordState.errorState != TextFieldState.ErrorStatus.NONE) {
            Icon(
                imageVector = statusIcon,
                stringResource(id = statusIconDescription),
                tint = textColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
