package security.planck.ui.passphrase.manage.compose

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.common.compose.list.RenderSelectableItem
import security.planck.ui.passphrase.compose.PassphraseScreen
import security.planck.ui.passphrase.compose.PassphraseValidationList
import security.planck.ui.passphrase.compose.PassphraseValidationRow
import security.planck.ui.passphrase.compose.RenderCommonStates
import security.planck.ui.passphrase.compose.RenderTooManyFailedAttempts
import security.planck.ui.passphrase.compose.ShowErrorFeedbackIfNeeded
import security.planck.ui.passphrase.manage.PassphraseManagementViewModel
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus


@Composable
fun PassphraseManagementDialogContent(
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
) {
    PassphraseScreen(
        viewModel = viewModel,
        title = stringResource(id = R.string.passphrase_management_dialog_title)
    ) { state ->
        Log.e("EFA-602", "MyScreen recomposed with text: ${state}")
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            RenderState(state, viewModel, dismiss)
        }
    }
}

@Composable
fun RenderState(
    state: PassphraseState,
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
) {
    RenderCommonStates(
        state = state,
        successText = stringResource(id = R.string.passphrase_management_dialog_success),
        dismiss = dismiss,
    ) {
        when (state) {
            PassphraseState.TooManyFailedAttempts -> {
                RenderTooManyFailedAttempts(
                    message = stringResource(id = R.string.passphrase_management_dialog_too_many_failed_attempts),
                    close = dismiss
                )
            }

            is PassphraseMgmtState.ChoosingAccountsToManage -> {
                Text(
                    text = stringResource(id = R.string.passphrase_management_dialog_pick_accounts),
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
                )
                Spacer(modifier = Modifier.height(16.dp))
                RenderChoosingAccountsToManage(state, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                ChooseScreenButtonsRow(
                    actionMode = state.actionMode,
                    manage = { viewModel.goToManagePassphrase() },
                    cancel = dismiss,
                )
            }

            is PassphraseMgmtState.ManagingAccounts -> {
                Log.e("EFA-602", "MyScreen recomposed with state: ${state.newPasswordState.text}")
                RenderManagingAccounts(
                    state,
                    validateInput = viewModel::updateAndValidateText,
                    confirm = { viewModel.setNewPassphrase() },
                    cancel = dismiss,
                )
            }

            else -> Unit
        }
    }
}

@Composable
fun RenderManagingAccounts(
    state: PassphraseMgmtState.ManagingAccounts,
    validateInput: (Int, String) -> Unit,
    //validateNewPassphrase: (String) -> Unit,
    //verifyNewPassphrase: (String) -> Unit,
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
        text = stringResource(id = R.string.passphrase_management_dialog_enter_old_passphrases),
        color = defaultColor
    )
    Spacer(modifier = Modifier.height(16.dp))
    OldPassphrasesVerification(
        state = state,
        defaultColor = defaultColor,
        errorColor = errorColor,
        successColor = successColor,
        validateInput = validateInput,
    )
    AccountsWithNoPassphrase(state, defaultColor)
    Spacer(modifier = Modifier.height(16.dp))
    NewPassphraseAndConfirmation(
        state = state,
        defaultColor = defaultColor,
        errorColor = errorColor,
        successColor = successColor,
        validateInput = { validateInput(state.oldPasswordStates.size, it) },
        verifyNewPassphrase = { validateInput(state.oldPasswordStates.size + 1, it) },
    )
    ShowErrorFeedbackIfNeeded(state)
    ManageScreenButtonsRow(
        state = state,
        confirm = confirm,
        cancel = cancel,
    )
}

@Composable
fun AccountsWithNoPassphrase(state: PassphraseMgmtState.ManagingAccounts, color: Color) {
    Column {
        state.accountsWithNoPassphrase.forEach { email ->
            Text(text = email, color = color, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(id = R.string.passphrase_management_dialog_passphrase_no_passphrase),
                color = color
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NewPassphraseAndConfirmation(
    state: PassphraseMgmtState.ManagingAccounts,
    defaultColor: Color,
    errorColor: Color,
    successColor: Color,
    validateInput: (String) -> Unit,
    verifyNewPassphrase: (String) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.passphrase_management_dialog_enter_new_passphrase),
        color = defaultColor,
    )
    Text(
        text = stringResource(id = R.string.passphrase_management_dialog_enter_new_passphrase_empty),
        color = defaultColor,
        fontWeight = FontWeight.Bold,
    )
    PassphraseValidationRow(
        passwordState = state.newPasswordState,
        errorColor = errorColor,
        defaultColor = defaultColor,
        successColor = successColor,
        onTextChanged = validateInput
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
fun OldPassphrasesVerification(
    state: PassphraseMgmtState.ManagingAccounts,
    defaultColor: Color,
    errorColor: Color,
    successColor: Color,
    validateInput: (Int, String) -> Unit,
) {
    PassphraseValidationList(
        passwordStates = state.oldPasswordStates,
        defaultColor = defaultColor,
        successColor = successColor,
        errorColor = errorColor,
        onTextChanged = validateInput,
    )
}

@Composable
private fun ChooseScreenButtonsRow(
    actionMode: Boolean,
    manage: () -> Unit,
    cancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,

        ) {
        if (actionMode) {
            TextActionButton(
                text = stringResource(id = R.string.cancel_action),
                textColor = colorResource(
                    id = R.color.colorAccent
                ),
                onClick = cancel,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextActionButton(
            text = stringResource(id = if (actionMode) R.string.passphrase_management_dialog_manage_button else R.string.close),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            onClick = if (actionMode) manage else cancel,
        )
    }
}

@Composable
private fun ManageScreenButtonsRow(
    state: PassphraseMgmtState.ManagingAccounts,
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextActionButton(
            text = stringResource(id = R.string.cancel_action),
            textColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
            onClick = cancel,
        )

        TextActionButton(
            text = stringResource(id = R.string.action_remove),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            enabled = state.status == PassphraseVerificationStatus.SUCCESS_EMPTY,
            onClick = confirm,
        )

        TextActionButton(
            text = stringResource(id = R.string.action_change),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            enabled = state.status == PassphraseVerificationStatus.SUCCESS,
            onClick = confirm,
        )
    }
}

@Composable
fun RenderChoosingAccountsToManage(
    state: PassphraseMgmtState.ChoosingAccountsToManage,
    viewModel: PassphraseManagementViewModel
) {
    Column {
        state.accounts.forEachIndexed { index, account ->
            RenderSelectableItem(
                item = account,
                onItemClicked = {
                    viewModel.accountClicked(index)
                },
                onItemLongClicked = {
                    viewModel.accountLongClicked(index)
                },
                normalColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
                selectedColor = getColorFromAttr(colorRes = R.attr.messageListSelectedBackgroundColor),
            ) { acc, modifier ->
                Text(
                    text = acc.data,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                    modifier = modifier.padding(8.dp)
                )
            }
        }
    }
}
