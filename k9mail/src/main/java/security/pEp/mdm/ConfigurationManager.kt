package security.pEp.mdm

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ConfigurationManager(
    private val context: Context,
    private val preferences: Preferences) {

    private var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null
    private lateinit var accounts: MutableList<Account>
    private val k9: K9 = context.applicationContext as K9

    fun loadConfigurations() {
        CoroutineScope(Dispatchers.Main).launch {
            loadConfigurationsSuspend()
                .onSuccess { sendRemoteConfig() }
                .onFailure {
                    Timber.e(
                        it,
                        "Could not load configurations after registering the receiver"
                    )
                }
        }
    }

    private suspend fun loadConfigurationsSuspend(): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            accounts = preferences.accounts
            val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE)
                    as RestrictionsManager
            val restrictions = manager.applicationRestrictions
            val entries = manager.getManifestRestrictions(context.applicationContext?.packageName)
            mapRestrictions(entries, restrictions)
            saveAccounts()
        }
    }

    private fun mapRestrictions(entries: List<RestrictionEntry>, restrictions: Bundle) {
        entries.forEach { entry ->
            when (entry.key) {
                RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION -> savePrivacyProtection(restrictions, entry)
                RESTRICTION_PEP_EXTRA_KEYS -> saveExtrasKeys(restrictions, entry)
                RESTRICTION_PEP_USE_TRUSTWORDS -> saveUseTrustwords(restrictions, entry)
                RESTRICTION_PEP_UNSECURE_DELIVERY_WARNING -> saveUnsecureDeliveryWarning(restrictions, entry)
                RESTRICTION_PEP_SYNC_FOLDER -> savepEpSyncFolder(restrictions, entry)
                RESTRICTION_PEP_DEBUG_LOG -> savepEpDebugLog(restrictions, entry)
            }
        }
    }

    private fun saveUnsecureDeliveryWarning(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                k9.setpEpForwardWarningEnabled(it.value)
            }
        }
    }

    private fun savepEpSyncFolder(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                K9.setUsingpEpSyncFolder(it.value)
            }
        }
    }

    private fun savepEpDebugLog(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                K9.setDebug(it.value)
            }
        }
    }

    private fun saveExtrasKeys(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<List<ExtraKey>>()?.toManageableSetting()
            config?.let {newExtraKeys ->
                val currentKeys = K9.getMasterKeys().toSet()
                newExtraKeys.value.forEach { extraKey ->
                    currentKeys.plus(extraKey.fpr)
                }
                K9.setMasterKeys(currentKeys)
            }
        }
    }

    private fun savePrivacyProtection(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                accounts.forEach { account ->
                    account.setpEpPrivacyProtection(config)
                }
            }

        }
    }

    private fun saveUseTrustwords(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                K9.setpEpUseTrustwords(config)
            }

        }
    }

    private fun saveAccounts() {
        accounts.forEach { account ->
            account.save(preferences)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendRemoteConfig() {
        listener?.updatedRestrictions()
    }

    fun unregisterReceiver() {
        if (restrictionsReceiver != null) {
            context.applicationContext.unregisterReceiver(restrictionsReceiver)
            restrictionsReceiver = null
        }
    }

    fun registerReceiver() {
        if (restrictionsReceiver == null) {
            restrictionsReceiver = RestrictionsReceiver(this)
        }
        context.applicationContext.registerReceiver(
                restrictionsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        )
    }

    fun setListener(listener: RestrictionsListener) {
        this.listener = listener
    }
}