package com.fsck.k9.activity.setup

import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val authModule = applicationContext {
    viewModel { AuthViewModel(application = get(), accountManager = get(), oAuthConfigurationProvider = get()) }
}
