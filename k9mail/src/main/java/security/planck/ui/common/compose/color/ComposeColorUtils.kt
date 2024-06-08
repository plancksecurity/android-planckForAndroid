package security.planck.ui.common.compose.color

import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource

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
