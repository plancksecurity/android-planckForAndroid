package security.planck.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

private const val NO_RESOURCE = 0
private const val NO_RATING = Int.MIN_VALUE

data class RatingDisplayHolder(
    private val ratingValue: Int = NO_RATING,
    @StringRes val textRes: Int = R.string.pep_rating_none,
    @ColorRes val colorRes: Int = R.color.planck_no_color,
    @DrawableRes val iconRes: Int = NO_RESOURCE,
    val visible: Boolean = false,
    val outgoing: Boolean = false,
) {
    fun matchesRating(rating: Rating?): Boolean {
        return when (rating) {
            null -> ratingValue == NO_RATING
            else -> rating.value == ratingValue
        }
    }

    fun getDrawable(context: Context): Drawable? =
        if (iconRes == NO_RESOURCE) null
        else ContextCompat.getDrawable(context, iconRes)
}