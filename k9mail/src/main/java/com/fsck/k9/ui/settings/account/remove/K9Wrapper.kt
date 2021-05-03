package com.fsck.k9.ui.settings.account.remove

import android.content.Context
import com.fsck.k9.K9
import javax.inject.Inject
import javax.inject.Named

class K9Wrapper @Inject constructor(
    @Named("AppContext") private val application: Context
) {
    fun setServicesEnabled() {
        K9.setServicesEnabled(application)
    }
}