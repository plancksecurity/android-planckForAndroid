package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.pEpException
import kotlinx.coroutines.*
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

class KeyImportPresenter @Inject constructor(private val preferences: Preferences) {

    private lateinit var fingerprint: String
    private lateinit var view: KeyImportView
    private lateinit var accountUuid: String

    private lateinit var pEp: PEpProvider
    private lateinit var context: Context
    private lateinit var accountIdentity: Identity
    private lateinit var currentFpr: String
    private lateinit var address: String

    fun initialize(view: KeyImportView, accountUuid: String) {
        this.view = view
        this.accountUuid = accountUuid
        context = view.getApplicationContext()
        pEp = PEpProviderFactory.createAndSetupProvider(context)
        address = preferences.getAccount(accountUuid).email
        accountIdentity = PEpUtils.createIdentity(Address(address), context)
        currentFpr = pEp.myself(accountIdentity).fpr
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            view.finish()
            return
        }
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_KEY_FILE -> data.data?.let { onKeyImport(it) }
        }
    }

    private fun onKeyImport(uri: Uri) {
        view.showDialog()
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val firstIdentity = importKey(uri)
            firstIdentity?.let {
                showKeyImportConfirmationDialog(firstIdentity,
                    onYes = {
                        val result = onKeyImportConfirmed(uri)
                        replyResult(result, uri)
                    },
                    onNo = {onKeyImportRejected()}
                )
            } ?: replyResult(false, uri)
            view.removeDialog()
        }

    }

    private fun showKeyImportConfirmationDialog(firstIdentity: Identity, onYes: () -> Unit, onNo: () -> Unit) {
        view.showKeyImportConfirmationDialog(firstIdentity, onYes, onNo)
    }

    fun onKeyImportRejected() {
        view.finish()
    }

    private fun onKeyImportConfirmed(uri: Uri): Boolean {
        return runBlocking {
            var result = false
            withContext(Dispatchers.IO) {
                try {
                    val id = pEp.setOwnIdentity(accountIdentity, fingerprint)
                    if (id == null || !pEp.canEncrypt(address)) {
                        //Timber.w("Couldn't set own key: %s", key)
                        pEp.setOwnIdentity(accountIdentity, currentFpr)
                        // report bad
                        result = false
                        pEp.myself(id)
                        //replyResult(false, uri)
                    }
                    else {
                        // report all good
                        result = true
                        //replyResult(true, uri)
                    }

                } catch (e: pEpException) {  // this means there was no right formatted key in the file.
                    result = true
                    pEp.setOwnIdentity(accountIdentity, currentFpr)
                }
                finally {
                    pEp.close()
                }
            }
            result
        }
    }

    private suspend fun importKey(uri: Uri): Identity?  = withContext(Dispatchers.IO){
        var result: Identity? = null
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)
            try {
                val key = IOUtils.toByteArray(inputStream)

                val importedIdentities = pEp.importKey(key)
                if (importedIdentities.isEmpty()) { // This means that the file contains a key, but not a proper private key which we need.
                    result = null
                }
                else {
                    fingerprint = (importedIdentities[0] as Identity).fpr
                    result = importedIdentities[0]
                }
            } catch (e: IOException) {
                pEp.setOwnIdentity(accountIdentity, currentFpr)
                throw FileNotFoundException()
            } catch (e: pEpException) {  // this means there was no right formatted key in the file.
                pEp.setOwnIdentity(accountIdentity, currentFpr)
                result = null
            } finally {
                try {
                    inputStream!!.close()
                    //pEp.close()
                } catch (ignore: IOException) {
                }
            }
        } catch (e: FileNotFoundException) {
            Timber.w("Couldn't read content from URI %s", uri)
            result = null
        }
        result
    }

    private fun replyResult(success: Boolean, uri: Uri) {
        val filename = uri.path
        when {
            success -> view.showCorrectKeyImport(fingerprint, filename)
            else -> view.showFailedKeyImport(filename)
        }
    }

}