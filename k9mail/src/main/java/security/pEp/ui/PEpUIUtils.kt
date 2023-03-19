package security.pEp.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.pEp.PEpUtils.isRatingUnsecure
import com.fsck.k9.pEp.models.FolderModel
import foundation.pEp.jniadapter.Rating

object PEpUIUtils {

    @JvmStatic
    fun accountNameSummary(word: String): String {
        return word.take(1).ifBlank { "?" }
    }

    @JvmStatic
    fun getDrawableForRating(context: Context, rating: Rating?): Drawable? {
        return when(rating){
            Rating.pEpRatingUndefined -> ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            Rating.pEpRatingCannotDecrypt -> ContextCompat.getDrawable(context, R.drawable.pep_status_cannot_decrypt)
            Rating.pEpRatingHaveNoKey -> ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            Rating.pEpRatingUnencrypted -> ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            Rating.pEpRatingUnencryptedForSome -> ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            Rating.pEpRatingUnreliable -> ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            Rating.pEpRatingReliable -> ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            Rating.pEpRatingTrusted -> ContextCompat.getDrawable(context, R.drawable.pep_status_trusted)
            Rating.pEpRatingTrustedAndAnonymized -> if (BuildConfig.IS_ENTERPRISE) ContextCompat.getDrawable(context, R.drawable.pep_status_gray) else  ContextCompat.getDrawable(context, R.drawable.pep_status_trusted)
            Rating.pEpRatingFullyAnonymous -> if (BuildConfig.IS_ENTERPRISE) ContextCompat.getDrawable(context, R.drawable.pep_status_gray) else ContextCompat.getDrawable(context, R.drawable.pep_status_trusted)
            Rating.pEpRatingMistrust -> ContextCompat.getDrawable(context, R.drawable.pep_status_mistrusted)
            Rating.pEpRatingB0rken -> ContextCompat.getDrawable(context, R.drawable.pep_status_under_attack)
            Rating.pEpRatingUnderAttack -> ContextCompat.getDrawable(context, R.drawable.pep_status_under_attack)
            null -> ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
        }
    }

    @JvmStatic
    fun getDrawableForRatingBordered(context: Context, rating: Rating?): Drawable? {
        if(BuildConfig.IS_ENTERPRISE) {
            return when {
                rating == null ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
                rating.value == Rating.pEpRatingCannotDecrypt.value ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_cannot_decrypt_bordered)
                rating.value == Rating.pEpRatingMistrust.value ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_mistrusted_bordered)
                rating.value == Rating.pEpRatingUnderAttack.value || rating.value == Rating.pEpRatingB0rken.value -> //-2 and under
                    ContextCompat.getDrawable(context, R.drawable.pep_status_under_attack_bordered)
                rating.value == Rating.pEpRatingTrusted.value ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_trusted_bordered)
                rating.value == Rating.pEpRatingReliable.value ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_green_bordered)
                rating.value >= Rating.pEpRatingUnencrypted.value
                        || rating.value == Rating.pEpRatingUnencryptedForSome.value
                        || rating.value == Rating.pEpRatingUnreliable.value -> //3 to 5
                    ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_bordered)
                else ->
                    ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            }
        }
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
            isRatingUnsecure(rating) ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_bordered)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red_bordered)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green_bordered)
            rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
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
            isRatingUnsecure(rating) ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_white)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red_white)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green_dark)
            rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_white)
            else ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray_white)
        }
    }

    @JvmStatic
    fun getDrawableForToolbarRating(context: Context, rating: Rating?): Drawable? {
        if(BuildConfig.IS_ENTERPRISE) {
            return getDrawableForRating(context, rating)
        }
        return when {
            rating == null ->
                ColorDrawable(Color.TRANSPARENT)
            isRatingUnsecure(rating) ->
                if (BuildConfig.IS_ENTERPRISE) {
                    ContextCompat.getDrawable(context, R.drawable.enterprise_status_unsecure)
                } else {
                    ColorDrawable(Color.TRANSPARENT)
                }
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            else ->
                ColorDrawable(Color.TRANSPARENT)
        }
    }

    @JvmStatic
    fun getDrawableForMessageList(context: Context, rating: Rating?): Drawable? {
        if(BuildConfig.IS_ENTERPRISE) {
            return getDrawableForRating(context, rating)
        }

        return when {
            rating == null ->
                null
            BuildConfig.IS_ENTERPRISE
                    && (
                    rating == Rating.pEpRatingCannotDecrypt
                            || rating == Rating.pEpRatingHaveNoKey
                    ) -> null
            isRatingUnsecure(rating) ->
                if (BuildConfig.IS_ENTERPRISE) {
                    ContextCompat.getDrawable(context, R.drawable.enterprise_status_unsecure)
                } else {
                    null
                }
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow)
            else ->
                null
        }
    }

    @JvmStatic
    fun getToolbarRatingVisibility(
        rating: Rating?,
        pEpEnabled: Boolean = true,
        forceHide: Boolean = false,
    ): Int {
        return when {
            forceHide || rating == null ->
                View.GONE
            BuildConfig.IS_ENTERPRISE
                    && (
                    rating != Rating.pEpRatingUndefined
                            && rating != Rating.pEpRatingHaveNoKey
                            && rating != Rating.pEpRatingTrustedAndAnonymized
                            && rating != Rating.pEpRatingFullyAnonymous
                    ) -> View.VISIBLE
            isRatingUnsecure(rating) ->
                View.GONE
            !pEpEnabled ->
                View.VISIBLE
            rating.value == Rating.pEpRatingMistrust.value || rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
                View.VISIBLE
            else ->
                View.GONE
        }
    }


    @JvmStatic
    @JvmOverloads
    fun getRatingColor(context: Context, rating: Rating?, pEpEnabled: Boolean = true): Int {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;
        return ContextCompat.getColor(context, getRatingColorRes(rating, pEpEnabled))
    }

    @JvmStatic
    fun getRatingColorRes(rating: Rating?, pEpEnabled: Boolean = true): Int {
        if(BuildConfig.IS_ENTERPRISE){
            return when {
                !pEpEnabled || rating == null ->
                    R.color.pep_no_color
                rating == Rating.pEpRatingReliable ||
                        rating == Rating.pEpRatingTrusted ->
                    R.color.pep_green
                rating == Rating.pEpRatingUnencrypted ||
                        rating == Rating.pEpRatingUnencryptedForSome ||
                        rating == Rating.pEpRatingUnreliable ||
                        rating == Rating.pEpRatingCannotDecrypt->
                    R.color.pep_yellow
                rating == Rating.pEpRatingMistrust ||
                        rating == Rating.pEpRatingUnderAttack->
                    R.color.pep_red
                else ->
                    R.color.pep_no_color
            }
        }
        return when {
            !pEpEnabled || rating == null ->
                R.color.pep_no_color
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.color.pep_no_color
            rating.value < Rating.pEpRatingUndefined.value ->
                R.color.pep_red
            rating.value < Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
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
    fun getRatingTextRes(rating: Rating?, pEpEnabled: Boolean = true): Int {
        if(BuildConfig.IS_ENTERPRISE){
            if(!pEpEnabled)
                return R.string.pep_rating_forced_unencrypt
            return when(rating) {
                null -> R.string.pep_rating_none
                Rating.pEpRatingUndefined -> R.string.pep_rating_undefined
                Rating.pEpRatingCannotDecrypt -> R.string.pep_rating_cannot_decrypt
                Rating.pEpRatingHaveNoKey -> R.string.pep_rating_no_key
                Rating.pEpRatingUnencrypted -> R.string.pep_rating_unencrypt
                Rating.pEpRatingUnencryptedForSome -> R.string.pep_rating_unencrypted_for_some
                Rating.pEpRatingUnreliable -> R.string.pep_rating_unreliable
                Rating.pEpRatingReliable -> R.string.pep_rating_secure
                Rating.pEpRatingTrusted -> R.string.pep_rating_trusted
                Rating.pEpRatingTrustedAndAnonymized -> R.string.pep_rating_trusted_anon
                Rating.pEpRatingFullyAnonymous -> R.string.pep_rating_full_anon
                Rating.pEpRatingMistrust -> R.string.pep_rating_mistrusted
                Rating.pEpRatingB0rken -> R.string.pep_rating_broken
                Rating.pEpRatingUnderAttack -> R.string.pep_rating_under_attack
                else -> R.string.pep_rating_none
            }

        }
        return when {
            rating == null ->
                R.string.pep_rating_none
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.string.pep_rating_none
            BuildConfig.IS_ENTERPRISE && rating == Rating.pEpRatingCannotDecrypt ->
                R.string.pep_rating_none
            BuildConfig.IS_ENTERPRISE && isRatingUnsecure(rating) ->
                R.string.enterprise_unsecure
            rating == Rating.pEpRatingUndefined ->
                R.string.pep_rating_none
            !pEpEnabled ->
                R.string.pep_rating_forced_unencrypt
            rating.value < Rating.pEpRatingUndefined.value ->
                R.string.pep_rating_mistrusted
            rating.value < Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
                R.string.pep_rating_none
            rating.value < Rating.pEpRatingTrusted.value ->
                R.string.pep_rating_secure
            rating.value >= Rating.pEpRatingTrusted.value ->
                R.string.pep_rating_trusted
            else ->
                R.string.pep_rating_none
        }
    }

    @JvmStatic
    fun orderFolderLists(account: Account, folders: MutableList<FolderModel>): MutableList<FolderModel> {

        val specialFolders = folders.getSpecialFolders(account)
        return folders
                .filterNot { folder ->
                    val folderName = folder.localFolder.name
                    account.isSpecialFolder(folderName)
                }
                .toMutableList()
                .addSpecialFoldersOrderedToTopOfList(specialFolders)
    }

    @JvmStatic
    fun orderFolderInfoLists(account: Account, folders: MutableList<FolderInfoHolder>): MutableList<FolderInfoHolder> {
        val specialFolders = folders.getSpecialFolders(account)

        return folders
                .filterNot { folder ->
                    val folderName = folder.name
                    account.isSpecialFolder(folderName)
                }
                .toMutableList()
                .addSpecialFoldersOrderedToTopOfList(specialFolders)
    }

    @JvmStatic
    fun getColorAsString(context: Context, @ColorRes color: Int): String =
            "#${Integer.toHexString(ContextCompat.getColor(context, color) and 0x00ffffff)}"

    fun Context.isValidGlideContext(): Boolean {
        val baseContext = if(this is ContextWrapper) baseContext else this
        return baseContext !is Activity || (!baseContext.isDestroyed && !baseContext.isFinishing)
    }

    private fun <E : Any> MutableList<E>.getSpecialFolders(account: Account): List<E?> {
        val inboxFolderName = account.inboxFolderName
        val sentFolderName = account.sentFolderName
        val draftsFolderName = account.draftsFolderName
        val outboxFolderName = account.outboxFolderName
        val archiveFolderName = account.archiveFolderName
        val spamFolderName = account.spamFolderName
        val trashFolderName = account.trashFolderName

        val inboxFolder = this.find { orderFolderPredicate(it, inboxFolderName) }
        val sentFolder = this.find { orderFolderPredicate(it, sentFolderName) }
        val draftsFolder = this.find { orderFolderPredicate(it, draftsFolderName) }
        val outboxFolder = this.find { orderFolderPredicate(it, outboxFolderName) }
        val archiveFolder = this.find { orderFolderPredicate(it, archiveFolderName) }
        val spamFolder = this.find { orderFolderPredicate(it, spamFolderName) }
        val trashFolder = this.find { orderFolderPredicate(it, trashFolderName) }
        return listOf(inboxFolder, sentFolder, draftsFolder, outboxFolder, archiveFolder, spamFolder, trashFolder)
    }

    private fun orderFolderPredicate(folderName: Any, name: String?): Boolean {
        return when (folderName) {
            is FolderModel -> folderName.localFolder.name == name
            is FolderInfoHolder -> folderName.name == name
            else -> false
        }

    }

    private fun <E : Any> MutableList<E>.addSpecialFoldersOrderedToTopOfList(specialFolders: List<E?>): MutableList<E> {
        specialFolders
                .filterNotNull()
                .forEachIndexed { index, folder -> this.add(index, folder) }
        return this
    }


}



