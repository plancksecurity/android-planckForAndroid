package security.planck.ui.common.compose.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fsck.k9.R
import security.planck.ui.common.compose.color.getColorFromAttr

@Composable
fun CenteredCircularProgressIndicatorWithText(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = colorResource(id = R.color.colorAccent),
            strokeWidth = 4.dp,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = text,
            fontFamily = FontFamily.SansSerif,
            color = getColorFromAttr(colorRes = R.attr.defaultColorOnBackground)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}