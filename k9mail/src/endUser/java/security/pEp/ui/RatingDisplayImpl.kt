package security.pEp.ui

import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

class RatingDisplayImpl : RatingDisplay() {
    override val ratingDisplayHolderList: List<RatingDisplayHolder> = listOf(
        RatingDisplayHolder(),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnderAttack.value,
            textRes = R.string.pep_rating_mistrusted,
            colorRes = R.color.pep_red,
            iconRes = R.drawable.pep_status_red,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingB0rken.value,
            textRes = R.string.pep_rating_mistrusted,
            colorRes = R.color.pep_red,
            iconRes = R.drawable.pep_status_red,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingMistrust.value,
            textRes = R.string.pep_rating_mistrusted,
            colorRes = R.color.pep_red,
            iconRes = R.drawable.pep_status_red,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUndefined.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.pep_no_color,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingCannotDecrypt.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.pep_no_color,
            iconRes = R.drawable.ico_cannot_decrypt,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingHaveNoKey.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.pep_no_color,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnencrypted.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.pep_no_color,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnreliable.value,
            textRes = R.string.pep_rating_secure,
            colorRes = R.color.pep_yellow,
            iconRes = R.drawable.pep_status_yellow,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingMediaKeyProtected.value,
            textRes = R.string.pep_rating_secure,
            colorRes = R.color.pep_yellow,
            iconRes = R.drawable.pep_status_yellow,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingReliable.value,
            textRes = R.string.pep_rating_secure,
            colorRes = R.color.pep_yellow,
            iconRes = R.drawable.pep_status_yellow,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingTrusted.value,
            textRes = R.string.pep_rating_secure_trusted,
            colorRes = R.color.pep_green,
            iconRes = R.drawable.pep_status_green,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingTrustedAndAnonymized.value, // FIXME: THIS RATING SHOULD NOT BE USED ACCORDING TO DESIGN
            textRes = R.string.pep_rating_secure_trusted,
            colorRes = R.color.pep_green,
            iconRes = R.drawable.pep_status_green,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingFullyAnonymous.value, // FIXME: THIS RATING SHOULD NOT BE USED ACCORDING TO DESIGN
            textRes = R.string.pep_rating_secure_trusted,
            colorRes = R.color.pep_green,
            iconRes = R.drawable.pep_status_green,
            visible = true
        ),
    )
}