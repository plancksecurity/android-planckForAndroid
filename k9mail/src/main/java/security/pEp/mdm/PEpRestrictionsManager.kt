package security.pEp.mdm

import android.content.RestrictionsManager

class PEpRestrictionsManager(
    restrictionsManager: RestrictionsManager,
    packageName: String,
): RestrictionsManagerContract {
    override val applicationRestrictions = restrictionsManager.applicationRestrictions
    override val manifestRestrictions = restrictionsManager.getManifestRestrictions(packageName)
}
