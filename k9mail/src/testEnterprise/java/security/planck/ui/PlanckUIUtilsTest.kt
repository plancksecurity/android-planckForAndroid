package security.planck.ui

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.RobolectricTest
import foundation.pEp.jniadapter.Rating
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PlanckUIUtilsTest : RobolectricTest() {
    private val app = ApplicationProvider.getApplicationContext<K9>()

    @Test
    fun `getDrawableForMessageList returns correct drawable for each rating and getDrawableForToolbarRating returns same drawable`() {
        assertNull(PlanckUIUtils.getDrawableForMessageList(app, null))
        assertNull(PlanckUIUtils.getDrawableForToolbarRating(app, null))

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnderAttack)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnderAttack)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingB0rken)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingB0rken)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_mistrusted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingMistrust)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_mistrusted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingMistrust)!!
        )

        assertNull(PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUndefined))
        assertNull(PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUndefined))

        assertDrawableEqualsRes(
            R.drawable.ico_cannot_decrypt,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingCannotDecrypt)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_cannot_decrypt,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingCannotDecrypt)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingHaveNoKey)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingHaveNoKey)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingHaveNoKey, outgoing = true)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnencrypted)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnencrypted)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnreliable)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnreliable)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingMediaKeyProtected)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingMediaKeyProtected)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_encrypted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingReliable)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_encrypted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingReliable)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingTrusted)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingTrusted)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingTrustedAndAnonymized)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingTrustedAndAnonymized)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForMessageList(app, Rating.pEpRatingFullyAnonymous)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingFullyAnonymous)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_inactive,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnencrypted, planckInactive = true)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_inactive,
            PlanckUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingHaveNoKey, planckInactive = true, outgoing = true)!!
        )
    }

    @Test
    fun `getRatingColor returns correct color for each rating`() {
        assertCorrectColor(R.color.planck_no_color, null)
        assertCorrectColor(R.color.planck_red, Rating.pEpRatingUnderAttack)
        assertCorrectColor(R.color.planck_red, Rating.pEpRatingB0rken)
        assertCorrectColor(R.color.planck_red, Rating.pEpRatingMistrust)
        assertCorrectColor(R.color.planck_no_color, Rating.pEpRatingUndefined)
        assertCorrectColor(R.color.planck_yellow, Rating.pEpRatingCannotDecrypt)
        assertCorrectColor(R.color.planck_red, Rating.pEpRatingHaveNoKey)
        assertCorrectColor(R.color.planck_yellow, Rating.pEpRatingUnencrypted)
        assertCorrectColor(R.color.planck_yellow, Rating.pEpRatingUnreliable)
        assertCorrectColor(R.color.planck_yellow, Rating.pEpRatingMediaKeyProtected)
        assertCorrectColor(R.color.planck_green, Rating.pEpRatingReliable)
        assertCorrectColor(R.color.planck_green, Rating.pEpRatingTrusted)
        assertCorrectColor(R.color.planck_green, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectColor(R.color.planck_green, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct color resource for each rating`() {
        assertCorrectColorRes(R.color.planck_no_color, null)
        assertCorrectColorRes(R.color.planck_red, Rating.pEpRatingUnderAttack)
        assertCorrectColorRes(R.color.planck_red, Rating.pEpRatingB0rken)
        assertCorrectColorRes(R.color.planck_red, Rating.pEpRatingMistrust)
        assertCorrectColorRes(R.color.planck_no_color, Rating.pEpRatingUndefined)
        assertCorrectColorRes(R.color.planck_yellow, Rating.pEpRatingCannotDecrypt)
        assertCorrectColorRes(R.color.planck_red, Rating.pEpRatingHaveNoKey)
        assertCorrectColorRes(R.color.planck_yellow, Rating.pEpRatingUnencrypted)
        assertCorrectColorRes(R.color.planck_yellow, Rating.pEpRatingUnreliable)
        assertCorrectColorRes(R.color.planck_yellow, Rating.pEpRatingMediaKeyProtected)
        assertCorrectColorRes(R.color.planck_green, Rating.pEpRatingReliable)
        assertCorrectColorRes(R.color.planck_green, Rating.pEpRatingTrusted)
        assertCorrectColorRes(R.color.planck_green, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectColorRes(R.color.planck_green, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct string resource for each rating`() {
        assertCorrectStringRes(R.string.pep_rating_none, null)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingUnderAttack)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingB0rken)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingMistrust)
        assertCorrectStringRes(R.string.pep_rating_none, Rating.pEpRatingUndefined)
        assertCorrectStringRes(R.string.pep_rating_cannot_decrypt, Rating.pEpRatingCannotDecrypt)
        assertCorrectStringRes(R.string.pep_rating_found_no_key, Rating.pEpRatingHaveNoKey)
        assertCorrectStringRes(R.string.pep_rating_not_encrypted, Rating.pEpRatingUnencrypted)
        assertCorrectStringRes(R.string.pep_rating_weakly_encrypted, Rating.pEpRatingUnreliable)
        assertCorrectStringRes(R.string.pep_rating_encrypted, Rating.pEpRatingMediaKeyProtected)
        assertCorrectStringRes(R.string.pep_rating_encrypted, Rating.pEpRatingReliable)
        assertCorrectStringRes(R.string.pep_rating_trusted, Rating.pEpRatingTrusted)
        assertCorrectStringRes(R.string.pep_rating_trusted, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectStringRes(R.string.pep_rating_trusted, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct string resource for each rating with protection disabled`() {
        assertCorrectStringRes(R.string.planck_inactive, null, true)
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingUnderAttack, true)
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingB0rken, true)
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingMistrust, true)
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingUndefined, true)
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingCannotDecrypt,
            true
        )
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingHaveNoKey, true)
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingUnencrypted,
            true
        )
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingUnreliable,
            true
        )
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingMediaKeyProtected,
            true
        )
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingReliable,
            true
        )
        assertCorrectStringRes(R.string.planck_inactive, Rating.pEpRatingTrusted, true)
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingTrustedAndAnonymized,
            true
        )
        assertCorrectStringRes(
            R.string.planck_inactive,
            Rating.pEpRatingFullyAnonymous,
            true
        )
    }

    @Test
    fun `getRatingColorRes returns correct visibility for each rating`() {
        assertCorrectVisibility(View.GONE, null)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnderAttack)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingB0rken)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMistrust)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUndefined)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingCannotDecrypt)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingHaveNoKey)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingHaveNoKey, outgoing = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnencrypted)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnreliable)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMediaKeyProtected)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingReliable)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrusted)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct visibility for each rating with protection disabled`() {
        assertCorrectVisibility(View.VISIBLE, null, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnderAttack, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingB0rken, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMistrust, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUndefined, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingCannotDecrypt, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingHaveNoKey, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnencrypted, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnreliable, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMediaKeyProtected, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingReliable, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrusted, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrustedAndAnonymized, inactive = true)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingFullyAnonymous, inactive = true)
    }

    @Test
    fun `getRatingColorRes returns correct visibility for each rating with forceHide`() {
        assertCorrectVisibility(View.GONE, null, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUnderAttack, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingB0rken, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingMistrust, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUndefined, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingCannotDecrypt, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingHaveNoKey, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingHaveNoKey, forceHide = true, outgoing = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUnencrypted, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUnreliable, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingMediaKeyProtected, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingReliable, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingTrusted, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingTrustedAndAnonymized, forceHide = true)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingFullyAnonymous, forceHide = true)
    }

    private fun assertCorrectVisibility(
        expected: Int,
        rating: Rating?,
        inactive: Boolean = false,
        forceHide: Boolean = false,
        outgoing: Boolean = false,
    ) {
        assertEquals(expected, PlanckUIUtils.getToolbarRatingVisibility(rating, outgoing, inactive, forceHide))
    }

    private fun assertCorrectStringRes(
        @StringRes expected: Int,
        rating: Rating?,
        planckInactive: Boolean = false,
        outgoing: Boolean = false,
    ) {
        assertEquals(expected, PlanckUIUtils.getRatingTextRes(rating, outgoing, planckInactive))
    }

    private fun assertCorrectColorRes(@ColorRes expected: Int, rating: Rating?) {
        assertEquals(expected, PlanckUIUtils.getRatingColorRes(rating))
    }

    private fun assertCorrectColor(@ColorRes expected: Int, rating: Rating?) {
        assertEquals(getColor(expected), PlanckUIUtils.getRatingColor(app, rating))
    }

    private fun getColor(@ColorRes res: Int): Int = ContextCompat.getColor(app, res)
}