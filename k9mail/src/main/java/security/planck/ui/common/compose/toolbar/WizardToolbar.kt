package security.planck.ui.common.compose.toolbar

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
                    .padding(start = 0.dp)
            )
        },
        backgroundColor = getColorFromAttr(colorRes = R.attr.defaultDialogBackground),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // ?attr/actionBarSize default height
    )
}