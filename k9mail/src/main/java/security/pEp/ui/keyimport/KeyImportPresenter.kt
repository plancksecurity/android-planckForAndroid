package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.DispatcherProvider
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
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@Suppress("BlockingMethodInNonBlockingContext")
class KeyImportPresenter @Inject constructor(
        private val preferences: Preferences,
        @Named("NewInstance") private val pEp: PEpProvider,
        @Named("AppContext") private val context: Context,
        private val dispatcherProvider: DispatcherProvider
) {

    private lateinit var fingerprint: String
    private lateinit var view: KeyImportView

    private lateinit var accountIdentities: List<Identity>
    private lateinit var currentFprs: List<String>
    private lateinit var addresses: List<String>
    private var keyImportMode = KeyImportMode.GENERAL_SETTINGS // whether we are working for a single account or all of them.

    private val scope: CoroutineScope = CoroutineScope(dispatcherProvider.main() + SupervisorJob())

    fun initialize(view: KeyImportView, accountUuid: String = "") {
        this.view = view

        keyImportMode = if(accountUuid.isNotEmpty()) KeyImportMode.ACCOUNT_SETTINGS else KeyImportMode.GENERAL_SETTINGS
        scope.launch {
            initializeAddresses(accountUuid)
            initializeIdentities()
            initializeFingerPrints()
            view.openFileChooser()
        }
    }

    private fun initializeIdentities() {
        accountIdentities = addresses.map { address -> PEpUtils.createIdentity(Address(address), context) }
    }

    private suspend fun initializeFingerPrints() = withContext(dispatcherProvider.io()) {
        currentFprs = accountIdentities.map { accountIdentity -> pEp.myself(accountIdentity).fpr }
    }

    private fun initializeAddresses(accountUuid: String) {
        if (accountUuid.isNotEmpty()) {
            keyImportMode = KeyImportMode.ACCOUNT_SETTINGS
            addresses = listOf(preferences.getAccount(accountUuid).email)
        } else {
            keyImportMode = KeyImportMode.GENERAL_SETTINGS
            addresses = preferences.availableAccounts.map { it.email }
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

    @VisibleForTesting
    fun onKeyImport(uri: Uri) {
        val filename = uri.path.toString()

        scope.launch {
            view.showLoading()
            val importedIdentities = importKey(uri)
            view.hideLoading()
            if(importedIdentities.isNotEmpty()) {
                view.showKeyImportConfirmationDialog(importedIdentities, filename)
            } else {
                view.showFailedKeyImport(filename)
            }
        }
    }

    fun onKeyImportRejected() {
        scope.launch {
            withContext(dispatcherProvider.io()) {
                pEp.close()
            }
        }
        view.finish()
    }

    private suspend fun onKeyImportConfirmed(importedIdentities: List<Identity>): Map<Identity, Boolean> {
        return withContext(dispatcherProvider.io()) {
            val result: MutableMap<Identity, Boolean> = mutableMapOf()
            var currentResult: Boolean
            runBlocking {
                importedIdentities.forEach { identity ->
                    val position = addresses.indexOf(identity.address)
                    if(position >= 0) { // address is setup in device
                        val currentIdentity = accountIdentities[position]
                        val currentFpr = currentFprs[position]
                        try {
                            val id = pEp.setOwnIdentity(currentIdentity, fingerprint)
                            currentResult = if (id == null || !pEp.canEncrypt(addresses[position])) {
                                Timber.w("Couldn't set own key: %s :: %s", identity.address, fingerprint)
                                pEp.setOwnIdentity(currentIdentity, currentFpr)
                                false
                            } else {
                                pEp.myself(id)
                                true
                            }

                        } catch (e: pEpException) {
                            currentResult = false
                            pEp.setOwnIdentity(currentIdentity, currentFpr)
                        }
                    }
                    else { // address is not setup in device: create and identity to set it as own key
                        currentResult = try {
                            val id = pEp.setOwnIdentity(pEp.myself(identity), fingerprint)
                            if (id == null) {
                                Timber.w("Couldn't set own key: %s :: %s", identity.address, fingerprint)
                                false
                            } else {
                                true
                            }
                        } catch (e: pEpException) {
                            false
                        }
                    }
                    result[identity] = currentResult
                }
            }
            pEp.close()
            result
        }
    }

    @VisibleForTesting
    suspend fun importKey(uri: Uri): List<Identity>  = withContext(dispatcherProvider.io()){
        var result: List<Identity>
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)
            try {
                val key = IOUtils.toByteArray(inputStream)

                val importedVector: Vector<Identity>? = pEp.importKey(key)
                val importedIdentities: List<Identity>  = when {
                    importedVector == null -> emptyList()
                    keyImportMode == KeyImportMode.GENERAL_SETTINGS -> importedVector
                    else -> importedVector.filter { it.address in addresses }
                }
                if (importedIdentities.isEmpty()) { // This means that the file contains a key, but not a proper private key which we need.
                    result = emptyList()
                } else {
                    result = importedIdentities.groupBy { it.address }.values.map { it.first() }
                    fingerprint = result.first().fpr
                }
            } catch (e: IOException) {
                throw FileNotFoundException()
            } catch (e: pEpException) {  // this means there was no right formatted key in the file.
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
            pEp.close()
        }
        result
    }

    private fun replyResult(result: Map<Identity, Boolean>, filename: String) {
        view.hideLoading()
        when {
            keyImportMode == KeyImportMode.ACCOUNT_SETTINGS -> replyResult(result.isNotEmpty() && result.values.first(), filename)
            result.count { it.value } == 0 -> view.showFailedKeyImport(filename)
            else -> view.showKeyImportResult(result, filename)
        }
    }

    private fun replyResult(success: Boolean, filename: String) {
        when {
            success -> view.showCorrectKeyImport(fingerprint, filename)
            else -> view.showFailedKeyImport(filename)
        }
    }

    fun onKeyImportAccepted(importedIdentities: List<Identity>, filename: String) {
        scope.launch {
            val result = onKeyImportConfirmed(importedIdentities)
            replyResult(result, filename)
        }
    }

    enum class KeyImportMode {
        ACCOUNT_SETTINGS, GENERAL_SETTINGS
    }
}