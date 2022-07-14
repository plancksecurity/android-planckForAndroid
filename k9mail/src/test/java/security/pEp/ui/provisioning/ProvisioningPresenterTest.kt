package security.pEp.ui.provisioning

import com.fsck.k9.pEp.ui.activities.provisioning.ProvisioningPresenter
import com.fsck.k9.pEp.ui.activities.provisioning.ProvisioningView
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import security.pEp.provisioning.InitializationFailedException
import security.pEp.provisioning.ProvisionState
import security.pEp.provisioning.ProvisioningFailedException
import security.pEp.provisioning.ProvisioningManager

class ProvisioningPresenterTest {
    private val provisioningManager: ProvisioningManager = mockk(relaxed = true)
    private val view: ProvisioningView = mockk(relaxed = true)
    private val presenter = ProvisioningPresenter(provisioningManager)

    @Test
    fun `attach() adds listener to provisioning manager`() {
        presenter.attach(view)

        verify { provisioningManager.addListener(presenter) }
    }

    @Test
    fun `detach() removes listener from provisioning manager`() {
        presenter.detach()

        verify { provisioningManager.removeListener(presenter) }
    }

    @Test
    fun `view handles WaitingForProvisioning state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(ProvisionState.WaitingForProvisioning)


        verify { view.waitingForProvisioning() }
    }

    @Test
    fun `view handles InProvisioning state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(ProvisionState.InProvisioning)


        verify { view.provisioningProgress() }
    }

    @Test
    fun `view handles Initializing state after successful provisioning`() {
        presenter.attach(view)


        presenter.provisionStateChanged(ProvisionState.Initializing(true))


        verify { view.initializingAfterSuccessfulProvision() }
    }

    @Test
    fun `view handles Initializing state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(ProvisionState.Initializing(false))


        verify { view.initializing() }
    }

    @Test
    fun `view handles Initialized state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(ProvisionState.Initialized)


        verify { view.initialized() }
    }

    @Test
    fun `view handles provisioning Error state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(
            ProvisionState.Error(ProvisioningFailedException("test error", RuntimeException()))
        )


        verify { view.displayProvisioningError("test error") }
    }

    @Test
    fun `view handles initialization Error state`() {
        presenter.attach(view)


        presenter.provisionStateChanged(
            ProvisionState.Error(InitializationFailedException("test error", RuntimeException()))
        )


        verify { view.displayInitializationError("test error") }
    }
}
