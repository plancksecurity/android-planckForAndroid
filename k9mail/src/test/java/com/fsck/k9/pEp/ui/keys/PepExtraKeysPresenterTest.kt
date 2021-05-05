package com.fsck.k9.pEp.ui.keys

import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.nhaarman.mockito_kotlin.any
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class PepExtraKeysPresenterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var presenter: PepExtraKeysPresenter

    @Mock
    private lateinit var view: PepExtraKeysView

    @Mock
    private lateinit var provider: PEpProvider
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        presenter = PepExtraKeysPresenter(coroutinesTestRule.testDispatcherProvider)
    }

    @Test
    @Throws(Exception::class)
    fun shouldGetMasterKeysInfoWhenSetupMasterKeys(): Unit =
            coroutinesTestRule.testDispatcher.run {
        presenter.initialize(view, provider, keys())
        Mockito.verify(provider).masterKeysInfo
    }

    @Test
    @Throws(Exception::class)
    fun shouldShowKeysInfoWhenSetupMasterKeys() =
            coroutinesTestRule.testDispatcher.run {
        presenter.initialize(view, provider, keys())
        Mockito.verify(view).showKeys(any())
    }

    private fun keys(): Set<String> {
        return emptySet()
    }
}