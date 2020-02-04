package security.pEp.ui.toolbar

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.annotation.ColorInt
import com.fsck.k9.pEp.PePUIArtefactCache
import foundation.pEp.jniadapter.Rating
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.pEp.ui.PEpUIUtils

interface ToolBarCustomizer {

    fun setStatusBarPepColor(pEpRating: Rating?)

    fun setStatusBarPepColor(@ColorInt colorReference: Int)

    fun setToolbarColor(pEpRating: Rating?)

    fun setToolbarColor(@ColorInt colorReference: Int)

}

class PEpToolbarCustomizer(private val activity: Activity) : ToolBarCustomizer {


    override fun setStatusBarPepColor(pEpRating: Rating?) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = PEpUIUtils.getRatingColor(activity.applicationContext, pEpRating) and 0x00FFFFFF
            setColor(color)
        }

    }

    override fun setStatusBarPepColor(@ColorInt colorReference: Int) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = colorReference and 0x00FFFFFF
            setColor(color)
        }
    }

    override fun setToolbarColor(pEpRating: Rating?) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val color = PEpUIUtils.getRatingColor(activity.applicationContext, pEpRating)
            activity.toolbar?.setBackgroundColor(color)
        }

    }

    override fun setToolbarColor(@ColorInt colorReference: Int) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            activity.toolbar?.setBackgroundColor(colorReference)
        }
    }

    private fun setColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            val window = activity.window
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // finally change the color
            window.statusBarColor = getDarkerColor(color)
        }
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