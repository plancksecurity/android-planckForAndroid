package security.planck.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.passphrase.compose.AccountPassphraseInput
import security.planck.ui.passphrase.compose.PassphraseScreen
import security.planck.ui.passphrase.compose.PassphraseValidationRow
import security.planck.ui.passphrase.compose.RenderCommonStates
import security.planck.ui.passphrase.compose.RenderCoreError
import security.planck.ui.passphrase.compose.ShowErrorFeedbackIfNeeded
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus

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

            is PassphraseState.CreatingAccount -> {
                RenderCreatingAccountKeys(
                    state,
                    validateNewPassphrase = viewModel::updateNewPassphrase,
                    verifyNewPassphrase = viewModel::updateNewPassphraseVerification,
                    confirm = viewModel::createAccountKeys,
                    cancel = cancel,
                )
            }

            else -> Unit
        }
    }
}

@Composable
fun RenderCreatingAccountKeys(
    state: PassphraseState.CreatingAccount,
    validateNewPassphrase: (String) -> Unit,
    verifyNewPassphrase: (String) -> Unit,
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    val defaultColor = getColorFromAttr(
        colorRes = R.attr.defaultColorOnBackground
    )
    val errorColor = colorResource(id = R.color.error_text_color)
    val successColor = getColorFromAttr(
        colorRes = R.attr.colorAccent
    )
    Text(
        text = stringResource(id = R.string.create_account_keys_dialog_description),
        color = defaultColor
    )
    Spacer(modifier = Modifier.height(16.dp))
    NewAccountPassphraseAndConfirmation(
        state = state,
        defaultColor = defaultColor,
        errorColor = errorColor,
        successColor = successColor,
        validateNewPassphrase = validateNewPassphrase,
        verifyNewPassphrase = verifyNewPassphrase,
    )
    ShowErrorFeedbackIfNeeded(state)
    // buttons at the bottom
    ButtonsRow(state.status, onConfirm = confirm, cancel = cancel)
}

@Composable
fun NewAccountPassphraseAndConfirmation(
    state: PassphraseState.CreatingAccount,
    defaultColor: Color,
    errorColor: Color,
    successColor: Color,
    validateNewPassphrase: (String) -> Unit,
    verifyNewPassphrase: (String) -> Unit,
) {
    AccountPassphraseInput(
        state = state.newPasswordState,
        defaultColor = defaultColor,
        errorColor = errorColor,
        successColor = successColor,
        onTextChanged = validateNewPassphrase,
    )
    Text(
        text = stringResource(id = R.string.passphrase_management_dialog_confirm_new_passphrase),
        color = defaultColor
    )
    PassphraseValidationRow(
        passwordState = state.newPasswordVerificationState,
        errorColor = errorColor,
        defaultColor = defaultColor,
        successColor = successColor,
        onTextChanged = verifyNewPassphrase
    )
}

@Composable
private fun ButtonsRow(
    status: PassphraseVerificationStatus,
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
            enabled = status == PassphraseVerificationStatus.SUCCESS,
            onClick = onConfirm,
        )
    }
}
