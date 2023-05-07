package security.planck.ui.toolbar

import android.content.Context
import android.graphics.PorterDuff
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
    var ispEpEnabled = true

    public override fun onFinishInflate() {
        super.onFinishInflate()
        securityStatusIcon = findViewById(R.id.securityStatusIcon)
        securityStatusText = findViewById(R.id.securityStatusText)
        secondLineText = findViewById(R.id.securityStatusSecondLine)
    }

    public fun hideRating() {
        if (visibility != GONE) {
            visibility = GONE
        }
    }

    @JvmOverloads
    fun setRating(rating: Rating?, forceHide: Boolean = false) {
        visibility = getToolbarRatingVisibility(rating, ispEpEnabled, forceHide)

        securityStatusIcon?.setImageDrawable(getDrawableForToolbarRating(context, rating))

        setSecurityStatusColors(rating)
        setSecurityStatusText(rating)
    }

    private fun setSecurityStatusColors(rating: Rating?) {
        if (!ispEpEnabled)
            securityStatusIcon?.setColorFilter(
                PlanckUIUtils.getRatingColor(context, rating, ispEpEnabled),
                PorterDuff.Mode.SRC_IN
            )
        else
            securityStatusIcon?.clearColorFilter()

        if (!BuildConfig.IS_ENTERPRISE) {
            setTextColor(rating)
        }
    }

    private fun setTextColor(rating: Rating?) {
        val textColor = PlanckUIUtils.getRatingColor(
            context,
            rating,
            ispEpEnabled
        )
        securityStatusText?.setTextColor(textColor)
        secondLineText?.setTextColor(textColor)
    }

    private fun setSecurityStatusText(rating: Rating?) {
        var firstLine = context.getString(getRatingTextRes(rating, ispEpEnabled))
        secondLineText?.let { secondTextView ->
            var secondLine = ""
            secondTextView.text = secondLine
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
