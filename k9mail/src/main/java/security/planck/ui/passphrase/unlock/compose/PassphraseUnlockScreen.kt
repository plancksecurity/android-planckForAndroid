package security.planck.ui.passphrase.unlock.compose

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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import security.planck.ui.passphrase.models.TextFieldStateContract.ErrorStatus
import security.planck.ui.passphrase.unlock.PassphraseUnlockLoading
import security.planck.ui.passphrase.unlock.PassphraseUnlockState
import security.planck.ui.passphrase.unlock.PassphraseUnlockViewModel

@Composable
fun PassphraseUnlockDialogContent(
    viewModel: PassphraseUnlockViewModel,
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
                id = R.string.passphrase_unlock_dialog_title
            )
        )
        viewModelState.value?.let { state ->
            RenderState(state, finishApp, viewModel, dismiss)
        }
    }
}

@Composable
private fun RenderState(
    state: PassphraseUnlockState,
    finishApp: () -> Unit,
    viewModel: PassphraseUnlockViewModel,
    dismiss: () -> Unit
) {
    when (state) {
        PassphraseUnlockState.TooManyFailedAttempts -> {
            RenderTooManyFailedAttempts(finishApp)
        }

        is PassphraseUnlockState.UnlockingPassphrases -> {
            RenderUnlockingPassphrases(
                state,
                validateInput = viewModel::validateInput,
                onConfirm = { viewModel.unlockKeysWithPassphrase(state.passwordStates.toList()) }
            )
        }

        PassphraseUnlockState.Dismiss -> {
            SideEffect {
                dismiss()
            }
        }
    }
}

@Composable
private fun RenderUnlockingPassphrases(
    state: PassphraseUnlockState.UnlockingPassphrases,
    validateInput: (TextFieldStateContract) -> Unit,
    onConfirm: () -> Unit,
) {
    if (state.loading.value == null) {
        RenderInputScreen(
            state,
            validateInput = validateInput,
            onConfirm = onConfirm
        )
    } else {
        RenderLoadingScreen(state)
    }
}

@Composable
private fun RenderLoadingScreen(state: PassphraseUnlockState.UnlockingPassphrases) {
    val string = when (val loadingState = state.loading.value!!) {
        PassphraseUnlockLoading.Processing -> stringResource(id = R.string.message_list_loading)
        is PassphraseUnlockLoading.WaitAfterFailedAttempt -> stringResource(
            id = R.string.passphrase_unlock_dialog_wait_after_failed_attempt,
            loadingState.seconds
        )
    }
    CenteredCircularProgressIndicatorWithText(text = string)
}

@Composable
private fun RenderInputScreen(
    state: PassphraseUnlockState.UnlockingPassphrases,
    validateInput: (TextFieldStateContract) -> Unit,
    onConfirm: () -> Unit,
) {
    val defaultColor = getColorFromAttr(
        colorRes = R.attr.defaultColorOnBackground
    )
    val errorColor = colorResource(id = R.color.error_text_color)
    val successColor = getColorFromAttr(
        colorRes = R.attr.colorAccent
    )
    PassphraseValidationList(
        passwordStates = state.passwordStates,
        defaultColor, successColor, errorColor,
        validateInput = validateInput,
    )
    val errorType = state.status.value
    if (errorType.isError) {
        val string = when (errorType) {
            PassphraseVerificationStatus.WRONG_FORMAT -> R.string.passphrase_wrong_input_feedback
            PassphraseVerificationStatus.WRONG_PASSPHRASE -> R.string.passhphrase_body_wrong_passphrase
            PassphraseVerificationStatus.CORE_ERROR -> R.string.error_happened_restart_app
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
    ButtonsRow(state, onConfirm = onConfirm)
}

@Composable
private fun ButtonsRow(
    state: PassphraseUnlockState.UnlockingPassphrases,
    onConfirm: () -> Unit,
) {
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
            enabled = state.status.value == PassphraseVerificationStatus.SUCCESS,
            onClick = onConfirm,
        )
    }
}

@Composable
private fun RenderTooManyFailedAttempts(finishApp: () -> Unit) {
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

@Composable
fun PassphraseValidationList(
    passwordStates: List<AccountTextFieldState>,
    defaultColor: Color,
    successColor: Color,
    errorColor: Color,
    validateInput: (TextFieldStateContract) -> Unit,
) {
    Column {
        passwordStates.forEachIndexed { index, state ->
            Column {
                Text(
                    text = state.email,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
                )

                val passwordState = passwordStates[index]
                PassphraseValidationRow(
                    passwordState = passwordState,
                    errorColor = errorColor,
                    successColor = successColor,
                    defaultColor = defaultColor,
                    validateInput = validateInput
                )
            }
        }
    }
}

@Composable
fun PassphraseValidationRow(
    passwordState: TextFieldStateContract,
    errorColor: Color,
    successColor: Color,
    defaultColor: Color,
    validateInput: (TextFieldStateContract) -> Unit,
) {
    val textColor = when (passwordState.errorState) {
        ErrorStatus.NONE -> defaultColor
        ErrorStatus.ERROR -> errorColor
        ErrorStatus.SUCCESS -> successColor
    }
    val statusIcon = when (passwordState.errorState) {
        ErrorStatus.NONE -> Icons.Filled.Close
        ErrorStatus.ERROR -> Icons.Filled.Close
        ErrorStatus.SUCCESS -> Icons.Filled.CheckBox
    }
    val statusIconDescription = when (passwordState.errorState) {
        ErrorStatus.NONE -> 0
        ErrorStatus.ERROR -> R.string.passphrase_unlock_dialog_wrong_passhprase_status_desc
        ErrorStatus.SUCCESS -> R.string.passphrase_unlock_dialog_correct_passphrase_status_desc
    }

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
        if (passwordState.errorState != ErrorStatus.NONE) {
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
