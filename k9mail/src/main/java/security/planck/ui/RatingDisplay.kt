package security.planck.ui

import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

abstract class RatingDisplay {
    protected abstract val ratingDisplayHolderList: List<RatingDisplayHolder>

    fun getForRating(
        rating: Rating?,
        outgoing: Boolean = false,
        planckInactive: Boolean = false
    ): RatingDisplayHolder {
        val ratingMatchingHolders = ratingDisplayHolderList.filter {
            it.matchesRating(rating)
        }
        val originalHolder =
            ratingMatchingHolders.firstOrNull { it.outgoing == outgoing }
                ?: ratingMatchingHolders.first()

        return if (planckInactive) originalHolder.copy(
            iconRes = R.drawable.ico_inactive,
            textRes = R.string.planck_inactive,
            visible = true,
        ) else originalHolder
    }

    companion object {
        fun getInstance(): RatingDisplay = RatingDisplayImpl()
        //private const val DISABLED_TEXT_RESOURCE = R.string.pep_rating_forced_unencrypt
        //private const val DISABLED_COLOR = R.color.planck_no_color
    }
}