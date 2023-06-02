package security.planck.ui.toolbar

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.ColorInt
import androidx.core.view.children
import com.fsck.k9.planck.PlanckColorUtils
import foundation.pEp.jniadapter.Rating
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.planck.ui.PlanckUIUtils

interface ToolBarCustomizer {

    fun setStatusBarPlanckColor(planckRating: Rating?)

    fun setStatusBarPlanckColor(@ColorInt colorReference: Int)

    fun setToolbarColor(planckRating: Rating?)

    fun setToolbarColor(@ColorInt colorReference: Int)

    fun colorizeToolbarActionItemsAndNavButton(@ColorInt colorReference: Int)
}

class PlanckToolbarCustomizer(private val activity: Activity) : ToolBarCustomizer {

    override fun setStatusBarPlanckColor(planckRating: Rating?) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = PlanckColorUtils.makeColorTransparent(PlanckUIUtils.getRatingColor(activity.applicationContext, planckRating))
            setColor(color)
        }

    }

    override fun setStatusBarPlanckColor(@ColorInt colorReference: Int) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = PlanckColorUtils.makeColorTransparent(colorReference)
            setColor(color)
        }
    }

    override fun setToolbarColor(planckRating: Rating?) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = PlanckUIUtils.getRatingColor(activity.applicationContext, planckRating)
            activity.toolbar?.setBackgroundColor(color)
        }

    }

    override fun setToolbarColor(@ColorInt colorReference: Int) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            activity.toolbar?.setBackgroundColor(colorReference)
        }
    }

    override fun colorizeToolbarActionItemsAndNavButton(@ColorInt colorReference: Int) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {

            val colorFilter = PorterDuffColorFilter(colorReference, PorterDuff.Mode.MULTIPLY)

            activity.toolbar?.children?.forEach { v ->
                if(v is ImageButton)
                        v.drawable.mutate().colorFilter = colorFilter
            }
            activity.toolbar?.overflowIcon?.colorFilter = colorFilter
        }
    }

    private fun setColor(color: Int) {

        val window = activity.window
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // finally change the color
        window.statusBarColor = getDarkerColor(color)
    }

    private fun getDarkerColor(@ColorInt color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val hsv = FloatArray(3)
        Color.RGBToHSV(red, green, blue, hsv)
        hsv[2] = hsv[2] * 0.9f
        return Color.HSVToColor(hsv)
    }
}