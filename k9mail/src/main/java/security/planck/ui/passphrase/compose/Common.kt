package security.planck.ui.passphrase.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.common.compose.input.PasswordInputField
import security.planck.ui.common.compose.progress.CenteredCircularProgressIndicatorWithText
import security.planck.ui.common.compose.toolbar.WizardToolbar
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseStateWithStatus
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract

@Composable
fun <VM : PassphraseViewModel> PassphraseScreen(
    viewModel: VM,
    title: String,
    content: @Composable (state: PassphraseState) -> Unit,
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
            title = title
        )
        viewModelState.value?.let { state ->
            content(state)
        }
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
                    color = defaultColor,
                    fontWeight = FontWeight.Bold
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
        TextFieldStateContract.ErrorStatus.NONE -> defaultColor
        TextFieldStateContract.ErrorStatus.ERROR -> errorColor
        TextFieldStateContract.ErrorStatus.SUCCESS -> successColor
    }
    val statusIcon = when (passwordState.errorState) {
        TextFieldStateContract.ErrorStatus.NONE -> Icons.Filled.Close
        TextFieldStateContract.ErrorStatus.ERROR -> Icons.Filled.Close
        TextFieldStateContract.ErrorStatus.SUCCESS -> Icons.Filled.CheckBox
    }
    val statusIconDescription = when (passwordState.errorState) {
        TextFieldStateContract.ErrorStatus.NONE -> 0
        TextFieldStateContract.ErrorStatus.ERROR -> R.string.passphrase_unlock_dialog_wrong_passhprase_status_desc
        TextFieldStateContract.ErrorStatus.SUCCESS -> R.string.passphrase_unlock_dialog_correct_passphrase_status_desc
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
        if (passwordState.errorState != TextFieldStateContract.ErrorStatus.NONE) {
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

@Composable
fun RenderCommonStates(
    state: PassphraseState,
    dismiss: () -> Unit,
    tooManyFailuresAction: () -> Unit,
    renderCustomStates: @Composable (state: PassphraseState) -> Unit,
) {
    when (state) {
        is PassphraseState.CoreError -> {
            RenderCoreError(close = dismiss)
        }

        PassphraseState.Success -> {
            RenderSingleMessageAndCloseButton(
                message = stringResource(id = R.string.close),
                close = dismiss
            )
        }

        PassphraseState.Loading -> {
            CenteredCircularProgressIndicatorWithText(text = stringResource(id = R.string.message_list_loading))
        }

        PassphraseState.TooManyFailedAttempts -> {
            RenderTooManyFailedAttempts(tooManyFailuresAction)
        }

        else -> renderCustomStates(state)
    }
}

@Composable
fun RenderTooManyFailedAttempts(close: () -> Unit) {
    RenderSingleMessageAndCloseButton(message = stringResource(id = R.string.passphrase_unlock_dialog_too_many_failed_attempts), close = close)
}

@Composable
fun RenderCoreError(close: () -> Unit) {
    RenderSingleMessageAndCloseButton(message = stringResource(id = R.string.error_happened_restart_app), close = close)
}

@Composable
fun RenderSingleMessageAndCloseButton(
    message: String,
    close: () -> Unit,
) {
    Text(
        text = message,
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
            onClick = close,
        )
    }
}

@Composable
fun ShowErrorFeedbackIfNeeded(state: PassphraseStateWithStatus) {
    val errorType = state.status.value
    if (errorType.isError) {
        val string = when (errorType) {
            PassphraseVerificationStatus.WRONG_FORMAT -> R.string.passphrase_wrong_input_feedback
            PassphraseVerificationStatus.WRONG_PASSPHRASE -> R.string.passhphrase_body_wrong_passphrase
            PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH -> R.string.passphrase_management_dialog_passphrase_no_match
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
}
