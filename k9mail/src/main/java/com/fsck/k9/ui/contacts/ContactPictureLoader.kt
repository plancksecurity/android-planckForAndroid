package com.fsck.k9.ui.contacts

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.FutureTarget
import com.fsck.k9.R
import com.fsck.k9.activity.compose.Recipient
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Address
import security.pEp.permissions.PermissionChecker
import security.pEp.ui.PEpUIUtils.isValidGlideContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max


class ContactPictureLoader @Inject constructor(
        @Named("AppContext") private val context: Context,
        private val contactLetterBitmapCreator: ContactLetterBitmapCreator,
        private val permissionChecker: PermissionChecker
) {

    private val contactsHelper: Contacts = Contacts.getInstance(context)
    private val backgroundCacheId: String = with(contactLetterBitmapCreator.config) {
        if (hasDefaultBackgroundColor) defaultBackgroundColor.toString() else "*"
    }

    fun setContactPicture(imageView: ImageView, address: Address) {
        if(imageView.context.isValidGlideContext()) {
            Glide.with(imageView.context)
                .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                .from(Address::class.java)
                .`as`(Bitmap::class.java)
                .decoder(ContactImageBitmapDecoder())
                .signature(contactLetterBitmapCreator.signatureOf(address))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(address)
                .dontAnimate()
                .transform(CircleTransform(imageView.context))
                .into(imageView)
        }
    }

    private inner class ContactImageBitmapDecoder(
            private val contactLetterOnly: Boolean = false
    ) : ResourceDecoder<Address, Bitmap> {

        override fun decode(address: Address, width: Int, height: Int): Resource<Bitmap> {
            val pool = Glide.get(context).bitmapPool

            val size = max(width, height)

            val bitmap = loadContactPicture(address)
                    ?: createContactLetterBitmap(address, size, pool)

            return BitmapResource.obtain(bitmap, pool)
        }

        private fun loadContactPicture(address: Address): Bitmap? {
            if (contactLetterOnly || !permissionChecker.hasContactsPermission()) return null

            val photoUri = contactsHelper.getPhotoUri(address.address) ?: return null
            return try {
                context.contentResolver.openInputStream(photoUri).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Timber.e(e, "Couldn't load contact picture: $photoUri")
                null
            }
        }

        private fun createContactLetterBitmap(address: Address, size: Int, pool: BitmapPool): Bitmap {
            val bitmap = pool.getDirty(size, size, Bitmap.Config.ARGB_8888)
                    ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            return contactLetterBitmapCreator.drawBitmap(bitmap, size, address)
        }

        override fun getId(): String {
            return "fallback-photo"
        }
    }

    private class AddressModelLoader(val backgroundCacheId: String) : ModelLoader<Address, Address> {
        override fun getResourceFetcher(address: Address, width: Int, height: Int): DataFetcher<Address> {
            return object : DataFetcher<Address> {
                override fun getId() = "${address.address}-${address.personal}-$backgroundCacheId"

                override fun loadData(priority: Priority?): Address = address

                override fun cleanup() = Unit
                override fun cancel() = Unit
            }
        }
    }

    private fun <T> FutureTarget<T>.getOrNull(): T? {
        return try {
            get()
        } catch (e: Exception) {
            null
        }
    }

    private fun Int.toDip(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    companion object {
        /**
         * Resize the pictures to the following value (device-independent pixels).
         */
        private const val PICTURE_SIZE = 40
    }

    class CircleTransform(context: Context?) : BitmapTransformation(context) {
        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            return circleCrop(pool, toTransform)!!
        }

        override fun getId(): String {
            return javaClass.name
        }

        companion object {
            private fun circleCrop(pool: BitmapPool, source: Bitmap?): Bitmap? {
                if (source == null) return null
                val size = Math.min(source.width, source.height)
                val x = (source.width - size) / 2
                val y = (source.height - size) / 2
                // TODO this could be acquired from the pool too
                val squared = Bitmap.createBitmap(source, x, y, size, size)
                var result = pool[size, size, Bitmap.Config.ARGB_8888]
                if (result == null) {
                    result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                }
                val canvas = Canvas(result!!)
                val paint = Paint()
                paint.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                paint.isAntiAlias = true
                val r = size / 2f
                canvas.drawCircle(r, r, r, paint)
                return result
            }
        }
    }
}
