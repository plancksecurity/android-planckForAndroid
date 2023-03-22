package security.pEp.ui

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

class PEpUIUtilsTest : RobolectricTest() {
    private val app = ApplicationProvider.getApplicationContext<K9>()

    @Test
    fun `getDrawableForMessageList returns correct drawable for each rating and getDrawableForToolbarRating returns same drawable`() {
        assertNull(PEpUIUtils.getDrawableForMessageList(app, null))
        assertNull(PEpUIUtils.getDrawableForToolbarRating(app, null))

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnderAttack)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnderAttack)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingB0rken)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_under_attack,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingB0rken)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_mistrusted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingMistrust)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_dangerous_mistrusted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingMistrust)!!
        )

        assertNull(PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUndefined))
        assertNull(PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUndefined))

        assertDrawableEqualsRes(
            R.drawable.ico_cannot_decrypt,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingCannotDecrypt)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_cannot_decrypt,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingCannotDecrypt)!!
        )

        assertNull(PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingHaveNoKey))
        assertNull(PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingHaveNoKey))

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnencrypted)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnencrypted)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingUnreliable)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingUnreliable)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingMediaKeyProtected)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_not_encrypted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingMediaKeyProtected)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_encrypted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingReliable)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_encrypted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingReliable)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingTrusted)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingTrusted)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingTrustedAndAnonymized)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingTrustedAndAnonymized)!!
        )

        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForMessageList(app, Rating.pEpRatingFullyAnonymous)!!
        )
        assertDrawableEqualsRes(
            R.drawable.ico_trusted,
            PEpUIUtils.getDrawableForToolbarRating(app, Rating.pEpRatingFullyAnonymous)!!
        )
    }

    @Test
    fun `getRatingColor returns correct color for each rating`() {
        assertCorrectColor(R.color.pep_no_color, null)
        assertCorrectColor(R.color.pep_red, Rating.pEpRatingUnderAttack)
        assertCorrectColor(R.color.pep_red, Rating.pEpRatingB0rken)
        assertCorrectColor(R.color.pep_red, Rating.pEpRatingMistrust)
        assertCorrectColor(R.color.pep_no_color, Rating.pEpRatingUndefined)
        assertCorrectColor(R.color.pep_yellow, Rating.pEpRatingCannotDecrypt)
        assertCorrectColor(R.color.pep_no_color, Rating.pEpRatingHaveNoKey)
        assertCorrectColor(R.color.pep_yellow, Rating.pEpRatingUnencrypted)
        assertCorrectColor(R.color.pep_yellow, Rating.pEpRatingUnreliable)
        assertCorrectColor(R.color.pep_yellow, Rating.pEpRatingMediaKeyProtected)
        assertCorrectColor(R.color.pep_green, Rating.pEpRatingReliable)
        assertCorrectColor(R.color.pep_green, Rating.pEpRatingTrusted)
        assertCorrectColor(R.color.pep_green, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectColor(R.color.pep_green, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct color resource for each rating`() {
        assertCorrectColorRes(R.color.pep_no_color, null)
        assertCorrectColorRes(R.color.pep_red, Rating.pEpRatingUnderAttack)
        assertCorrectColorRes(R.color.pep_red, Rating.pEpRatingB0rken)
        assertCorrectColorRes(R.color.pep_red, Rating.pEpRatingMistrust)
        assertCorrectColorRes(R.color.pep_no_color, Rating.pEpRatingUndefined)
        assertCorrectColorRes(R.color.pep_yellow, Rating.pEpRatingCannotDecrypt)
        assertCorrectColorRes(R.color.pep_no_color, Rating.pEpRatingHaveNoKey)
        assertCorrectColorRes(R.color.pep_yellow, Rating.pEpRatingUnencrypted)
        assertCorrectColorRes(R.color.pep_yellow, Rating.pEpRatingUnreliable)
        assertCorrectColorRes(R.color.pep_yellow, Rating.pEpRatingMediaKeyProtected)
        assertCorrectColorRes(R.color.pep_green, Rating.pEpRatingReliable)
        assertCorrectColorRes(R.color.pep_green, Rating.pEpRatingTrusted)
        assertCorrectColorRes(R.color.pep_green, Rating.pEpRatingTrustedAndAnonymized)
        assertCorrectColorRes(R.color.pep_green, Rating.pEpRatingFullyAnonymous)
    }

    @Test
    fun `getRatingColorRes returns correct string resource for each rating`() {
        assertCorrectStringRes(R.string.pep_rating_none, null)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingUnderAttack)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingB0rken)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingMistrust)
        assertCorrectStringRes(R.string.pep_rating_none, Rating.pEpRatingUndefined)
        assertCorrectStringRes(R.string.pep_rating_cannot_decrypt, Rating.pEpRatingCannotDecrypt)
        assertCorrectStringRes(R.string.pep_rating_none, Rating.pEpRatingHaveNoKey)
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
        assertCorrectStringRes(R.string.pep_rating_none, null, false)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingUnderAttack, false)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingB0rken, false)
        assertCorrectStringRes(R.string.pep_rating_dangerous, Rating.pEpRatingMistrust, false)
        assertCorrectStringRes(R.string.pep_rating_none, Rating.pEpRatingUndefined, false)
        assertCorrectStringRes(
            R.string.pep_rating_cannot_decrypt,
            Rating.pEpRatingCannotDecrypt,
            false
        )
        assertCorrectStringRes(R.string.pep_rating_none, Rating.pEpRatingHaveNoKey, false)
        assertCorrectStringRes(
            R.string.pep_rating_not_encrypted,
            Rating.pEpRatingUnencrypted,
            false
        )
        assertCorrectStringRes(
            R.string.pep_rating_forced_unencrypt,
            Rating.pEpRatingUnreliable,
            false
        )
        assertCorrectStringRes(
            R.string.pep_rating_forced_unencrypt,
            Rating.pEpRatingMediaKeyProtected,
            false
        )
        assertCorrectStringRes(
            R.string.pep_rating_forced_unencrypt,
            Rating.pEpRatingReliable,
            false
        )
        assertCorrectStringRes(R.string.pep_rating_forced_unencrypt, Rating.pEpRatingTrusted, false)
        assertCorrectStringRes(
            R.string.pep_rating_forced_unencrypt,
            Rating.pEpRatingTrustedAndAnonymized,
            false
        )
        assertCorrectStringRes(
            R.string.pep_rating_forced_unencrypt,
            Rating.pEpRatingFullyAnonymous,
            false
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
        assertCorrectVisibility(View.GONE, Rating.pEpRatingHaveNoKey)
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
        assertCorrectVisibility(View.GONE, null, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnderAttack, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingB0rken, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMistrust, enabled = false)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingUndefined, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingCannotDecrypt, enabled = false)
        assertCorrectVisibility(View.GONE, Rating.pEpRatingHaveNoKey, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnencrypted, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingUnreliable, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingMediaKeyProtected, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingReliable, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrusted, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingTrustedAndAnonymized, enabled = false)
        assertCorrectVisibility(View.VISIBLE, Rating.pEpRatingFullyAnonymous, enabled = false)
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
        enabled: Boolean = true,
        forceHide: Boolean = false
    ) {
        assertEquals(expected, PEpUIUtils.getToolbarRatingVisibility(rating, enabled, forceHide))
    }

    private fun assertCorrectStringRes(
        @StringRes expected: Int,
        rating: Rating?,
        enabled: Boolean = true
    ) {
        assertEquals(expected, PEpUIUtils.getRatingTextRes(rating, enabled))
    }

    private fun assertCorrectColorRes(@ColorRes expected: Int, rating: Rating?) {
        assertEquals(expected, PEpUIUtils.getRatingColorRes(rating))
    }

    private fun assertCorrectColor(@ColorRes expected: Int, rating: Rating?) {
        assertEquals(getColor(expected), PEpUIUtils.getRatingColor(app, rating))
    }

    private fun getColor(@ColorRes res: Int): Int = ContextCompat.getColor(app, res)
}