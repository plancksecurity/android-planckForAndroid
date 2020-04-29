package com.fsck.k9.pEp.importAccount

import android.app.ProgressDialog
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import kotlinx.coroutines.*
import timber.log.Timber

fun setAccountPassword(context: Context,
                       account: Account,
                       incomingPassword: String?,
                       outgoingPassword: String?) {
    val setPassword = SetAccountPassword(context, account, incomingPassword, outgoingPassword)
    setPassword.setPasswords()
}

class SetAccountPassword constructor(private val context: Context,
                                     private val account: Account,
                                     private val incomingPassword: String?,
                                     private val outgoingPassword: String?) {

    private var progressDialog: ProgressDialog? = null

    fun setPasswords() {
        runBlocking {
            showProgressDialog()
            doInBackground()
            removeProgressDialog()
        }
    }

    private suspend fun doInBackground() = withContext(Dispatchers.IO) {
        launch(Dispatchers.IO) {
            try {
                if (incomingPassword != null) {
                    val storeUri = account.storeUri
                    val incoming = RemoteStore.decodeStoreUri(storeUri)
                    val newIncoming = incoming.newPassword(incomingPassword)
                    val newStoreUri = RemoteStore.createStoreUri(newIncoming)
                    account.storeUri = newStoreUri
                }
                if (outgoingPassword != null) {
                    val transportUri = account.transportUri
                    val outgoing = Transport.decodeTransportUri(transportUri)
                    val newOutgoing = outgoing.newPassword(outgoingPassword)
                    val newTransportUri = Transport.createTransportUri(newOutgoing)
                    account.transportUri = newTransportUri
                }
                account.isEnabled = true
                account.save(Preferences.getPreferences(context))
                K9.setServicesEnabled(context)
                MessagingController.getInstance(context.applicationContext)
                        .listFolders(account, true, null)
            } catch (e: Exception) {
                Timber.e(e, "Something happened while setting account passwords")
            }
        }
        delay(1000)

    }

    private fun showProgressDialog() {
        val title = context.getString(R.string.settings_import_activate_account_header)
        val passwordCount = if (outgoingPassword == null) 1 else 2
        val message = context.resources.getQuantityString(R.plurals.settings_import_setting_passwords, passwordCount)
        progressDialog = ProgressDialog.show(context, title, message, true)
    }

    private fun removeProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        progressDialog = null
    }

}