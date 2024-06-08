package security.planck.passphrase

import android.util.Log
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

private const val EMAIL = "test@mail.ch"

class PassphraseRepositoryTest {
    private val planckProvider: PlanckProvider = mockk {
        coEvery { hasPassphrase(any()) }.returns(Result.success(true))
    }
    private val planckProviderProvider: Provider<PlanckProvider> = mockk {
        every { get() }.returns(planckProvider)
    }
    private val account: Account = mockk {
        every { email }.returns(EMAIL)
    }
    private val preferences: Preferences = mockk {
        every { availableAccounts }.answers { listOf(account) }
    }
    private val k9: K9 = mockk {
        every { startAllServices() }.just(runs)
    }
    private val repository = PassphraseRepository(planckProviderProvider, preferences, k9)

    @Before
    fun setUp() {
        repository.resetPassphraseLock()
        mockkStatic(Log::class)
        every { Log.e(any(), any()) }.returns(0)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `initially Repository passphraseUnlocked is false`() {
        assertFalse(PassphraseRepository.passphraseUnlocked)
    }

    @Test
    fun `getAccountsWithPassphrase() uses PlanckProvider and Preferences`() = runBlocking {
        val result = repository.getAccountsWithPassPhrase()


        verify { preferences.availableAccounts }
        coVerify { planckProvider.hasPassphrase(EMAIL) }
        assertEquals(listOf(account), result.getOrThrow())
    }

    @Test
    fun `getAccountsWithPassphrase() returns success with empty list if no account has passphrase`() =
        runBlocking {
            coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))


            val result = repository.getAccountsWithPassPhrase()


            assertEquals(emptyList<Account>(), result.getOrThrow())
        }

    @Test
    fun `getAccountsWithPassphrase() returns failure if PlanckProvider call fails`() = runBlocking {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.failure(TestException()))


        val result = repository.getAccountsWithPassPhrase()


        assertEquals(TestException(), result.exceptionOrNull())
    }

    @Test
    fun `unlockPassphrase() sets passphraseUnlocked to true and starts all services`() =
        runBlocking {
            repository.unlockPassphrase()


            verify { k9.startAllServices() }
            assertTrue(PassphraseRepository.passphraseUnlocked)
        }

    @Test
    fun `initializeBlocking() gets accounts with passphrases and starts all services if none`() {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.success(false))


        repository.initializeBlocking()


        verify { preferences.availableAccounts }
        coVerify { planckProvider.hasPassphrase(EMAIL) }
        verify { k9.startAllServices() }
        assertTrue(PassphraseRepository.passphraseUnlocked)
    }

    @Test
    fun `initializeBlocking() does not start services if there is any account with passphrase`() {
        repository.initializeBlocking()


        verify { preferences.availableAccounts }
        coVerify { planckProvider.hasPassphrase(EMAIL) }
        verify(exactly = 0) { k9.startAllServices() }
        assertFalse(PassphraseRepository.passphraseUnlocked)
    }

    @Test
    fun `initializeBlocking() does not start services if PlanckProvider call fails`() {
        coEvery { planckProvider.hasPassphrase(any()) }.returns(Result.failure(TestException()))


        repository.initializeBlocking()


        verify { preferences.availableAccounts }
        coVerify { planckProvider.hasPassphrase(EMAIL) }
        verify(exactly = 0) { k9.startAllServices() }
        assertFalse(PassphraseRepository.passphraseUnlocked)
    }

    private data class TestException(override val message: String = "test") : Throwable()
}