package com.fsck.k9.helper

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.mdm.ManageableSetting
import java.io.File

private const val CURRENT_APP_VERSION = 531L

@ExperimentalCoroutinesApi
class AppUpdaterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val packageInfo: PackageInfo = mockk {
        every { longVersionCode }.returns(CURRENT_APP_VERSION)
    }
    private val packageManager: PackageManager = mockk {
        every { getPackageInfo(any<String>(), any<Int>()) }.returns(packageInfo)
    }

    private val app: Context = mockk {
        every { packageManager }.returns(this@AppUpdaterTest.packageManager)
        every { packageName }.returns("packageName")
    }
    private val storageEditor: StorageEditor = mockk(relaxed = true)
    private val storage: Storage = mockk {
        every { edit() }.returns(storageEditor)
        every { getBoolean(any(), any()) }.returns(false)
    }
    private val preferences: Preferences = mockk {
        every { storage }.returns(this@AppUpdaterTest.storage)
    }


    private val bodyFile: File = mockk(relaxed = true) {
        every { name }.returns("body")
    }
    private val cacheFile: File = mockk {
        every { listFiles() }.returns(arrayOf(bodyFile))
        every { exists() }.returns(true)
    }
    private val appUpdater = AppUpdater(app, cacheFile, coroutinesTestRule.testDispatcherProvider)

    @Before
    fun setUp() {
        mockkStatic(Preferences::class)
        every { Preferences.getPreferences(any()) }.returns(preferences)
        mockkStatic(K9::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Preferences::class)
        unmockkStatic(K9::class)
    }

    @Test
    fun `AppUpdater updates for any version if there was no version saved`() = runTest {
        every { storage.getLong("appVersionCode", -1) }.returns(-1)


        appUpdater.performOperationsOnUpdate()
        advanceUntilIdle()


        verify { verifyV317Update() }
        verify { verifyV3111Update() }
    }

    @Test
    fun `AppUpdater updates only for the versions that are between saved and new version`() =
        runTest {
            every { storage.getLong("appVersionCode", -1) }.returns(529)


            appUpdater.performOperationsOnUpdate()
            advanceUntilIdle()


            verify(exactly = 0) { verifyV317Update() }
            verify { verifyV3111Update() }
        }

    @Test
    fun `AppUpdater does not update anything if no update target versions are in range`() =
        runTest {
            every { storage.getLong("appVersionCode", -1) }.returns(530)


            appUpdater.performOperationsOnUpdate()
            advanceUntilIdle()


            verify(exactly = 0) { verifyV317Update() }
            verify(exactly = 0) { verifyV3111Update() }
        }

    private fun verifyV317Update() {
        storageEditor.remove("messageViewArchiveActionVisible")
        storageEditor.remove("messageViewDeleteActionVisible")
        storageEditor.remove("messageViewMoveActionVisible")
        storageEditor.remove("messageViewCopyActionVisible")
        storageEditor.remove("messageViewSpamActionVisible")
    }

    private fun verifyV3111Update() {
        storage.getBoolean("pEpUsePassphraseForNewKeys", BuildConfig.USE_PASSPHRASE_FOR_NEW_KEYS)
        storageEditor.remove("pEpUsePassphraseForNewKeys")
        K9.setPlanckUsePassphraseForNewKeys(ManageableSetting(false))
    }


    @Test
    fun `AppUpdater removes files with name containing body in the cache if app was updated`() =
        runTest {
            every { storage.getLong("appVersionCode", -1) }.returns(-1)


            appUpdater.performOperationsOnUpdate()


            verify { cacheFile.listFiles() }
            verify { bodyFile.delete() }
        }

    @Test
    fun `AppUpdater does not remove files if app was not updated`() = runTest {
        every { storage.getLong("appVersionCode", -1) }.returns(CURRENT_APP_VERSION)


        appUpdater.performOperationsOnUpdate()


        verify { cacheFile.wasNot(called) }
        verify { bodyFile.wasNot(called) }
    }
}
