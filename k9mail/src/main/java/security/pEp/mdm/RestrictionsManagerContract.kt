package security.pEp.mdm

import android.content.RestrictionEntry
import android.os.Bundle

interface RestrictionsManagerContract {
    val applicationRestrictions: Bundle
    val manifestRestrictions: List<RestrictionEntry>
}