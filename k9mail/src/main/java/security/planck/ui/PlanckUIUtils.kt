package security.planck.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.planck.PlanckUtils.isRatingUnsecure
import com.fsck.k9.planck.models.FolderModel
import foundation.pEp.jniadapter.Rating

object PlanckUIUtils {
    private val ratingDisplay: RatingDisplay by lazy { RatingDisplay.getInstance() }

    @JvmStatic
    fun accountNameSummary(word: String): String {
        return word.take(1).ifBlank { "?" }
    }

    @JvmStatic
    fun getDrawableForRatingBordered(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_bordered)
            isRatingUnsecure(rating) ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_bordered)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_red_bordered)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_green_bordered)
            rating.value >= Rating.pEpRatingUnreliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_bordered)
            else ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_bordered)
        }
    }

    @JvmStatic
    fun getDrawableForRatingRecipient(context: Context, rating: Rating?): Drawable? {
        return when {
            rating == null ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_white)
            isRatingUnsecure(rating) ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_white)
            rating.value == Rating.pEpRatingMistrust.value ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_red_white)
            rating.value >= Rating.pEpRatingTrusted.value ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_green_dark)
            rating.value >= Rating.pEpRatingUnreliable.value ->
                ContextCompat.getDrawable(context, R.drawable.pep_status_yellow_white)
            else ->
                ContextCompat.getDrawable(context, R.drawable.planck_status_gray_white)
        }
    }

    @JvmStatic
    fun getDrawableForToolbarRating(context: Context, rating: Rating?): Drawable? {
        return ratingDisplay.getForRating(rating).getDrawable(context)
    }

    @JvmStatic
    fun getDrawableForMessageList(context: Context, rating: Rating?): Drawable? {
        return ratingDisplay.getForRating(rating).getDrawable(context)
    }

    @JvmStatic
    fun getToolbarRatingVisibility(
        rating: Rating?,
        pEpEnabled: Boolean = true,
        forceHide: Boolean = false,
    ): Int {
        val hide = forceHide || !ratingDisplay.getForRating(rating, pEpEnabled).visible
        return if (hide) View.GONE else View.VISIBLE
    }


    @JvmStatic
    @JvmOverloads
    fun getRatingColor(context: Context, rating: Rating?, pEpEnabled: Boolean = true): Int {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;
        return ContextCompat.getColor(context, getRatingColorRes(rating, pEpEnabled))
    }

    @JvmStatic
    fun getRatingColorRes(rating: Rating?, pEpEnabled: Boolean = true): Int {
        return ratingDisplay.getForRating(rating, pEpEnabled).colorRes
    }

    @JvmStatic
    fun getRatingTextRes(rating: Rating?, pEpEnabled: Boolean = true): Int {
        return ratingDisplay.getForRating(rating, pEpEnabled).textRes
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



