package security.planck.ui.toolbar

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.ColorInt
import androidx.core.view.children
import com.fsck.k9.R
import com.fsck.k9.planck.ui.tools.ThemeManager
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToolBarCustomizer(private val activity: Activity) {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    fun setDefaultStatusBarColor() {
        setStatusBarColor(ThemeManager.getColorFromAttributeResource(activity, R.attr.statusbarDefaultColor))
    }

    fun setMessageStatusBarColor() {
        setStatusBarColor(ThemeManager.getColorFromAttributeResource(activity, R.attr.messageViewStatusBarColor))
    }

    fun setDefaultToolbarColor() {
        setToolbarColor(ThemeManager.getColorFromAttributeResource(activity, R.attr.toolbarDefaultColor))
    }

    fun setMessageToolbarColor() {
        setToolbarColor(ThemeManager.getColorFromAttributeResource(activity, R.attr.messageViewToolbarColor))
    }

    private fun setToolbarColor(@ColorInt colorReference: Int) {
        uiScope.launch {
            activity.toolbar?.setBackgroundColor(colorReference)
        }
    }

    fun colorizeToolbarActionItemsAndNavButton(@ColorInt colorReference: Int) {
        uiScope.launch {

            val colorFilter = PorterDuffColorFilter(colorReference, PorterDuff.Mode.MULTIPLY)

            activity.toolbar?.children?.forEach { v ->
                if(v is ImageButton)
                    v.drawable.mutate().colorFilter = colorFilter
            }
            activity.toolbar?.overflowIcon?.colorFilter = colorFilter
        }
    }

    private fun setStatusBarColor(@ColorInt color: Int) {

        val window = activity.window
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // finally change the color
        window.statusBarColor = color
    }
}