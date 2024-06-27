package security.planck.ui.passphrase.newaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import com.fsck.k9.activity.setup.CreateAccountKeysViewModel
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.passphrase.compose.PassphraseScreen
import security.planck.ui.passphrase.compose.PassphraseValidationList
import security.planck.ui.passphrase.compose.RenderCommonStates
import security.planck.ui.passphrase.compose.RenderCoreError
import security.planck.ui.passphrase.compose.RenderTooManyFailedAttempts
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.unlock.PassphraseUnlockViewModel

@Composable
fun CreateAccountKeysDialogContent(
    viewModel: CreateAccountKeysViewModel,
    successDismiss: () -> Unit,
    errorDismiss: () -> Unit,
    cancel: () -> Unit,
) {
    PassphraseScreen(
        viewModel = viewModel,
        title = stringResource(
            id = R.string.create_account_keys_dialog_title
        )
    ) { state ->
        RenderState(state, viewModel, successDismiss, errorDismiss, cancel)
    }
}

@Composable
private fun RenderState(
    state: PassphraseState,
    viewModel: CreateAccountKeysViewModel,
    successDismiss: () -> Unit,
    errorDismiss: () -> Unit,
    cancel: () -> Unit,
) {
    RenderCommonStates(
        state = state,
        successText = stringResource(id = R.string.create_account_keys_dialog_success_message),
        dismiss = successDismiss,
    ) {
        when (state) {
            is PassphraseState.CoreError -> {
                RenderCoreError(
                    close = errorDismiss
                )
            }

            is PassphraseUnlockState.UnlockingPassphrases -> {
                RenderUnlockingPassphrases(
                    state,
                    validateInput = viewModel::updateAndValidateText,
                    onConfirm = { viewModel.createAccountKeys() },
                    onCancel = cancel,
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun RenderUnlockingPassphrases(
    state: PassphraseUnlockState.UnlockingPassphrases,
    validateInput: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    RenderInputScreen(
        state,
        validateInput = validateInput,
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}

@Composable
private fun RenderInputScreen(
    state: PassphraseUnlockState.UnlockingPassphrases,
    validateInput: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val defaultColor = getColorFromAttr(
        colorRes = R.attr.defaultColorOnBackground
    )
    val errorColor = colorResource(id = R.color.error_text_color)
    val successColor = getColorFromAttr(
        colorRes = R.attr.colorAccent
    )
    Text(text = stringResource(id = R.string.create_account_keys_dialog_description), color = defaultColor)
    Spacer(modifier = Modifier.height(16.dp))
    PassphraseValidationList(
        passwordStates = state.passwordStates,
        defaultColor, successColor, errorColor,
        onTextChanged = validateInput,
    )
    val errorType = state.status
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

    // buttons at the bottom
    ButtonsRow(state, onConfirm = onConfirm, cancel = onCancel)
}

@Composable
private fun ButtonsRow(
    state: PassphraseUnlockState.UnlockingPassphrases,
    onConfirm: () -> Unit,
    cancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextActionButton(
            text = stringResource(id = R.string.cancel_action),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            onClick = cancel,
        )
        Spacer(modifier = Modifier.weight(1f))
        TextActionButton(
            text = stringResource(id = R.string.pep_confirm_trustwords),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            enabled = state.status == PassphraseVerificationStatus.SUCCESS,
            onClick = onConfirm,
        )
    }
}
