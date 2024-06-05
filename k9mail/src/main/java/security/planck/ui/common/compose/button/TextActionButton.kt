package security.planck.ui.common.compose.button

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
