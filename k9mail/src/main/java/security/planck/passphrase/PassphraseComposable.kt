package security.planck.passphrase

import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsck.k9.Account
import com.fsck.k9.R

@Composable
fun PassphraseManagementDialogContent(
    viewModel: PassphraseManagementViewModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    dismiss: () -> Unit,
) {
    val minWidth = dimensionResource(id = R.dimen.key_import_floating_width)
    val paddingHorizontal = 16.dp
    val paddingTop = 16.dp
    val paddingBottom = 8.dp
    val viewModelState = viewModel.state.observeAsState()

    Column(
        //horizontalAlignment = Alignment.CenterHorizontally,
        //verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .widthIn(min = minWidth)
            .padding(horizontal = paddingHorizontal, vertical = 0.dp)
            .padding(top = paddingTop, bottom = paddingBottom)
    ) {
        WizardToolbar(title = stringResource(id = R.string.passphrase_management_dialog_title))

        when (val state = viewModelState.value) {
            PassphraseMgmtState.Idle -> {}
            is PassphraseMgmtState.CoreError -> {}
            PassphraseMgmtState.Loading -> {
                CenteredCircularProgressIndicator()
            }

            is PassphraseMgmtState.ManagingAccounts -> {
                PassphraseManagementList(
                    accountUsesPassphraseList = state.accountsUsingPassphrase
                )

                // buttons at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TextActionButton(
                        text = stringResource(id = R.string.cancel_action),
                        textColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                        onClick = onCancel
                    )
                    TextActionButton(
                        text = stringResource(id = R.string.pep_confirm_trustwords),
                        textColor = colorResource(
                            id = R.color.colorAccent
                        ),
                    ) {
                        onConfirm()
                    }
                }
            }

            is PassphraseMgmtState.UnlockingPassphrases -> {
                //val passwordStates = remember { mutableStateListOf(*Array(state.accountsUsingPassphrase.size) { TextFieldState() }) }
                val passwordStates = remember {
                    state.accountsUsingPassphrase.map { TextFieldState() }
                }

                PassphraseUnlockingList(
                    accountsUsingPassphrase = state.accountsUsingPassphrase,
                    passwordStates = passwordStates,
                    accountsWithErrors = state.accountUnlockErrors,
                )

                // buttons at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TextActionButton(
                        text = stringResource(id = R.string.cancel_action),
                        textColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                        onClick = onCancel
                    )
                    TextActionButton(
                        text = stringResource(id = R.string.pep_confirm_trustwords),
                        textColor = colorResource(
                            id = R.color.colorAccent
                        ),
                        enabled = passwordStates.none { it.errorState }
                    ) {
                        viewModel.unlockKeysWithPassphrase(state.accountsUsingPassphrase.map { it.email }, passwordStates.map { it.textState })
                        onConfirm()
                    }
                }
            }

            PassphraseMgmtState.Finish -> {
                dismiss()
            }

            null -> {

            }
        }
    }
}

@Composable
fun WizardToolbar(title: String) {
    val textSize = 24.sp
    val fontWeight = FontWeight.Bold

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = textSize,
                color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                fontWeight = fontWeight,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
            )
        },
        backgroundColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // ?attr/actionBarSize default height
    )
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
    accountsUsingPassphrase: List<Account>,
    passwordStates: List<TextFieldState>,
    accountsWithErrors: List<String>,
) {

    LazyColumn {
        itemsIndexed(accountsUsingPassphrase) { index, account ->
            Column {
                Text(text = account.email, color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground))
                PasswordInputField(passwordStates[index], initialError = accountsWithErrors.contains(account.email))
            }
        }
    }
}

@Composable
fun PasswordInputField(
    passwordState: TextFieldState,
    initialError: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val color = getColorFromAttr(
        colorRes = R.attr.defaultColorOnBackground
    )

    OutlinedTextField(
        value = passwordState.textState,
        onValueChange = {
            passwordState.textState = it
            passwordState.errorState = it.length < 6
        },
        label = { Text("Password", color = color) },
        isError = passwordState.errorState,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            // Please provide localized description for accessibility services
            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, description, tint = color,)
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = color,
            unfocusedBorderColor = color,
            textColor = color,
            cursorColor = color
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

@Composable
fun TextActionButton(
    text: String,
    textColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonColors = ButtonDefaults.textButtonColors(
        contentColor = if (enabled) textColor else Color.Gray,
        disabledContentColor = Color.Gray
    )

    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(top = 8.dp)
    ) {
        Text(
            text = text, fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp
        )
    }
}

@Composable
fun CenteredCircularProgressIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = colorResource(id = R.color.colorAccent),
            strokeWidth = 4.dp,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
fun getColorFromAttr(@AttrRes colorRes: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    val theme = context.theme

    // Retrieve the text color from the XML style
    theme.resolveAttribute(colorRes, typedValue, true)
    return if (typedValue.resourceId != 0) {
        // Attribute is a reference to a color resource
        colorResource(id = typedValue.resourceId)
    } else {
        // Attribute is a direct color value
        Color(typedValue.data)
    }
}

data class TextFieldState(
    private val text: String = "",
    private val isError: Boolean = false,
) {
    var textState by mutableStateOf(text)
    var errorState by mutableStateOf(isError)
}
