package security.planck.ui.toolbar

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating
import security.planck.ui.PlanckUIUtils
import security.planck.ui.PlanckUIUtils.getDrawableForToolbarRating
import security.planck.ui.PlanckUIUtils.getRatingTextRes
import security.planck.ui.PlanckUIUtils.getToolbarRatingVisibility

private const val MIN_LENGTH_BEFORE_LINEBREAK = 8

class PlanckSecurityStatusLayout(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private var securityStatusIcon: AppCompatImageView? = null
    private var securityStatusText: AppCompatTextView? = null
    private var secondLineText: AppCompatTextView? = null

    public override fun onFinishInflate() {
        super.onFinishInflate()
        securityStatusIcon = findViewById(R.id.securityStatusIcon)
        securityStatusText = findViewById(R.id.securityStatusText)
        secondLineText = findViewById(R.id.securityStatusSecondLine)
    }

    fun hideRating() {
        if (visibility != GONE) {
            visibility = GONE
        }
    }

    @JvmOverloads
    fun setIncomingRating(
        rating: Rating?,
        planckInactive: Boolean = false
    ) {
        setRating(
            rating = rating,
            outgoing = false,
            forceHide = false,
            planckInactive = planckInactive
        )
    }

    @JvmOverloads
    fun setOutgoingRating(
        rating: Rating?,
        forceHide: Boolean = false,
        planckInactive: Boolean = false
    ) {
        setRating(
            rating = rating,
            outgoing = true,
            forceHide = forceHide,
            planckInactive = planckInactive
        )
    }

    private fun setRating(
        rating: Rating?,
        outgoing: Boolean,
        forceHide: Boolean = false,
        planckInactive: Boolean = false
    ) {
        visibility = getToolbarRatingVisibility(rating, outgoing, planckInactive, forceHide)

        securityStatusIcon?.setImageDrawable(getDrawableForToolbarRating(context, rating, outgoing, planckInactive))

        setSecurityStatusColors(rating)
        setSecurityStatusText(rating, outgoing, planckInactive)
    }

    private fun setSecurityStatusColors(rating: Rating?) {
        if (!BuildConfig.IS_OFFICIAL) {
            setTextColor(rating)
        }
    }

    private fun setTextColor(rating: Rating?) {
        val textColor = PlanckUIUtils.getRatingColor(
            context,
            rating,
        )
        securityStatusText?.setTextColor(textColor)
        secondLineText?.setTextColor(textColor)
    }

    private fun setSecurityStatusText(rating: Rating?, outgoing: Boolean, planckInactive: Boolean) {
        var firstLine = context.getString(getRatingTextRes(rating, outgoing, planckInactive))
        secondLineText?.let { secondTextView ->
            var secondLine = ""
            secondTextView.text = secondLine
            if (firstLine.length > MIN_LENGTH_BEFORE_LINEBREAK) {
                val beforeLimit = firstLine.substring(0, MIN_LENGTH_BEFORE_LINEBREAK)
                val afterLimit = firstLine.substringAfter(beforeLimit)
                if (afterLimit.contains(" ")) {
                    secondLine = afterLimit.substringAfter(" ")
                    firstLine = firstLine.substringBefore(secondLine)
                } else if (beforeLimit.contains(" ")) {
                    firstLine = beforeLimit.substringBeforeLast(" ")
                    secondLine = beforeLimit.substringAfterLast(" ") + afterLimit
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
        postInvalidate()
    }
}
