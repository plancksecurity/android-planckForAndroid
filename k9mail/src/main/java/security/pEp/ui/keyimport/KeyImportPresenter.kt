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
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        this.view = view
        this.accountUuid = accountUuid
        scope.launch {
            context = view.getApplicationContext()
            pEp = PEpProviderFactory.createAndSetupProvider(context)
            address = preferences.getAccount(accountUuid).email
            accountIdentity = PEpUtils.createIdentity(Address(address), context)
            withContext(Dispatchers.IO) { currentFpr = pEp.myself(accountIdentity).fpr }
        }
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
        val filename = uri.path.toString()
        view.showDialog()
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val firstIdentity = importKey(uri)
            view.removeDialog()
            firstIdentity?.let {
                view.showKeyImportConfirmationDialog(firstIdentity, filename)
            } ?: replyResult(false, filename)
        }
    }

    fun onKeyImportRejected() {
        view.finish()
    }

    private suspend fun onKeyImportConfirmed(): Boolean {
        return withContext(Dispatchers.IO) {
            var result = false
            runBlocking {
                try {
                    val id = pEp.setOwnIdentity(accountIdentity, fingerprint)
                    result = if (id == null || !pEp.canEncrypt(address)) {
                        Timber.w("Couldn't set own key: %s", fingerprint)
                        pEp.setOwnIdentity(accountIdentity, currentFpr)
                        false
                    } else {
                        pEp.myself(id)
                        true
                    }

                } catch (e: pEpException) {  // this means there was no right formatted key in the file.
                    result = true
                    pEp.setOwnIdentity(accountIdentity, currentFpr)
                }
            }
            result
        }
    }

    private suspend fun importKey(uri: Uri): Identity?  = withContext(Dispatchers.IO){
        var result: Identity?
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
                    result = importedIdentities.firstOrNull { identity -> identity.address == address }
                    result?.let {
                        fingerprint = (importedIdentities[0] as Identity).fpr
                        result = importedIdentities[0]
                    }
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

    private fun replyResult(success: Boolean, filename: String) {
        when {
            success -> view.showCorrectKeyImport(fingerprint, filename)
            else -> view.showFailedKeyImport(filename)
        }
    }

    fun onKeyImportAccepted(filename: String) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch {
            val result = onKeyImportConfirmed()
            replyResult(result, filename)
        }
    }

}