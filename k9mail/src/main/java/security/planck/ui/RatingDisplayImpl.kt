package security.planck.ui

import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

class RatingDisplayImpl : RatingDisplay() {
    override val ratingDisplayHolderList: List<RatingDisplayHolder> = listOf(
        RatingDisplayHolder(),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnderAttack.value,
            textRes = R.string.pep_rating_dangerous,
            colorRes = R.color.planck_red,
            iconRes = R.drawable.ico_dangerous_under_attack,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingB0rken.value,
            textRes = R.string.pep_rating_dangerous,
            colorRes = R.color.planck_red,
            iconRes = R.drawable.ico_dangerous_under_attack,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingMistrust.value,
            textRes = R.string.pep_rating_dangerous,
            colorRes = R.color.planck_red,
            iconRes = R.drawable.ico_dangerous_mistrusted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUndefined.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.planck_no_color,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingCannotDecrypt.value,
            textRes = R.string.pep_rating_cannot_decrypt,
            colorRes = R.color.planck_yellow,
            iconRes = R.drawable.ico_cannot_decrypt,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingHaveNoKey.value,
            textRes = R.string.pep_rating_none,
            colorRes = R.color.planck_no_color,
            visible = false
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnencrypted.value,
            textRes = R.string.pep_rating_not_encrypted,
            colorRes = R.color.planck_yellow,
            iconRes = R.drawable.ico_not_encrypted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingUnreliable.value,
            textRes = R.string.pep_rating_weakly_encrypted,
            colorRes = R.color.planck_yellow,
            iconRes = R.drawable.ico_not_encrypted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingMediaKeyProtected.value,
            textRes = R.string.pep_rating_encrypted,
            colorRes = R.color.planck_yellow,
            iconRes = R.drawable.ico_not_encrypted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingReliable.value,
            textRes = R.string.pep_rating_encrypted,
            colorRes = R.color.planck_green,
            iconRes = R.drawable.ico_encrypted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingTrusted.value,
            textRes = R.string.pep_rating_trusted,
            colorRes = R.color.planck_green,
            iconRes = R.drawable.ico_trusted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingTrustedAndAnonymized.value, // FIXME: THIS RATING SHOULD NOT BE USED ACCORDING TO DESIGN
            textRes = R.string.pep_rating_trusted,
            colorRes = R.color.planck_green,
            iconRes = R.drawable.ico_trusted,
            visible = true
        ),
        RatingDisplayHolder(
            ratingValue = Rating.pEpRatingFullyAnonymous.value, // FIXME: THIS RATING SHOULD NOT BE USED ACCORDING TO DESIGN
            textRes = R.string.pep_rating_trusted,
            colorRes = R.color.planck_green,
            iconRes = R.drawable.ico_trusted,
            visible = true
        ),
    )
}