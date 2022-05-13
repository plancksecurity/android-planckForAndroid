package security.pEp.ui.toolbar

import android.content.Context
import android.graphics.PorterDuff
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

private const val MIN_LENGTH_BEFORE_LINEBREAK = 8

class PEpSecurityStatusLayout(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private var securityStatusIcon: AppCompatImageView? = null
    private var securityStatusText: AppCompatTextView? = null
    private var secondLineText: AppCompatTextView? = null
    var encrypt = true

    public override fun onFinishInflate() {
        super.onFinishInflate()
        securityStatusIcon = findViewById(R.id.securityStatusIcon)
        securityStatusText = findViewById(R.id.securityStatusText)
        secondLineText = findViewById(R.id.securityStatusSecondLine)
    }

    fun setRating(rating: Rating?) {
        visibility = getToolbarRatingVisibility(rating, encrypt)

        securityStatusIcon?.setImageDrawable(getDrawableForToolbarRating(context, rating))

        setSecurityStatusColors(rating)
        setSecurityStatusText(rating)
    }

    private fun setSecurityStatusColors(rating: Rating?) {
        if (!encrypt)
            securityStatusIcon?.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.pep_no_color
                ),
                PorterDuff.Mode.SRC_IN
            )
        else
            securityStatusIcon?.clearColorFilter()

        securityStatusText?.setTextColor(
            ContextCompat.getColor(
                context,
                getRatingColorRes(rating, encrypt)
            )
        )
        secondLineText?.setTextColor(
            ContextCompat.getColor(
                context,
                getRatingColorRes(rating, encrypt)
            )
        )
    }

    private fun setSecurityStatusText(rating: Rating?) {
        var firstLine = context.getString(getRatingTextRes(rating, encrypt))
        secondLineText?.let { secondTextView ->
            var secondLine = ""
            if (firstLine.length > MIN_LENGTH_BEFORE_LINEBREAK) {
                val afterLimit = firstLine.substring(MIN_LENGTH_BEFORE_LINEBREAK)
                if (afterLimit.contains(" ")) {
                    secondLine = afterLimit.substringAfter(" ")
                    firstLine = firstLine.substringBefore(secondLine)
                }
            }
            if (secondLine.isNotBlank()) {
                secondTextView.visibility = VISIBLE
                secondTextView.text = secondLine
            } else {
                secondTextView.visibility = GONE
            }
        }
        securityStatusText?.text = firstLine
    }
}
