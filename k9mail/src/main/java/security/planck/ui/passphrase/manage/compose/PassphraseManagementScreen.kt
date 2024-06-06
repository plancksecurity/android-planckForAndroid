package security.planck.ui.passphrase.manage.compose

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
import security.planck.ui.common.compose.progress.CenteredCircularProgressIndicatorWithText
import security.planck.ui.passphrase.compose.PassphraseScreen
import security.planck.ui.passphrase.compose.PassphraseValidationList
import security.planck.ui.passphrase.compose.PassphraseValidationRow
import security.planck.ui.passphrase.compose.RenderCommonStates
import security.planck.ui.passphrase.compose.RenderTooManyFailedAttempts
import security.planck.ui.passphrase.compose.ShowErrorFeedbackIfNeeded
import security.planck.ui.passphrase.manage.PassphraseManagementViewModel
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldStateContract


@Composable
fun PassphraseManagementDialogContent(
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
) {
    PassphraseScreen(
        viewModel = viewModel,
        title = stringResource(id = R.string.passphrase_management_dialog_title)
    ) { state ->
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
                    manage = { viewModel.selectAccountsToManagePassphrase(state.selectedAccounts) },
                    cancel = dismiss,
                )
            }

            is PassphraseMgmtState.ManagingAccounts -> {
                if (state.loading.value == null) {
                    RenderManagingAccounts(
                        state,
                        validateInput = viewModel::validateInput,
                        validateNewPassphrase = { viewModel.validateNewPassphrase(state) },
                        verifyNewPassphrase = { viewModel.verifyNewPassphrase(state) },
                        confirm = { viewModel.setNewPassphrase(state) },
                        cancel = dismiss,
                    )
                } else {
                    RenderLoadingScreen(state)
                }
            }

            else -> Unit
        }
    }
}

@Composable
fun RenderLoadingScreen(state: PassphraseMgmtState.ManagingAccounts) {
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
fun RenderManagingAccounts(
    state: PassphraseMgmtState.ManagingAccounts,
    validateInput: (TextFieldStateContract) -> Unit,
    validateNewPassphrase: (TextFieldStateContract) -> Unit,
    verifyNewPassphrase: (TextFieldStateContract) -> Unit,
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
        validateInput = validateNewPassphrase,
        verifyNewPassphrase = verifyNewPassphrase,
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
    validateInput: (TextFieldStateContract) -> Unit,
    verifyNewPassphrase: (TextFieldStateContract) -> Unit,
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
        validateInput = validateInput
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
        validateInput = verifyNewPassphrase
    )

}

@Composable
fun OldPassphrasesVerification(
    state: PassphraseMgmtState.ManagingAccounts,
    defaultColor: Color,
    errorColor: Color,
    successColor: Color,
    validateInput: (TextFieldStateContract) -> Unit,
) {
    PassphraseValidationList(
        passwordStates = state.oldPasswordStates,
        defaultColor = defaultColor,
        successColor = successColor,
        errorColor = errorColor,
        validateInput = validateInput,
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
            enabled = state.status.value == PassphraseVerificationStatus.SUCCESS_EMPTY,
            onClick = confirm,
        )

        TextActionButton(
            text = stringResource(id = R.string.action_change),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            enabled = state.status.value == PassphraseVerificationStatus.SUCCESS,
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
        state.accountsUsingPassphrase.forEachIndexed { index, account ->
            RenderSelectableItem(
                item = account,
                onItemClicked = { clickedItem ->
                    viewModel.accountClicked(clickedItem)
                },
                onItemLongClicked = {
                    viewModel.accountLongClicked(it)
                },
                normalColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
                selectedColor = getColorFromAttr(colorRes = R.attr.messageListSelectedBackgroundColor),
            ) { acc, modifier ->
                Text(
                    text = acc.data.account,
                    color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                    modifier = modifier.padding(8.dp)
                )
            }
        }
    }
}
