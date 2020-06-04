package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.pEpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

class KeyImportPresenter @Inject constructor() {

    private lateinit var fingerprint: String
    private lateinit var view: KeyImportView
    private lateinit var account: String

    fun initialize(view: KeyImportView, account: String) {
        this.view = view
        this.account = account
    }

    fun onAccept(fingerprint: String) {
        val trimmedFingerprint = fingerprint.replace(" ", "")
        if (trimmedFingerprint.isEmpty()) {
            view.showEmptyInputError()
        } else {
            this.fingerprint = trimmedFingerprint
            view.openFileChooser()
        }
    }

    fun onReject() {
        view.finish()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_KEY_FILE -> data.data?.let { onKeyImport(it) }
        }
    }

    private fun onKeyImport(uri: Uri) {
        runBlocking {
            view.showDialog()
            val success = importKey(uri)
            replyResult(success, uri)
            view.removeDialog()
        }

    }


    private suspend fun importKey(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        var result = true
        try {
            val context = view.getApplicationContext()
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)
            val pEp = PEpProviderFactory.createAndSetupProvider(context)
            val accountIdentity = PEpUtils.createIdentity(Address(account), context)
            val currentFpr = pEp.myself(accountIdentity).fpr
            try {
                val key = IOUtils.toByteArray(inputStream)
                pEp.importKey(key)
                val id = pEp.setOwnIdentity(accountIdentity, fingerprint)
                if (id == null || !pEp.canEncrypt(account)) {
                    Timber.w("Couldn't set own key: %s", key)
                    pEp.setOwnIdentity(accountIdentity, currentFpr)
                    result = false
                }
            } catch (e: IOException) {
                pEp.setOwnIdentity(accountIdentity, currentFpr)
                throw FileNotFoundException()
            } catch (e: pEpException) {
                pEp.setOwnIdentity(accountIdentity, currentFpr)
                result = false
            } finally {
                try {
                    inputStream!!.close()
                    pEp.close()
                } catch (ignore: IOException) {
                }
            }
        } catch (e: FileNotFoundException) {
            Timber.w("Couldn't read content from URI %s", uri)
            result = false
        }
        result
    }

    private fun replyResult(success: Boolean, uri: Uri) {
        val filename = uri.path
        when {
            success -> view.showCorrectKeyImport(fingerprint, filename)
            else -> view.showFailedKeyImport(fingerprint, filename)
        }
    }

}