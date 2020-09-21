package com.fsck.k9.pEp.ui.keys

import com.fsck.k9.pEp.PEpProvider
import com.nhaarman.mockito_kotlin.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class PepExtraKeysPresenterTest {
    private lateinit var presenter: PepExtraKeysPresenter

    @Mock
    private lateinit var view: PepExtraKeysView

    @Mock
    private lateinit var provider: PEpProvider
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        presenter = PepExtraKeysPresenter()
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @Throws(Exception::class)
    fun shouldGetMasterKeysInfoWhenSetupMasterKeys() = runBlockingTest {
        presenter.initialize(view, provider, keys())
        Mockito.verify(provider).masterKeysInfo
    }

    @Test
    @Throws(Exception::class)
    fun shouldShowKeysInfoWhenSetupMasterKeys() = runBlockingTest {
        presenter.initialize(view, provider, keys())
        Mockito.verify(view).showKeys(any())
    }

    private fun keys(): Set<String> {
        return emptySet()
    }
}