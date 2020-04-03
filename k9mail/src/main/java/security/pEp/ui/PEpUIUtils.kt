package security.pEp.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.fsck.k9.Account
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
            isRatingUnsecure(rating) ->
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
            isRatingUnsecure(rating) ->
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
            isRatingUnsecure(rating) ->
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
            isRatingUnsecure(rating) ->
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
            rating == null ||
                    isRatingUnsecure(rating) ->
                View.GONE
            !encrypt ->
                View.VISIBLE
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
            !encrypt || rating == null || rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey ->
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
            rating == null || rating == Rating.pEpRatingB0rken || rating == Rating.pEpRatingHaveNoKey || rating == Rating.pEpRatingUndefined ->
                R.string.pep_rating_none
            !encrypt ->
                R.string.pep_rating_forced_unencrypt
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

    @JvmStatic
    fun orderFolderLists(account: Account, folders: MutableList<FolderModel>): MutableList<FolderModel> {
        val inboxFolderName = account.inboxFolderName
        val sentFolderName = account.sentFolderName
        val draftsFolderName = account.draftsFolderName
        val outboxFolderName = account.outboxFolderName
        val archiveFolderName = account.archiveFolderName
        val spamFolderName = account.spamFolderName
        val trashFolderName = account.trashFolderName

        val inboxFolder = folders.find { folderModel -> folderModel.localFolder.name == inboxFolderName }
        val sentFolder = folders.find { folderModel -> folderModel.localFolder.name == sentFolderName }
        val draftsFolder = folders.find { folderModel -> folderModel.localFolder.name == draftsFolderName }
        val outboxFolder = folders.find { folderModel -> folderModel.localFolder.name == outboxFolderName }
        val archiveFolder = folders.find { folderModel -> folderModel.localFolder.name == archiveFolderName }
        val spamFolder = folders.find { folderModel -> folderModel.localFolder.name == spamFolderName }
        val trashFolder = folders.find { folderModel -> folderModel.localFolder.name == trashFolderName }
        val specialFolders = listOf(inboxFolder, sentFolder, draftsFolder, outboxFolder, archiveFolder, spamFolder, trashFolder)

        return folders
                .filterNot { folder ->
                    val folderName = folder.localFolder.name
                    account.isSpecialFolder(folderName)
                }
                .toMutableList()
                .addSpecialFoldersOrderedToTopOfList(specialFolders)
    }

    @JvmStatic
    fun orderFolderInfoLists(account: Account, folders: List<FolderInfoHolder>): MutableList<FolderInfoHolder> {
        val inboxFolderName = account.inboxFolderName
        val sentFolderName = account.sentFolderName
        val draftsFolderName = account.draftsFolderName
        val outboxFolderName = account.outboxFolderName
        val archiveFolderName = account.archiveFolderName
        val spamFolderName = account.spamFolderName
        val trashFolderName = account.trashFolderName

        val inboxFolder = folders.find { folderModel -> folderModel.name == inboxFolderName }
        val sentFolder = folders.find { folderModel -> folderModel.name == sentFolderName }
        val draftsFolder = folders.find { folderModel -> folderModel.name == draftsFolderName }
        val outboxFolder = folders.find { folderModel -> folderModel.name == outboxFolderName }
        val archiveFolder = folders.find { folderModel -> folderModel.name == archiveFolderName }
        val spamFolder = folders.find { folderModel -> folderModel.name == spamFolderName }
        val trashFolder = folders.find { folderModel -> folderModel.name == trashFolderName }
        val specialFolders = listOf(inboxFolder, sentFolder, draftsFolder, outboxFolder, archiveFolder, spamFolder, trashFolder)

        return folders
                .filterNot { folder ->
                    val folderName = folder.name
                    account.isSpecialFolder(folderName)
                }
                .toMutableList()
                .addSpecialFoldersOrderedToTopOfList(specialFolders)
    }

}

private fun <E : Any> MutableList<E>.addSpecialFoldersOrderedToTopOfList(specialFolders: List<E?>): MutableList<E> {
    specialFolders
            .filterNotNull()
            .forEachIndexed { index, folder -> this.add(index, folder) }
    return this
}

