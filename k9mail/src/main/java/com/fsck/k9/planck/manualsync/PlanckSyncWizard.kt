package com.fsck.k9.planck.manualsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.databinding.ActivityImportWizzardFromPgpBinding
import com.fsck.k9.planck.ui.tools.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

private const val NO_RESOURCE = 0

@AndroidEntryPoint
class PlanckSyncWizard : WizardActivity() {
    private lateinit var binding: ActivityImportWizzardFromPgpBinding
    private val viewModel: PlanckSyncWizardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportWizzardFromPgpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(false)
        setUpFloatingWindowWrapHeight()
        setupViews()
        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (viewModel.isHandshaking()) {
            menuInflater.inflate(R.menu.menu_add_device, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.english -> viewModel.changeTrustwordsLanguage(0)
            R.id.german -> viewModel.changeTrustwordsLanguage(1)
        }
        return true
    }

    private fun setupViews() {
        binding.dissmissActionButton.setOnClickListener {
            viewModel.cancelHandshake()
            finish()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.rejectHandshake()
            finish()
        }
        binding.showLongTrustwords.setOnClickListener {
            binding.showLongTrustwords.isVisible = false
            viewModel.switchTrustwordsLength()
        }

        binding.trustwords.setOnLongClickListener {
            viewModel.switchTrustwordsLength()
            true
        }
        binding.afirmativeActionButton.setTextColor(
            ThemeManager.getColorFromAttributeResource(
                this, R.attr.defaultColorOnBackground
            )
        )
    }

    private fun observeViewModel() {
        viewModel.getSyncState().observe(this) {
            renderSyncState(it)
        }
    }

    private fun renderSyncState(syncState: SyncScreenState) {
        binding.syncStateFeedback.text = "State: ${syncState::class.java.simpleName}"
        binding.showLongTrustwords.isVisible = viewModel.shortTrustWords
        when (syncState) {
            SyncScreenState.Idle,
            SyncScreenState.AwaitingOtherDevice -> {
                showAwaitingOtherDevice()
            }

            SyncScreenState.HandshakeReadyAwaitingUser -> {
                showAwaitingUserToStartHandshake()
            }

            is SyncScreenState.UserHandshaking -> {
                showHandshake(syncState)
            }

            is SyncScreenState.AwaitingHandshakeCompletion -> {
                showAwaitingHandshakeCompletion(syncState)
            }

            is SyncScreenState.Done -> {
                showKeySyncDone()
            }

            SyncScreenState.Cancelled -> {
                finish()
            }

            SyncScreenState.TimeoutError -> {
                showSomethingWentWrong()
            }
        }
    }

    private fun showAwaitingOtherDevice() {
        showScreen(
            waitingForSyncVisible = true,
            dismissButtonVisible = true,
            syncStateFeedbackVisible = true
        )
        binding.syncStateFeedback.setText(R.string.sync_dialog_awaiting_other_device)
    }

    private fun showSomethingWentWrong() {
        showScreen(
            description = R.string.keysync_wizard_error_message,
            currentState = getAwaitingUserStateDrawable(),
            positiveButtonText = R.string.key_import_accept
        ) {
            viewModel.cancelHandshake()
            finish()
        }
    }

    private fun showKeySyncDone() {
        showScreen(
            description = getKeySyncDoneDescription(),
            currentState = getKeySyncDoneStateDrawable(),
            positiveButtonText = R.string.key_import_accept
        ) {
            finish()
        }
    }

    private fun showAwaitingHandshakeCompletion(syncState: SyncScreenState.AwaitingHandshakeCompletion) {
        invalidateOptionsMenu()
        showScreen(
            description = R.string.keysync_wizard_waiting_message,
            ownFpr = syncState.ownFpr,
            partnerFpr = syncState.partnerFpr,
            loadingAnimation = getLoadingAnimationDrawable(),
            dismissButtonVisible = true
        )
    }

    private fun showAwaitingUserToStartHandshake() {
        showScreen(
            description = getAwaitingUserDescription(),
            currentState = getAwaitingUserStateDrawable(),
            positiveButtonText = R.string.keysync_wizard_action_next
        ) {
            viewModel.next()
        }
    }

    private fun showHandshake(syncState: SyncScreenState.UserHandshaking) {
        invalidateOptionsMenu()
        showLangIcon()
        showScreen(
            description = R.string.keysync_wizard_handshake_message,
            ownFpr = syncState.ownFpr,
            partnerFpr = syncState.partnerFpr,
            trustwords = syncState.trustwords,
            negativeButtonVisible = true,
            dismissButtonVisible = true,
            positiveButtonText = R.string.key_import_accept
        ) {
            viewModel.acceptHandshake()
        }
    }

    private fun showScreen(
        @StringRes description: Int = NO_RESOURCE,
        ownFpr: String = "",
        partnerFpr: String = "",
        trustwords: String = "",
        @DrawableRes currentState: Int = NO_RESOURCE,
        @DrawableRes loadingAnimation: Int = NO_RESOURCE,
        waitingForSyncVisible: Boolean = false,
        syncStateFeedbackVisible: Boolean = BuildConfig.DEBUG,
        negativeButtonVisible: Boolean = false,
        dismissButtonVisible: Boolean = false,
        @StringRes positiveButtonText: Int = NO_RESOURCE,
        positiveButtonClick: () -> Unit = {},
    ) {
        if (description == NO_RESOURCE) {
            binding.description.isVisible = false
        } else {
            binding.description.isVisible = true
            binding.description.setText(description)
        }
        if (ownFpr.isNotBlank()) {
            showFprs(ownFpr, partnerFpr)
        } else {
            binding.fprContainer.isVisible = false
        }
        if (trustwords.isNotBlank()) {
            showTrustwords(trustwords)
        } else {
            binding.trustwordsContainer.isVisible = false
        }
        if (currentState == NO_RESOURCE) {
            binding.currentState.isVisible = false
        } else {
            binding.currentState.isVisible = true
            binding.currentState.setImageResource(currentState)
        }
        if (loadingAnimation == NO_RESOURCE) {
            binding.loading.isVisible = false
        } else {
            binding.loading.isVisible = true
            binding.loading.indeterminateDrawable = ContextCompat.getDrawable(
                this, getLoadingAnimationDrawable()
            )
        }
        binding.waitingForSync.isVisible = waitingForSyncVisible
        binding.syncStateFeedback.isVisible = syncStateFeedbackVisible
        binding.negativeActionButton.isVisible = negativeButtonVisible
        binding.dissmissActionButton.visibility =
            if (dismissButtonVisible) View.VISIBLE else View.INVISIBLE
        if (positiveButtonText == NO_RESOURCE) {
            binding.afirmativeActionButton.isVisible = false
        } else {
            binding.afirmativeActionButton.isVisible = true
            binding.afirmativeActionButton.setText(positiveButtonText)
        }
        binding.afirmativeActionButton.setOnClickListener { positiveButtonClick() }
    }

    private fun showTrustwords(trustwords: String) {
        binding.trustwordsContainer.isVisible = true
        binding.trustwords.text = trustwords
    }

    private fun showFprs(ownFpr: String, partnerFpr: String) {
        binding.fprContainer.isVisible = true
        binding.fprCurrentDeviceValue.text = ownFpr
        binding.fprNewDeviceValue.text = partnerFpr
    }

    private fun showLangIcon() {
        val resource: Int = ThemeManager.getAttributeResource(this, R.attr.iconLanguageGray)
        toolbar?.overflowIcon = ContextCompat.getDrawable(this, resource)
    }

    @StringRes
    private fun getAwaitingUserDescription(): Int {
        return if (viewModel.formingGroup) R.string.keysync_wizard_create_group_first_message
        else R.string.keysync_wizard_add_device_to_existing_group_message
    }

    @DrawableRes
    private fun getAwaitingUserStateDrawable(): Int {
        return if (viewModel.formingGroup) R.drawable.ic_sync_2nd_device
        else R.drawable.ic_sync_3rd_device
    }

    @StringRes
    private fun getKeySyncDoneDescription(): Int {
        return if (viewModel.formingGroup) R.string.keysync_wizard_group_creation_done_message
        else R.string.keysync_wizard_group_joining_done_message
    }

    @DrawableRes
    private fun getKeySyncDoneStateDrawable(): Int {
        return if (viewModel.formingGroup) R.drawable.ic_sync_2nd_device_synced
        else R.drawable.ic_sync_3rd_device_synced
    }

    @DrawableRes
    private fun getLoadingAnimationDrawable(): Int {
        return if (viewModel.formingGroup) R.drawable.add_second_device
        else R.drawable.add_device_to_group
    }

    companion object {
        fun startKeySync(context: Activity) {
            val intent = Intent(context, PlanckSyncWizard::class.java)
            context.startActivity(intent)
        }
    }
}