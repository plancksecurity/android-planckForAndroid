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
    private val mainModule = applicationContext {
        bean { Preferences.getPreferences(get()) }
        bean { MessagingController.getInstance(get()) }
        bean { TransportProvider() }
        bean { get<Context>().resources }
        bean { StorageManager.getInstance(get()) }
        bean { FolderNameFormatter(get()) }
    }

    val appModules = listOf(
            mainModule,
            settingsUiModule,
            //unreadWidgetModule,
            endToEndUiModule,
            openPgpModule,
            autocryptModule,
            mailStoreModule
    )

    @JvmStatic fun start(application: Application) {
        @Suppress("ConstantConditionIf")
        Koin.logger = if (BuildConfig.DEBUG) AndroidLogger() else EmptyLogger()

        StandAloneContext.startKoin(appModules) with application
    }

    @JvmOverloads
    @JvmStatic
    fun <T : Any> get(clazz: Class<T>, name: String = "", parameters: Parameters = { emptyMap() }): T {
        val koinContext = StandAloneContext.koinContext as KoinContext
        val kClass = clazz.kotlin

        return if (name.isEmpty()) {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchAll(kClass) }
        } else {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchByName(name) }
        }
    }

    inline fun <reified T : Any> get(name: String = "", noinline parameters: Parameters = { emptyMap() }): T {
        val koinContext = StandAloneContext.koinContext as KoinContext
        return koinContext.get(name, parameters)
    }
}
