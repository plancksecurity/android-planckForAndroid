package security.planck.ui.passphrase.unlock.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.common.compose.progress.CenteredCircularProgressIndicatorWithText
import security.planck.ui.passphrase.compose.PassphraseScreen
import security.planck.ui.passphrase.compose.PassphraseValidationList
import security.planck.ui.passphrase.compose.RenderCommonStates
import security.planck.ui.passphrase.compose.RenderCoreError
import security.planck.ui.passphrase.compose.RenderTooManyFailedAttempts
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract
import security.planck.ui.passphrase.unlock.PassphraseUnlockViewModel

@Composable
fun PassphraseUnlockDialogContent(
    viewModel: PassphraseUnlockViewModel,
    dismiss: () -> Unit,
    finishApp: () -> Unit,
) {
    PassphraseScreen(
        viewModel = viewModel,
        title = stringResource(
            id = R.string.passphrase_unlock_dialog_title
        )
    ) { state ->
        RenderState(state, finishApp, viewModel, dismiss)
    }
}

@Composable
private fun RenderState(
    state: PassphraseState,
    finishApp: () -> Unit,
    viewModel: PassphraseUnlockViewModel,
    dismiss: () -> Unit
) {
    RenderCommonStates(
        state = state,
        successText = stringResource(id = R.string.passphrase_unlock_dialog_success),
        dismiss = dismiss,
    ) {
        when (state) {
            PassphraseState.TooManyFailedAttempts -> {
                RenderTooManyFailedAttempts(
                    close = finishApp
                )
            }
            is PassphraseState.CoreError -> {
                RenderCoreError(
                    message = stringResource(id = R.string.passphrase_unlock_dialog_initial_fatal_error_feedback),
                    close = finishApp
                )
            }

            is PassphraseUnlockState.UnlockingPassphrases -> {
                RenderUnlockingPassphrases(
                    state,
                    validateInput = viewModel::validateInput,
                    onConfirm = { viewModel.unlockKeysWithPassphrase(state.passwordStates.toList()) }
                )
            }

            else -> Unit
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
fun RenderLoadingScreen(state: PassphraseUnlockState.UnlockingPassphrases) {
    val string = when (val loadingState = state.loading.value!!) {
        PassphraseLoading.Processing -> stringResource(id = R.string.message_list_loading)
        is PassphraseLoading.WaitAfterFailedAttempt -> stringResource(
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
