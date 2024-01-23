package security.planck.ui.resetpartnerkey

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Identity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.dialog.BackgroundTaskDialogView
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ResetPartnerKeyViewModel @Inject constructor(
    private val context: Application,
    private val planckProvider: PlanckProvider,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val resetPartnerKeyStateLd: MutableLiveData<BackgroundTaskDialogView.State> =
        MutableLiveData(BackgroundTaskDialogView.State.CONFIRMATION)
    val resetPartnerKeyState: LiveData<BackgroundTaskDialogView.State> = resetPartnerKeyStateLd

    private lateinit var partner: Identity

    fun initialize(
        partner: String?,
    ) {
        viewModelScope.launch {
            populateData(partner).onFailure {
                Timber.e(it)
                resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }

    fun resetPlanckData() {
        viewModelScope.launch {
            resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.LOADING
            kotlin.runCatching {
                withContext(dispatcherProvider.planckDispatcher()) {
                    planckProvider.keyResetIdentity(partner, null)
                }
            }.onSuccess {
                resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.SUCCESS
            }.onFailure {
                Timber.e(it)
                resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }

    fun partnerKeyResetFinished() {
        resetPartnerKeyStateLd.value = BackgroundTaskDialogView.State.CONFIRMATION
    }

    private fun populateData(
        partner: String?,
    ): Result<Unit> = kotlin.runCatching {
        partner ?: error("partner missing")
        this@ResetPartnerKeyViewModel.partner =
            PlanckUtils.createIdentity(Address.create(partner), context)
    }
}