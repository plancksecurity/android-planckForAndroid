package security.planck.ui.common.compose.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsck.k9.R
import security.planck.ui.common.compose.color.getColorFromAttr

@Composable
fun WizardToolbar(title: String) {
    val textSize = 24.sp
    val fontWeight = FontWeight.Bold

    Row(
        modifier = Modifier
            .background(color = getColorFromAttr(colorRes = R.attr.defaultDialogBackground))
            .fillMaxWidth()
            .height(56.dp) // ?attr/actionBarSize default height
    ) {
        Text(
            text = title,
            fontSize = textSize,
            color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground),
            fontWeight = fontWeight,
            modifier = Modifier
                .fillMaxHeight()
        )
    }
}