package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.exceptions.pEpException
import kotlinx.coroutines.*
import org.apache.commons.io.IOUtils
import security.pEp.ui.keyimport.KeyImportActivity.Companion.ACTIVITY_REQUEST_PICK_KEY_FILE
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

    private lateinit var context: Context
    private lateinit var accountIdentities: List<Identity>
    private lateinit var currentFprs: List<String>
    private lateinit var addresses: List<String>

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initialize(view: KeyImportView, accountUuid: String) {
        this.view = view

        scope.launch {
            context = view.getApplicationContext()
            addresses = if(accountUuid.isNotEmpty()) {
                listOf(preferences.getAccount(accountUuid).email)
            } else {
                preferences.availableAccounts.map { it.email }
            }
            accountIdentities = addresses.map {address -> PEpUtils.createIdentity(Address(address), context) }
            withContext(Dispatchers.IO) { currentFprs = accountIdentities.map { accountIdentity -> pEp.myself(accountIdentity).fpr } }
            view.openFileChooser()
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

        scope.launch {
            view.showLoading()
            val importedIdentities = importKey(uri)
            view.hideLoading()
            if(importedIdentities.isNotEmpty()) {
                view.showKeyImportConfirmationDialog(importedIdentities, filename)
            } else {
                replyResult(emptyList(), filename)
            }
        }
    }

    fun onKeyImportRejected() {
        view.finish()
    }

    private suspend fun onKeyImportConfirmed(): List<Identity> {
        return withContext(Dispatchers.IO) {
            val result: MutableList<Identity> = mutableListOf()
            var currentResult = false
            runBlocking {
                accountIdentities.forEachIndexed { index, accountIdentity ->
                    try {
                        val id = pEp.setOwnIdentity(accountIdentity, fingerprint)
                        currentResult = if (id == null || !pEp.canEncrypt(addresses[index])) {
                            Timber.w("Couldn't set own key: %s", fingerprint)
                            pEp.setOwnIdentity(accountIdentity, currentFprs[index])
                            false
                        } else {
                            pEp.myself(id)
                            true
                        }

                    } catch (e: pEpException) {  // this means there was no right formatted key in the file.
                        currentResult = false
                        pEp.setOwnIdentity(accountIdentity, currentFprs[index])
                    }
                    if(currentResult) {
                        result.add(accountIdentity)
                    }
                }
            }
            result
        }
    }

    private suspend fun importKey(uri: Uri): List<Identity>  = withContext(Dispatchers.IO){
        var result: List<Identity> = emptyList()
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)
            try {
                val key = IOUtils.toByteArray(inputStream)

                val importedIdentities = pEp.importKey(key)
                if (importedIdentities.isEmpty()) { // This means that the file contains a key, but not a proper private key which we need.
                    result = emptyList()
                }
                else {
                    result = importedIdentities.filter { identity -> identity.address in addresses }.groupBy { it.address }.values.map { it.first() }
                    fingerprint = result.first().fpr
                }
            } catch (e: IOException) {
                //pEp.setOwnIdentity(accountIdentity, currentFpr)
                throw FileNotFoundException()
            } catch (e: pEpException) {  // this means there was no right formatted key in the file.
                //pEp.setOwnIdentity(accountIdentity, currentFpr)
                result = emptyList()
            } finally {
                try {
                    inputStream!!.close()
                    //pEp.close()
                } catch (ignore: IOException) {
                }
            }
        } catch (e: FileNotFoundException) {
            Timber.w("Couldn't read content from URI %s", uri)
            result = emptyList()
        }
        result
    }

    private fun replyResult(result: List<Identity>, filename: String) {
        view.hideLoading()
        when {
            result.isNotEmpty() -> view.showCorrectKeyImport(result, filename)
            else -> view.showFailedKeyImport(filename)
        }
    }

    fun onKeyImportAccepted(filename: String) {
        scope.launch {
            val result = onKeyImportConfirmed()
            replyResult(result, filename)
        }
    }

}