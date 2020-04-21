package com.fsck.k9.pEp.importAccount

import android.app.Activity
import android.app.ProgressDialog
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import kotlinx.coroutines.*
import timber.log.Timber

fun setPasswords(activity: Activity,
                 account: Account,
                 incomingPassword: String?,
                 outgoingPassword: String?,
                 remainingAccounts: List<Account>) {
    SetPasswords(activity, account, incomingPassword, outgoingPassword, remainingAccounts)
            .setPasswords()
}

class SetPasswords constructor(private val activity: Activity,
                               private val account: Account,
                               private val incomingPassword: String?,
                               private val outgoingPassword: String?,
                               private val remainingAccounts: List<Account>) {

    private var progressDialog: ProgressDialog? = null

    fun setPasswords() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val startTime = System.currentTimeMillis()
            showProgressDialog()
            doInBackground()
            val elapsedTime = System.currentTimeMillis() - startTime
            sleep(Math.max(0, 1000 - elapsedTime))
            removeProgressDialog()
            onPostExecute()
        }
    }

    private suspend fun sleep(millis: Long) = withContext(Dispatchers.IO) {
        Thread.sleep(millis)
    }

    private suspend fun doInBackground() = withContext(Dispatchers.IO) {
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
            account.save(Preferences.getPreferences(activity))
            K9.setServicesEnabled(activity)
            MessagingController.getInstance(activity.application)
                    .listFolders(account, true, null)
        } catch (e: Exception) {
            Timber.e(e, "Something went while setting account passwords")
        }
    }

    private fun onPostExecute() {
        val mActivity = activity as PEpImporterActivity
        mActivity.setNonConfigurationInstance(null)
        mActivity.refresh()
        removeProgressDialog()
        if (remainingAccounts.isNotEmpty()) {
            mActivity.promptForServerPasswords(remainingAccounts)
        }
        mActivity.onImportFinished()
    }

    private fun showProgressDialog() {
        val title = activity.getString(R.string.settings_import_activate_account_header)
        val passwordCount = if (outgoingPassword == null) 1 else 2
        val message = activity.resources.getQuantityString(R.plurals.settings_import_setting_passwords, passwordCount)
        progressDialog = ProgressDialog.show(activity, title, message, true)
    }

    private fun removeProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        progressDialog = null
    }

}