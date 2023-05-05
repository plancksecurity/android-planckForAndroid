package com.fsck.k9.ui.settings

import com.fsck.k9.helper.FileBrowserHelper
import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.account.AccountsLiveData
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext
import security.planck.permissions.PermissionChecker
import security.planck.permissions.PermissionRequester
import security.planck.ui.permissions.PEpPermissionChecker
import security.planck.ui.permissions.PepPermissionRequester
import java.util.concurrent.Executors

val settingsUiModule = applicationContext {
    bean { AccountsLiveData(get()) }
    viewModel { SettingsViewModel(get()) }

    bean { FileBrowserHelper.getInstance() }
    bean { GeneralSettingsDataStore(get(), get(), get("SaveSettingsExecutorService")) }
    bean("SaveSettingsExecutorService") {
        Executors.newSingleThreadExecutor(NamedThreadFactory("SaveSettings"))
    }

    bean<PermissionChecker> { PEpPermissionChecker(get()) }
    factory<PermissionRequester> { params -> PepPermissionRequester(params["activity"]) }

    viewModel { AccountSettingsViewModel(get(), get()) }
    bean { AccountSettingsDataStoreFactory(get(), get(), get("SaveSettingsExecutorService")) }
}
