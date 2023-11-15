package security.planck.ui

import com.fsck.k9.R
import com.fsck.k9.planck.PlanckUtils
import foundation.pEp.jniadapter.Rating

abstract class RatingDisplay {
    protected abstract val ratingDisplayHolderList: List<RatingDisplayHolder>

    fun getForRating(
        rating: Rating?,
        pEpEnabled: Boolean = true,
        planckInactive: Boolean = false
    ): RatingDisplayHolder {
        val originalHolder = ratingDisplayHolderList.first { it.matchesRating(rating) }
        return when {
            rating == null
                    || pEpEnabled
                    || PlanckUtils.isRatingUnsecure(rating) ->
                if (planckInactive) originalHolder.copy(
                    iconRes = R.drawable.ico_inactive,
                    textRes = R.string.planck_inactive
                ) else originalHolder

            else -> {
                originalHolder.copy(
                    textRes = DISABLED_TEXT_RESOURCE,
                    colorRes = DISABLED_COLOR
                )
            }
        }
    }

    companion object {
        fun getInstance(): RatingDisplay = RatingDisplayImpl()
        private const val DISABLED_TEXT_RESOURCE = R.string.pep_rating_forced_unencrypt
        private const val DISABLED_COLOR = R.color.planck_no_color
    }
}