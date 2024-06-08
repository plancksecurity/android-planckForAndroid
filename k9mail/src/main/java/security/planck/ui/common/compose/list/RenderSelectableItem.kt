package security.planck.ui.common.compose.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import security.planck.ui.passphrase.models.SelectableItem

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
