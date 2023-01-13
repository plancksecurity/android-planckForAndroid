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
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            isRatingUnsecure(rating) ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_gray)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_red)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_green)
            rating.value >= Rating.pEpRatingUnreliable.value -> // TODO: change this to the media key rating when implemented on engine side.
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
        return when {
            rating == null ->
                ColorDrawable(Color.TRANSPARENT)
            isRatingUnsecure(rating) ->
                if (BuildConfig.IS_END_USER) {
                    ColorDrawable(Color.TRANSPARENT)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.enterprise_status_unsecure)
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
        return when {
            rating == null ->
                null
            !BuildConfig.IS_END_USER
                    && (
                    rating == Rating.pEpRatingCannotDecrypt
                            || rating == Rating.pEpRatingHaveNoKey
                    ) -> null
            isRatingUnsecure(rating) ->
                if (BuildConfig.IS_END_USER) {
                    null
                } else {
                    ContextCompat.getDrawable(context, R.drawable.enterprise_status_unsecure)
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
            !BuildConfig.IS_END_USER
                    && (
                        rating == Rating.pEpRatingCannotDecrypt
                                || rating == Rating.pEpRatingHaveNoKey
                    ) -> View.GONE
            isRatingUnsecure(rating) ->
                if (BuildConfig.IS_END_USER) View.GONE
                else View.VISIBLE
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
        return when {
            !pEpEnabled || rating == null ->
                R.color.pep_no_color
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.color.pep_no_color
            !BuildConfig.IS_END_USER && rating == Rating.pEpRatingCannotDecrypt ->
                R.color.pep_no_color
            !BuildConfig.IS_END_USER && isRatingUnsecure(rating) ->
                R.color.compose_unsecure_delivery_warning
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
        return when {
            rating == null ->
                R.string.pep_rating_none
            rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
                R.string.pep_rating_none
            !BuildConfig.IS_END_USER && rating == Rating.pEpRatingCannotDecrypt ->
                R.string.pep_rating_none
            !BuildConfig.IS_END_USER && isRatingUnsecure(rating) ->
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
                R.string.pep_rating_secure_trusted
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



