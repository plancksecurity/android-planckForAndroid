package com.fsck.k9.ui.endtoend

import androidx.lifecycle.LifecycleOwner
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val endToEndUiModule = module {
    factory { AutocryptSetupMessageLiveEvent(messageCreator = get()) }
    factory { AutocryptSetupTransferLiveEvent() }
    factory { (lifecycleOwner: LifecycleOwner, autocryptTransferView: AutocryptKeyTransferActivity) ->
        AutocryptKeyTransferPresenter(
                lifecycleOwner = lifecycleOwner,
                context = get(),
                openPgpApiManager = get { parametersOf(lifecycleOwner) },
                transportProvider = get(),
                preferences = get(),
                viewModel = get(),
                view = autocryptTransferView)
    }
    viewModel { AutocryptKeyTransferViewModel(autocryptSetupMessageLiveEvent = get(),
            autocryptSetupTransferLiveEvent = get()) }
}