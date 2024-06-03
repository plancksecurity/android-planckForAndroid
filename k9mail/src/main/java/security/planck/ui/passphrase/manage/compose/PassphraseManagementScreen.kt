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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import security.planck.ui.passphrase.manage.PassphraseManagementViewModel
import security.planck.ui.passphrase.manage.PassphraseMgmtState
import security.planck.ui.passphrase.manage.SelectableItem
import security.planck.ui.passphrase.models.TextFieldStateContract
import security.planck.ui.passphrase.unlock.compose.PassphraseValidationList
import security.planck.ui.passphrase.unlock.compose.PassphraseValidationRow


@Composable
fun PassphraseManagementDialogContent(
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
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
                RenderState(state, viewModel, dismiss)
            }
        }
    }
}

@Composable
fun RenderState(
    state: PassphraseMgmtState,
    viewModel: PassphraseManagementViewModel,
    dismiss: () -> Unit,
) {
    when (state) {
        is PassphraseMgmtState.ChoosingAccountsToManage -> {
            RenderChoosingAccountsToManage(state, viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            ChooseScreenButtonsRow(
                actionMode = state.actionMode,
                manage = { viewModel.selectAccountsToManagePassphrase(state.selectedAccounts) },
                cancel = dismiss,
            )
        }

        is PassphraseMgmtState.CoreError -> {
            Text(
                text = stringResource(id = R.string.error_happened_restart_app),
                fontFamily = FontFamily.SansSerif,
                color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        PassphraseMgmtState.Dismiss -> {
            SideEffect {
                dismiss()
            }
        }
        PassphraseMgmtState.Loading -> {
            CenteredCircularProgressIndicatorWithText(text = stringResource(id = R.string.message_list_loading))
        }

        is PassphraseMgmtState.ManagingAccounts -> {
            RenderManagingAccounts(
                state,
                validateInput = viewModel::validateInput,
                validateNewPassphrase = { viewModel.validateNewPassphrase(state) },
                verifyNewPassphrase = { viewModel.verifyNewPassphrase(state) },
                confirm = { viewModel.setNewPassphrase(state) },
                cancel = dismiss,
                previous = viewModel::goBackToChoosingAccounts
            )
        }
    }
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
    Text(text = stringResource(id = R.string.passphrase_management_dialog_enter_old_passphrases), color = defaultColor)
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
    ManageScreenButtonsRow(
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
    Text(text = stringResource(id = R.string.passphrase_management_dialog_enter_new_passphrase), color = defaultColor)
    PassphraseValidationRow(
        passwordState = state.newPasswordState,
        errorColor = errorColor,
        defaultColor = defaultColor,
        successColor = successColor,
        validateInput = validateInput
    )
    Text(text = stringResource(id = R.string.passphrase_management_dialog_confirm_new_passphrase), color = defaultColor)
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


//    LazyColumn {
//        itemsIndexed(state.oldPasswordStates) { index, account ->
//            RenderSelectableItem(
//                item = account,
//                onItemClicked = { clickedItem ->
//                    viewModel.accountClicked(clickedItem)
//                },
//                onItemLongClicked = {
//                    viewModel.accountLongClicked(it)
//                },
//                normalColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
//                selectedColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
//            ) { acc, modifier ->
//                Text(text = acc.data.account, color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground), modifier = modifier.padding(vertical = 8.dp))
//            }
//        }
//    }
}

@Composable
fun NewPassphraseVerificationRow(
    passwordState: TextFieldStateContract,
    textColor: Color,
    errorColor: Color,
    defaultColor: Color,
    validateInput: (TextFieldStateContract) -> Unit,
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
                selectedColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
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
