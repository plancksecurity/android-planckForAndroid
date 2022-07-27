package security.pEp.mdm

import android.content.RestrictionEntry
import android.content.RestrictionsManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.fsck.k9.BuildConfig
import javax.inject.Singleton

interface RestrictionsManagerContract {
    val applicationRestrictions: Bundle
    val manifestRestrictions: List<RestrictionEntry>
}

class PEpRestrictionsManager(
    restrictionsManager: RestrictionsManager,
    packageName: String,
): RestrictionsManagerContract {
    override val applicationRestrictions = restrictionsManager.applicationRestrictions
    override val manifestRestrictions = restrictionsManager.getManifestRestrictions(packageName)
}

@Singleton
class FakeRestrictionsManager: RestrictionsManagerContract {
    override var applicationRestrictions: Bundle = getProvisioningRestrictions()
    override var manifestRestrictions: List<RestrictionEntry> = getStartupManifestRestrictions()

    companion object {
        fun getProvisioningRestrictions(): Bundle {
            return Bundle().apply {
                putString(RESTRICTION_PROVISIONING_URL, "https://provisioningtest")
                putBundle(
                    RESTRICTION_ACCOUNT_MAIL_SETTINGS,
                    getMailSettingsBundle()
                )
            }
        }

        fun getStartupManifestRestrictions(): List<RestrictionEntry> {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                listOf(
                    RestrictionEntry(RESTRICTION_PROVISIONING_URL, "defaultUrl"),
                    getMailSettingsRestrictionEntry()
                )
            } else emptyList()
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun getMailSettingsRestrictionEntry(): RestrictionEntry =
            RestrictionEntry.createBundleEntry(
            RESTRICTION_ACCOUNT_MAIL_SETTINGS,
            arrayOf(
                RestrictionEntry(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, "{{mail}}"),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER,
                            ""
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME,
                            "{{mail}}"
                        ),
                    )
                ),
                RestrictionEntry.createBundleEntry(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                    arrayOf(
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER,
                            ""
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT,
                            888
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE,
                            "STARTTLS"
                        ),
                        RestrictionEntry(
                            RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME,
                            "{{mail}}"
                        ),
                    )
                ),
            )
        )

        private fun getMailSettingsBundle(): Bundle = Bundle().apply {
            putString(RESTRICTION_ACCOUNT_EMAIL_ADDRESS, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
            putBundle(
                RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS,
                bundleOf(
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER to
                            BuildConfig.PEP_TEST_EMAIL_SERVER,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT to 993,
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE to "SSL/TLS",
                    RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME to
                            BuildConfig.PEP_TEST_EMAIL_ADDRESS
                )
            )
            putBundle(
                RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS,
                bundleOf(
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER to
                            BuildConfig.PEP_TEST_EMAIL_SERVER,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT to 587,
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE to "STARTTLS",
                    RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME to
                            BuildConfig.PEP_TEST_EMAIL_ADDRESS
                )
            )
        }
    }
}
