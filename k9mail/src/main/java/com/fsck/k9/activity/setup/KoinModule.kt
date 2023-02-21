package com.fsck.k9.activity.setup

import com.fsck.k9.auth.JwtTokenDecoder
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val authModule = applicationContext {
    factory { JwtTokenDecoder() }
    viewModel {
        AuthViewModel(
            application = get(),
            accountManager = get(),
            oAuthConfigurationProvider = get(),
            jwtTokenDecoder = get()
        )
    }
}
