package com.fsck.k9

import android.app.Application
import android.content.Context
import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.mail.TransportProvider
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.mailstore.mailStoreModule
import com.fsck.k9.ui.endtoend.endToEndUiModule
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.settings.settingsUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

object DI {
    private val mainModule = module {
        single { Preferences.getPreferences(get()) }
        single { MessagingController.getInstance(get()) }
        single { TransportProvider() }
        single { get<Context>().resources }
        single { StorageManager.getInstance(get()) }
        single { FolderNameFormatter(get()) }
    }

    private val appModules = listOf(
        mainModule,
        settingsUiModule,
        //unreadWidgetModule,
        endToEndUiModule,
        openPgpModule,
        autocryptModule,
        mailStoreModule
    )

    @JvmStatic
    fun start(application: Application) {
        @Suppress("ConstantConditionIf")
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(application)
            modules(appModules)
        }
    }

}
