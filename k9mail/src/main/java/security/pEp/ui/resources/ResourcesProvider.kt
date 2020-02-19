package security.pEp.ui.resources

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.fsck.k9.K9

interface ResourcesProvider {

    fun getAttributeResource(@AttrRes resource: Int): Int

    fun getColorFromAttributeResource(@AttrRes resource: Int): Int
}

class PEpResourcesProvider(private val context: Context) : ResourcesProvider {

    override fun getAttributeResource(@AttrRes resource: Int): Int {
        val a: TypedArray = context.theme.obtainStyledAttributes(K9.getK9ThemeResourceId(), intArrayOf(resource))
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


