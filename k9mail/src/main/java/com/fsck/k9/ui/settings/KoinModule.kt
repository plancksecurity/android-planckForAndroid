package com.fsck.k9.ui.settings

import com.fsck.k9.helper.FileBrowserHelper
import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.account.AccountsLiveData
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import security.pEp.permissions.PermissionChecker
import security.pEp.permissions.PermissionRequester
import security.pEp.ui.permissions.PEpPermissionChecker
import security.pEp.ui.permissions.PepPermissionRequester
import java.util.concurrent.Executors

val settingsUiModule = module {
    single { AccountsLiveData(preferences = get()) }
    viewModel { SettingsViewModel(accounts = get()) }

    single { FileBrowserHelper.getInstance() }
    factory { GeneralSettingsDataStore(context = get(), preferences = get(),
            executorService = get(named("SaveSettingsExecutorService"))) }
    single(named("SaveSettingsExecutorService")) {
        Executors.newSingleThreadExecutor(NamedThreadFactory(threadNamePrefix = "SaveSettings"))
    }

    single<PermissionChecker> { PEpPermissionChecker(get()) }
    factory<PermissionRequester> {PepPermissionRequester(get()) }

    viewModel { AccountSettingsViewModel(preferences = get(), folderRepositoryManager = get()) }
    single { AccountSettingsDataStoreFactory(context = get(), preferences = get(),
            executorService = get(named("SaveSettingsExecutorService"))) }
}
