package security.pEp.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

object PEpUIUtils {

    @JvmStatic
    fun accountNameSummary(word: String): String {
        return word.take(0).ifBlank { "?" }
    }

    @JvmStatic
    fun getDrawableForRating(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value == Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            else ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
        }
    }

    @JvmStatic
    fun getDrawableForRatingRecipient(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_white)
            rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_white)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red_white)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green_dark)
            rating.value == Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_white)
            else ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_white)
        }
    }

    @JvmStatic
    fun getRatingColor(context: Context, rating: Rating?): Int {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;
        return ContextCompat.getColor(context, getRatingColorRes(rating))
    }

    @JvmStatic
    fun getRatingColorRes(rating: Rating?): Int {
        return when {
            rating == null ->
                R.color.pep_no_color
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.color.pep_no_color
            rating.value < Rating.pEpRatingUndefined.value ->
                 R.color.pep_red
            rating.value < Rating.pEpRatingReliable.value ->
                R.color.pep_no_color
            rating.value < Rating.pEpRatingTrusted.value ->
                 R.color.pep_yellow
            rating.value >= Rating.pEpRatingTrusted.value ->
                R.color.pep_green
            else ->
                R.color.pep_no_color
        }
    }
}