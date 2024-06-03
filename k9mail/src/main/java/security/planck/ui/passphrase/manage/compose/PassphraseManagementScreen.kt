package security.planck.ui.passphrase.manage.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.button.TextActionButton
import security.planck.ui.common.compose.color.getColorFromAttr
import security.planck.ui.common.compose.progress.CenteredCircularProgressIndicatorWithText
import security.planck.ui.common.compose.toolbar.WizardToolbar
import security.planck.ui.passphrase.manage.PassphraseManagementViewModel
import security.planck.ui.passphrase.models.PassphraseLoading
import security.planck.ui.passphrase.models.PassphraseMgmtState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.SelectableItem
import security.planck.ui.passphrase.models.TextFieldStateContract
import security.planck.ui.passphrase.unlock.compose.PassphraseValidationList
import security.planck.ui.passphrase.unlock.compose.PassphraseValidationRow
import security.planck.ui.passphrase.unlock.compose.RenderTooManyFailedAttempts


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
                id = R.string.passphrase_management_dialog_title
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        viewModelState.value?.let { state ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                RenderState(state, viewModel, dismiss, finishApp)
            }
        }
    }
}

@Composable
fun RenderState(
    state: PassphraseState,
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
    finishApp: () -> Unit,
) {
    when (state) {
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

        is PassphraseState.CoreError -> {
            Text(
                text = stringResource(id = R.string.error_happened_restart_app),
                fontFamily = FontFamily.SansSerif,
                color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        PassphraseState.Dismiss -> {
            SideEffect {
                dismiss()
            }
        }

        PassphraseState.Loading -> {
            CenteredCircularProgressIndicatorWithText(text = stringResource(id = R.string.message_list_loading))
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
                    previous = viewModel::goBackToChoosingAccounts
                )
            } else {
                RenderLoadingScreen(state)
            }
        }

        PassphraseState.TooManyFailedAttempts -> {
            RenderTooManyFailedAttempts(dismiss)
        }

        else -> Unit
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
    previous: () -> Unit,
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
    Spacer(modifier = Modifier.height(16.dp))
    NewPassphraseAndConfirmation(
        state = state,
        defaultColor = defaultColor,
        errorColor = errorColor,
        successColor = successColor,
        validateInput = validateNewPassphrase,
        verifyNewPassphrase = verifyNewPassphrase,
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
    ManageScreenButtonsRow(
        state = state,
        confirm = confirm,
        cancel = cancel,
        previous = previous
    )
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
        color = defaultColor
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
    previous: () -> Unit,
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
            text = stringResource(id = R.string.previous_action),
            textColor = colorResource(
                id = R.color.colorAccent
            ),
            onClick = previous,
        )

        TextActionButton(
            text = stringResource(id = R.string.pep_confirm_trustwords),
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
                    modifier = modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun <Item> RenderSelectableItem(
    item: SelectableItem<Item>,
    normalColor: Color,
    selectedColor: Color,
    onItemClicked: (SelectableItem<Item>) -> Unit,
    onItemLongClicked: (SelectableItem<Item>) -> Unit,
    modifier: Modifier = Modifier,
    renderItem: @Composable (item: SelectableItem<Item>, modifier: Modifier) -> Unit,
) {
    val backgroundColor = if (item.selected) selectedColor else normalColor
    renderItem(
        item,
        modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onItemClicked(item)
                    },
                    onLongPress = {
                        onItemLongClicked(item)
                    }
                )
            }
    )
}
