package security.pEp.ui.resources

import android.app.Activity
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.fsck.k9.K9
import com.fsck.k9.pEp.ui.tools.ThemeManager

interface ResourcesProvider {

    fun getAttributeResource(@AttrRes resource: Int): Int

    fun getColorFromAttributeResource(@AttrRes resource: Int): Int
}

//TODO: Check context scope needed
class PEpResourcesProvider(private val context: Activity) : ResourcesProvider {

    override fun getAttributeResource(@AttrRes resource: Int): Int {
        val a: TypedArray = context.theme.obtainStyledAttributes(ThemeManager.appThemeResourceId, intArrayOf(resource))
        return a.getResourceId(0, 0)
    }

    @ColorInt
    override fun getColorFromAttributeResource(@AttrRes resource: Int): Int {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(resource, typedValue, true)
        return typedValue.data
    }

}


