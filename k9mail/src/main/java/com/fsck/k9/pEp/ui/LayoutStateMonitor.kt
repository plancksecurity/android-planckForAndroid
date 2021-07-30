package com.fsck.k9.pEp.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.util.Consumer
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor

abstract class LayoutStateMonitor {
    private lateinit var windowManager: WindowManager
    private val layoutStateChangeCallback = LayoutStateChangeCallback()

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor {
            handler.post(it)
        }
    }

    private inner class LayoutStateChangeCallback : Consumer<WindowLayoutInfo> {
        override fun accept(windowLayoutInfo: WindowLayoutInfo) {
            onLayoutStateChanged(windowLayoutInfo)
        }
    }

    abstract fun onLayoutStateChanged(windowLayoutInfo: WindowLayoutInfo)

    fun register() {
        windowManager.registerLayoutChangeCallback(
            runOnUiThreadExecutor(),
            layoutStateChangeCallback
        )
    }

    fun unRegister() {
        windowManager.unregisterLayoutChangeCallback(layoutStateChangeCallback)
    }

    fun initialize(context: Context) {
        windowManager = WindowManager(context)
    }

}