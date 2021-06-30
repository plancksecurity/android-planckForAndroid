package security.pEp.ui.toolbar

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.max

class MeasuredWidthTextView: AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) :
            super(context, attrs, attributeSetId)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val textWidth = getMaxLineWidth(layout).toInt() + paddingLeft + paddingRight
        (parent as OnMeasuredWidthTextViewDrawnListener).onMeasuredWdithTextViewDrawn(textWidth)
    }

    private fun getMaxLineWidth(layout: Layout): Float {
        var maximumWidth = 0.0f
        val lines = layout.lineCount
        for (i in 0 until lines) {
            maximumWidth = max(layout.getLineWidth(i), maximumWidth)
        }
        return maximumWidth
    }

    interface OnMeasuredWidthTextViewDrawnListener {
        fun onMeasuredWdithTextViewDrawn(textWidth: Int)
    }
}