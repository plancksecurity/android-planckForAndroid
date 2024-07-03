package security.planck.ui.provisioning

import com.fsck.k9.planck.ui.activities.provisioning.ProvisioningViewModel
import com.fsck.k9.planck.ui.activities.provisioning.ProvisioningView
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import security.planck.provisioning.InitializationFailedException
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningManager

class ProvisioningViewModelTest {
    private val provisioningManager: ProvisioningManager = mockk(relaxed = true)
    private val view: ProvisioningView = mockk(relaxed = true)
    private val presenter = ProvisioningViewModel(provisioningManager)

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
