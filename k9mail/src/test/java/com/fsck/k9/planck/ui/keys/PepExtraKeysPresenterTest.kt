package com.fsck.k9.planck.ui.keys

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PEpProvider
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.planck.ui.blacklist.KeyListItem
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PepExtraKeysPresenterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val view: PepExtraKeysView = mockk(relaxed = true)
    private val provider: PEpProvider = mockk()
    private val preferences: Preferences = mockk()
    private val presenter = PepExtraKeysPresenter(
        provider,
        preferences,
        coroutinesTestRule.testDispatcherProvider
    )

    @Before
    fun setUp() {
        every { provider.masterKeysInfo }.returns(
            listOf(
                KeyListItem("fpr1", "guid1"),
                KeyListItem("fpr2", "guid2")
            )
        )

        mockkStatic(K9::class)
        every { K9.getMasterKeys() }.returns(emptySet())
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
    }

    @Test
    fun `presenter gets master key info on initialization`() = runTest {
        presenter.initialize(view)
        advanceUntilIdle()


        verify { K9.getMasterKeys() }
        coVerify { provider.masterKeysInfo }
    }

    @Test
    fun `view shows keys on presenter initialization`() = runTest {
        presenter.initialize(view)
        advanceUntilIdle()


        val slot = slot<List<KeyListItem>>()
        verify {
            view.showKeys(capture(slot))
        }

        val keys = slot.captured
        assertEquals(2, keys.size)
        assertEquals("fpr1", keys.first().fpr)
        assertEquals("fpr2", keys[1].fpr)
        assertEquals("guid1", keys.first().gpgUid)
        assertEquals("guid2", keys[1].gpgUid)
    }

    @Test
    fun `onPause saves K9 app settings to disk`() = runTest {
        val storage: Storage = mockk()
        val storageEditor: StorageEditor = mockk(relaxed = true)
        coEvery { preferences.storage }.returns(storage)
        coEvery { storage.edit() }.returns(storageEditor)


        presenter.initialize(view)
        presenter.onPause()
        advanceUntilIdle()

        coVerify { K9.save(storageEditor) }
        coVerify { storageEditor.commit() }
    }
}
