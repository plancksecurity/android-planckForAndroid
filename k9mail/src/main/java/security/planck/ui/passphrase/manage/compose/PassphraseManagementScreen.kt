package security.planck.ui.passphrase.manage.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import security.planck.ui.passphrase.manage.AccountUsesPassphrase

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