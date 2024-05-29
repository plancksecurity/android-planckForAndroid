package security.planck.passphrase

import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsck.k9.R

@Composable
fun PassphraseManagementDialogContent(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    viewModel: PassphraseManagementViewModel,
) {
    val minWidth = dimensionResource(id = R.dimen.key_import_floating_width)
    val paddingHorizontal = 16.dp
    val paddingTop = 16.dp
    val paddingBottom = 8.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .widthIn(min = minWidth)
            .padding(horizontal = paddingHorizontal, vertical = 0.dp)
            .padding(top = paddingTop, bottom = paddingBottom)
    ) {
        WizardToolbar(title = stringResource(id = R.string.passphrase_management_dialog_title))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextActionButton(
                text = stringResource(id = R.string.cancel_action),
                textColor = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
                onCancel
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
fun TextActionButton(
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(top = 8.dp)
    ) {
        Text(
            text = text, color = textColor, fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp
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
