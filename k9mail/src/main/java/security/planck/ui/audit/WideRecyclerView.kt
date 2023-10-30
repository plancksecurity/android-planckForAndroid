package security.planck.ui.audit

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView


class WideRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    var maxWidth = 0
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        setMeasuredDimension(if (maxWidth > 0) maxWidth else measuredWidth, measuredHeight)
    }
}