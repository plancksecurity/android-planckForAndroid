package security.pEp.ui.toolbar

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating
import security.pEp.ui.PEpUIUtils.getDrawableForToolbarRating
import security.pEp.ui.PEpUIUtils.getRatingColorRes
import security.pEp.ui.PEpUIUtils.getRatingTextRes
import security.pEp.ui.PEpUIUtils.getToolbarRatingVisibility

class PEpSecurityStatusLayout(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var privacyStatusIcon: AppCompatImageView? = null
    private var privacyStatusText: AppCompatTextView? = null
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
}