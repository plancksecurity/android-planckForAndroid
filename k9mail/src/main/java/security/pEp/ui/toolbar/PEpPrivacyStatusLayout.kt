package security.pEp.ui.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating
import security.pEp.ui.PEpUIUtils.getDrawableForToolbarRating
import security.pEp.ui.PEpUIUtils.getRatingColorRes
import security.pEp.ui.PEpUIUtils.getRatingTextRes
import security.pEp.ui.PEpUIUtils.getToolbarRatingVisibility

class PEpPrivacyStatusLayout(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs),
    MeasuredWidthTextView.OnMeasuredWidthTextViewDrawnListener {

    private var privacyStatusIcon: AppCompatImageView? = null
    private var privacyStatusText: MeasuredWidthTextView? = null
    var encrypt = true

    public override fun onFinishInflate() {
        super.onFinishInflate()
        privacyStatusIcon = findViewById(R.id.privacyStatusIcon)
        privacyStatusText = findViewById(R.id.privacyStatusText)
    }

    fun setRating(rating: Rating?) {
        visibility = getToolbarRatingVisibility(rating, encrypt)

        privacyStatusIcon?.setImageDrawable(getDrawableForToolbarRating(context, rating))

        if (!encrypt)
            privacyStatusIcon?.setColorFilter(ContextCompat.getColor(context, R.color.pep_no_color), android.graphics.PorterDuff.Mode.SRC_IN)
        else
            privacyStatusIcon?.clearColorFilter()

        privacyStatusText?.setText(getRatingTextRes(rating, encrypt))
        privacyStatusText?.setTextColor(ContextCompat.getColor(context, getRatingColorRes(rating, encrypt)))

    }

    override fun onMeasuredWdithTextViewDrawn(textWidth: Int) {
        val previousVisibility = visibility
        visibility = View.INVISIBLE
        setLeftMargin(0)
        val totalSpaceNeeded = privacyStatusIcon!!.width + textWidth + privacyStatusText!!.marginLeft
        val leftMargin = (width - totalSpaceNeeded)/2
        setLeftMargin(leftMargin)
        visibility = previousVisibility
    }

    private fun setLeftMargin(px: Int) {
        val params = privacyStatusIcon!!.layoutParams as LayoutParams
        params.setMargins(px, params.topMargin, params.rightMargin, params.bottomMargin)
        privacyStatusIcon!!.layoutParams = params
    }
}