package com.fsck.k9

import android.app.Application
import com.fsck.k9.activity.setup.authModule
import com.fsck.k9.auth.createOAuthConfigurationProvider
import com.fsck.k9.autodiscovery.providersxml.autodiscoveryProvidersXmlModule
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.folders.FolderNameFormatter
import org.koin.Koin
import org.koin.KoinContext
import org.koin.android.ext.koin.with
import org.koin.android.logger.AndroidLogger
import org.koin.core.parameter.Parameters
import org.koin.dsl.module.applicationContext
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext

object DI {
    private val mainModule = applicationContext {
        bean { MessagingController.getInstance(get()) }
        bean { FolderNameFormatter(get()) }
        bean { createOAuthConfigurationProvider() }
    }

    val appModules = listOf(
            mainModule,
            //unreadWidgetModule,
            authModule,
            autodiscoveryProvidersXmlModule,
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
