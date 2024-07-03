// TODO: 06/07/2020 Move to the correct package, it is here as a workarround
// As the other package is not seen by Junit.

//TODO init test dependency tree and Inject dependencies
package com.fsck.k9.planck.ui.passphrase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.K9
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.ui.PassphraseProvider
import security.planck.ui.passphrase.old.PassphraseInputView
import security.planck.ui.passphrase.old.PassphrasePresenter
import security.planck.ui.passphrase.old.PassphraseRequirementType

@ExperimentalCoroutinesApi
class PassphrasePresenterTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    lateinit var context: Context
    private val view: PassphraseInputView = mockk(relaxed = true)
    private lateinit var presenter: PassphrasePresenter
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val controller: MessagingController = mockk {
        every { tryToDecryptMessagesThatCouldNotDecryptBefore() }.just(runs)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        mockkStatic(PassphraseProvider::class)
        mockkObject(PassphraseProvider)
        mockkStatic(K9::class)
        presenter = PassphrasePresenter(
            planckProvider,
            controller,
            coroutinesTestRule.testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        unmockkObject(PassphraseProvider)
        unmockkStatic(PassphraseProvider::class)
        unmockkStatic(K9::class)
    }

    @Test
    fun `when initializing the present view is also initialized`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        verify { view.init() }
        verify { view.initAffirmativeListeners() }
    }

    @Test
    fun `when initializing due to missing password view shows password request`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        verify { view.showPasswordRequest() }
        verify { view.enableNonSyncDismiss() }
    }

    @Test
    fun `when initializing due to *wrong* password view shows retry password request`() {
        presenter.init(view, PassphraseRequirementType.WRONG_PASSPHRASE)


        verify { view.showRetryPasswordRequest() }
        verify { view.enableNonSyncDismiss() }
    }

    @Test
    fun `when initializing due to sync password view shows retry password request`() {
        presenter.init(view, PassphraseRequirementType.SYNC_PASSPHRASE)


        verify { view.showSyncPasswordRequest() }
        verify { view.enableSyncDismiss() }
    }

    @Test
    fun `when initializing the present view listeners are initialized`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        verify { view.initAffirmativeListeners() }
    }

    @Test
    fun `when we receive cancel() view is finished and the PassphraseProvider stopped`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        presenter.cancel()


        verify { view.finish() }
        verify { PassphraseProvider.stop() }
    }

    @Test
    fun `cancelSync() stops PassphraseProvider and finishes the view`() = runTest {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        presenter.cancelSync()
        advanceUntilIdle()


        coVerify { PassphraseProvider.stop() }
        verify { view.finish() }
    }

    @Test
    fun `when we deliver an empty input text we ask the view to disable the affirmative action`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)


        presenter.validateInput("")


        verify { view.enableActionConfirmation(false) }
    }

    @Test
    fun `when we deliver an empty input text we ask the view to enable the affirmative action`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)

        presenter.validateInput("passphrase")

        verify { view.enableActionConfirmation(true) }
    }

    @Test
    fun `when we deliver a valid input we deliver it to the PassphraseProvider and close the dialog`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)
        val passphrase = "passphrase"


        presenter.deliverPassphrase(passphrase)


        verify { PassphraseProvider.passphrase = passphrase }
        verify { PassphraseProvider.stop() }
        verify { view.finish() }
    }

    @Test
    fun `when we deliver a valid sync passphrase we deliver it to the PlanckProvider and close the dialog`() =
        runTest {
            presenter.init(view, PassphraseRequirementType.SYNC_PASSPHRASE)
            val passphrase = "passphrase"


            presenter.deliverPassphrase(passphrase)
            advanceUntilIdle()


            verify { planckProvider.configPassphrase(passphrase) }
            verify { PassphraseProvider.stop() }
            verify { view.finish() }
        }

    @Test
    fun `when we deliver a valid sync passphrase, MessagingController tries to decrypt messages that could not decrypt before`() =
        runTest {
            presenter.init(view, PassphraseRequirementType.SYNC_PASSPHRASE)
            val passphrase = "passphrase"


            presenter.deliverPassphrase(passphrase)
            advanceUntilIdle()


            verify { controller.tryToDecryptMessagesThatCouldNotDecryptBefore() }
        }

    @Test
    fun `when we deliver a valid passphrase for new keys we set it to the PassphraseProvider and K9 and close the dialog`() {
        presenter.init(view, PassphraseRequirementType.NEW_KEYS_PASSPHRASE)
        val passphrase = "passphrase"


        presenter.deliverPassphrase(passphrase)


        verify { PassphraseProvider.passphrase = passphrase }
        verify { PassphraseProvider.stop() }
        verify { view.finish(true) }
    }

}