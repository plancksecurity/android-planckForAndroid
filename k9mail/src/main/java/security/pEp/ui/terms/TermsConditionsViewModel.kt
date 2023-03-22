package security.pEp.ui.terms

import androidx.lifecycle.ViewModel
import com.fsck.k9.BuildConfig

class TermsConditionsViewModel : ViewModel() {
    fun getTermsLink(): String {
        return BuildConfig.TERMS_CONDITIONS_LINK
    }
}