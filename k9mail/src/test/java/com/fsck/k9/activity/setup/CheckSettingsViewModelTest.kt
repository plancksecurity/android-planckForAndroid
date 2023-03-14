package com.fsck.k9.activity.setup

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.activity.setup.CheckSettingsState.*
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.pEp.serversettings.ServerSettingsChecker

@ExperimentalCoroutinesApi
class CheckSettingsViewModelTest : RobolectricTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val serverSettingsChecker: ServerSettingsChecker = mockk()
    private val context: Context = mockk()
    private val account: Account = mockk()

    private val viewModel: CheckSettingsViewModel = CheckSettingsViewModel(
        serverSettingsChecker,
        coroutinesTestRule.testDispatcherProvider
    )
    private val receivedUiStates = mutableListOf<CheckSettingsState>()

    @Before
    fun setUp() {
        receivedUiStates.clear()
        observeViewModel(viewModel)
        assertEquals(listOf(Idle), receivedUiStates)
    }

    @Test
    fun `start() checks settings using ServerSettingsChecker`() = runTest {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .returns(Result.success(Unit))


        viewModel.start(context, account, CheckDirection.INCOMING, true)
        advanceUntilIdle()


        coVerify {
            serverSettingsChecker.checkServerSettings(
                context,
                account,
                CheckDirection.INCOMING,
                true
            )
        }
    }

    @Test
    fun `start() sets state to CheckingIncoming if checking incoming settings`() {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .returns(Result.success(Unit))


        viewModel.start(
            context, account,
            CheckDirection.INCOMING, true
        )


        assertTrue(CheckingIncoming in receivedUiStates)
    }

    @Test
    fun `start() sets state to CheckingOutgoing if checking outgoing settings`() {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .returns(Result.success(Unit))


        viewModel.start(
            context, account,
            CheckDirection.OUTGOING, true
        )


        assertTrue(CheckingOutgoing in receivedUiStates)
    }

    @Test
    fun `start() sets state to Success if settings check is successful`() = runTest {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .returns(Result.success(Unit))


        viewModel.start(
            context, account,
            CheckDirection.INCOMING, true
        )
        advanceUntilIdle()


        assertEquals(
            listOf(
                Idle,
                CheckingIncoming,
                Success
            ),
            receivedUiStates
        )
    }

    @Test
    fun `start() sets state to Error if there was an error checking settings`() = runTest {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .returns(Result.failure(TestException("test")))


        viewModel.start(
            context, account,
            CheckDirection.INCOMING, true
        )
        advanceUntilIdle()


        assertEquals(
            listOf(
                Idle,
                CheckingIncoming,
                Error(TestException("test"))
            ),
            receivedUiStates
        )
    }

    @Test
    fun `cancel() cancels start() operation`() = runTest {
        coEvery { serverSettingsChecker.checkServerSettings(context, account, any(), any()) }
            .coAnswers {
                runBlocking {
                    delay(200)
                    Result.success(Unit)
                }
            }


        viewModel.start(
            context, account,
            CheckDirection.INCOMING, true
        )
        viewModel.cancel()
        advanceUntilIdle()


        assertNotFinished()
    }

    private fun assertNotFinished() {
        assertFalse(receivedUiStates.any { it == Success })
        assertFalse(receivedUiStates.any { it is Error })
    }

    private fun observeViewModel(viewModel: CheckSettingsViewModel) {
        viewModel.state.observeForever { value ->
            receivedUiStates.add(value)
        }
    }

    private data class TestException(override val message: String) : Exception(message)
}

