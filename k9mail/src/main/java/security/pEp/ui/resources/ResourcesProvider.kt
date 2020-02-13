package security.pEp.ui.resources

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.AttrRes
import com.fsck.k9.K9

interface ResourcesProvider {

    fun getAttributeResource(@AttrRes resource: Int): Int
}

class PEpResourcesProvider(private val context: Context) : ResourcesProvider {

    override fun getAttributeResource(resource: Int): Int {
        val a: TypedArray = context.theme.obtainStyledAttributes(K9.getK9ThemeResourceId(), intArrayOf(resource))
        return a.getResourceId(0, 0)
    }

}


