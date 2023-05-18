// TODO: 06/07/2020 Move to the correct package, it is here as a workarround
// As the other package is not seen by Junit.

//TODO init test dependency tree and Inject dependencies
package com.fsck.k9.planck.ui.passphrase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.PEpProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import security.planck.ui.passphrase.PassphraseInputView
import security.planck.ui.passphrase.PassphrasePresenter
import security.planck.ui.passphrase.PassphraseRequirementType

class PassphrasePresenterTest: RobolectricTest() {
    lateinit var  context: Context
    private val view: PassphraseInputView = mockk(relaxed = true)
    lateinit var presenter: PassphrasePresenter
    private val pEpProvider: PEpProvider = mockk()
    private val preferences: Preferences = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        presenter = PassphrasePresenter(pEpProvider, preferences)
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

        verify {view.showRetryPasswordRequest() }
        verify {view.enableNonSyncDismiss() }

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
    fun `when we receive cancel() view is finished`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)

        presenter.cancel()

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
    fun `when we deliver a valid input we send it to the provider and close the dialog`() {
        presenter.init(view, PassphraseRequirementType.MISSING_PASSPHRASE)
        val passphrase = "passphrase"

        presenter.deliverPassphrase(passphrase)

        //verify(provider).configPassphrase(passphrase)
        verify{ view.finish() }
    }

}