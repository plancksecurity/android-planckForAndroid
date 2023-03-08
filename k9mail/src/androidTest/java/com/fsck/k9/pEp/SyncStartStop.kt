package com.fsck.k9.pEp

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class SyncStartStop {

    @Test
    fun start_and_stop_sync_do_it_for_all_engine_instances() {
        val app = ApplicationProvider.getApplicationContext<K9>()
        val controller = MessagingController.getInstance(app)

        val controllerThread = controller.thread
        Timber.d("EXPERIMENT CONTROLLER THREAD: $controllerThread, ${controllerThread.state}")

        runOnControllerThread { assertFalse(it.getpEpProvider().isSyncRunning) }

        Preferences.getPreferences(app).newAccount().also {
            it.setupState = Account.SetupState.READY
            it.email = "hello@hello.ch"
            it.name = "hello"
        }

        app.setpEpSyncEnabled(true)
        assertTrue(app.getpEpSyncProvider().isSyncRunning)
        runOnControllerThread { assertTrue(it.getpEpProvider().isSyncRunning) }


        app.getpEpSyncProvider().stopSync()
        assertFalse(app.getpEpSyncProvider().isSyncRunning)
        runOnControllerThread { assertFalse(it.getpEpProvider().isSyncRunning) }
    }

    private fun runOnControllerThread(block: (MessagingController) -> Unit) {
        val app = ApplicationProvider.getApplicationContext<K9>()
        val controller = MessagingController.getInstance(app)
        val controllerThread = controller.thread
        Timber.d("EXPERIMENT CONTROLLER THREAD: $controllerThread, ${controllerThread.state}")
        var called: Boolean
        controllerThread.run {
            block(controller)
            called = true
        }

        runBlocking {
            while(!called) {
                delay(100)
            }
        }
    }
}