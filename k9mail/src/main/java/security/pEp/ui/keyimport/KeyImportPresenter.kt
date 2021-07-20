package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.exceptions.pEpException
import kotlinx.coroutines.*
import org.apache.commons.io.IOUtils
import security.pEp.ui.keyimport.KeyImportActivity.Companion.ACTIVITY_REQUEST_PICK_KEY_FILE
import security.pEp.ui.keyimport.KeyImportActivity.Companion.IMPORT_STATE
import security.pEp.ui.keyimport.KeyImportActivity.Companion.SAVED_STATE_URI
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class KeyImportPresenter @Inject constructor(
    private val preferences: Preferences,
    @Named("NewInstance") private val pEp: PEpProvider
) {

    private lateinit var fingerprint: String
    private lateinit var view: KeyImportView
    private lateinit var accountUuid: String

    private lateinit var context: Context
    private lateinit var accountIdentity: Identity
    private lateinit var currentFpr: String
    private lateinit var address: String
    private lateinit var importState: ImportState

    private var uri: Uri? = null

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initialize(view: KeyImportView, accountUuid: String) {
        this.view = view
        this.accountUuid = accountUuid
        this.importState = ImportState.KEY_NOT_SELECTED
        scope.launch {
            context = view.getApplicationContext()
            address = preferences.getAccount(accountUuid).email
            accountIdentity = PEpUtils.createIdentity(Address(address), context)
            withContext(Dispatchers.IO) { currentFpr = pEp.myself(accountIdentity).fpr }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            this.importState = ImportState.KEY_NOT_SELECTED
            view.finish()
            return
        }
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_KEY_FILE -> data.data?.let { uri ->
                this.uri = uri
                this.importState = ImportState.KEY_SELECTED
                onKeyImport()
            }
        }
    }

    fun onKeyImport() {
        uri?.let { uri ->
            val filename = uri.path.toString()
            scope.launch {
                view.showLayout()
                view.showLoading()
                val firstIdentity = importKey(uri)
                view.hideLoading()
                firstIdentity?.let {
                    view.showKeyImportConfirmationDialog(firstIdentity, filename)
                    view.showLayout()
                } ?: replyResult(false)
            }
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

    private suspend fun importKey(uri: Uri): Identity? = withContext(Dispatchers.IO) {
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

    private fun replyResult(success: Boolean) {
        view.hideLoading()
        when {
            success -> {
                this.importState = ImportState.KEY_IMPORTED
                view.showCorrectKeyImport()
            }
            else -> {
                this.importState = ImportState.KEY_NOT_IMPORTED
                view.showFailedKeyImport()
            }
        }
    }

    fun onKeyImportAccepted() {
        view.hideLayout()
        scope.launch {
            val result = onKeyImportConfirmed()
            replyResult(result)
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SAVED_STATE_URI, uri?.toString())
        outState.putSerializable(IMPORT_STATE, this.importState)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val uriString = savedInstanceState.getString(SAVED_STATE_URI)
            if (!uriString.isNullOrEmpty()) {
                uri = Uri.parse(uriString)
            }
            this.importState = savedInstanceState.get(IMPORT_STATE) as ImportState
        }

    }

    fun resumeImport() {
        when (this.importState) {
            ImportState.KEY_NOT_SELECTED ->
                view.openFileChooser()
            ImportState.KEY_SELECTED ->
                onKeyImport()
            ImportState.KEY_IMPORTED ->
                view.showCorrectKeyImport()
            ImportState.KEY_NOT_IMPORTED ->
                view.showFailedKeyImport()
            else -> {
                // NOOP
            }
        }
    }

    fun fileManagerOpened() {
        this.importState = ImportState.FILE_MANAGER_OPEN
    }

    enum class ImportState {
        KEY_NOT_SELECTED,
        FILE_MANAGER_OPEN,
        KEY_SELECTED,
        KEY_IMPORTED,
        KEY_NOT_IMPORTED
    }
}