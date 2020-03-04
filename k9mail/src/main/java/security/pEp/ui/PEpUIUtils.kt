package security.pEp.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import foundation.pEp.jniadapter.Rating

object PEpUIUtils {

    @JvmStatic
    fun accountNameSummary(word: String): String {
        return word.take(1).ifBlank { "?" }
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
    fun getDrawableForRatingBordered(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
            rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red_bordered)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green_bordered)
            rating.value == Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_bordered)
            else ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
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
    fun getDrawableForToolbarRating(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ColorDrawable(Color.TRANSPARENT)
            rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                ColorDrawable(Color.TRANSPARENT)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value == Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            else ->
                ColorDrawable(Color.TRANSPARENT)
        }
    }

    @JvmStatic
    fun getDrawableForMessageList(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                null
            rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                null
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value == Rating.pEpRatingReliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            else ->
                null
        }
    }

    @JvmStatic
    fun getToolbarRatingVisibility(rating: Rating?, encrypt: Boolean = true): Int {
        return when {
            !encrypt ->
                View.VISIBLE
            rating == null ||
                    rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value ->
                View.GONE
            rating.value == Rating.pEpRatingMistrust.value || rating.value >= Rating.pEpRatingReliable.value ->
                View.VISIBLE
            else ->
                View.GONE
        }
    }


    @JvmStatic
    fun getRatingColor(context: Context, rating: Rating?): Int {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;
        return ContextCompat.getColor(context, getRatingColorRes(rating))
    }

    @JvmStatic
    fun getRatingColorRes(rating: Rating?, encrypt: Boolean = true): Int {
        return when {
            !encrypt ->
                R.color.pep_no_color
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

    @JvmStatic
    fun getRatingTextRes(rating: Rating?, encrypt: Boolean = true): Int {
        return when {
            !encrypt ->
                R.string.pep_rating_forced_unencrypt
            rating == null ->
                R.string.pep_rating_none
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.string.pep_rating_none
            rating.value < Rating.pEpRatingUndefined.value ->
                R.string.pep_rating_mistrusted
            rating.value < Rating.pEpRatingReliable.value ->
                R.string.pep_rating_none
            rating.value < Rating.pEpRatingTrusted.value ->
                R.string.pep_rating_secure
            rating.value >= Rating.pEpRatingTrusted.value ->
                R.string.pep_rating_secure_trusted
            else ->
                R.string.pep_rating_none
        }
    }


}