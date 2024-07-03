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
    private val viewModel = ProvisioningViewModel(provisioningManager)

    @Test
    fun `view handles WaitingForProvisioning state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(ProvisionState.WaitingForProvisioning)


        verify { view.waitingForProvisioning() }
    }

    @Test
    fun `view handles InProvisioning state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(ProvisionState.InProvisioning)


        verify { view.provisioningProgress() }
    }

    @Test
    fun `view handles Initializing state after successful provisioning`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(ProvisionState.Initializing(true))


        verify { view.initializingAfterSuccessfulProvision() }
    }

    @Test
    fun `view handles Initializing state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(ProvisionState.Initializing(false))


        verify { view.initializing() }
    }

    @Test
    fun `view handles Initialized state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(ProvisionState.Initialized)


        verify { view.initialized() }
    }

    @Test
    fun `view handles provisioning Error state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(
            ProvisionState.Error(ProvisioningFailedException("test error", RuntimeException()))
        )


        verify { view.displayProvisioningError("test error") }
    }

    @Test
    fun `view handles initialization Error state`() {
        viewModel.attach(view)


        viewModel.provisionStateChanged(
            ProvisionState.Error(InitializationFailedException("test error", RuntimeException()))
        )


        verify { view.displayInitializationError("test error") }
    }
}
