package com.fsck.k9.planck.manualsync

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
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
private const val ENGLISH_POSITION = 0
private const val GERMAN_POSITION = 1
private const val IGNORE_REJECT_BUTTON = true

@AndroidEntryPoint
class PlanckSyncWizard : WizardActivity() {
    private lateinit var binding: ActivityImportWizzardFromPgpBinding
    private val viewModel: PlanckSyncWizardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportWizzardFromPgpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeToolbar(false, "")
        setUpFloatingWindowWrapHeight()
        setupViews()
        onBackPressedDispatcher.addCallback { viewModel.cancelIfNotDone() }
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
            R.id.english -> viewModel.changeTrustwordsLanguage(ENGLISH_POSITION)
            R.id.german -> viewModel.changeTrustwordsLanguage(GERMAN_POSITION)
        }
        return true
    }

    private fun setupViews() {
        binding.dissmissActionButton.setOnClickListener {
            viewModel.cancelHandshake()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.rejectHandshake()
        }
        binding.showLongTrustwords.setOnClickListener {
            binding.showLongTrustwords.isVisible = false
            viewModel.switchTrustwordsLength()
        }

        binding.trustwords.setOnLongClickListener {
            viewModel.switchTrustwordsLength()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.getSyncState().observe(this) {
            renderSyncState(it)
        }
    }

    private fun renderSyncState(syncState: SyncScreenState) {
        val stateDebugText = "State: ${syncState::class.java.simpleName}"
        binding.syncStateFeedback.text = stateDebugText
        when (syncState) {
            SyncState.Idle,
            is SyncState.AwaitingOtherDevice -> {
                showAwaitingOtherDevice()
            }

            is SyncState.HandshakeReadyAwaitingUser -> {
                showAwaitingUserToStartHandshake()
            }

            is SyncState.UserHandshaking -> {
                showHandshake(syncState)
            }

            is SyncState.AwaitingHandshakeCompletion -> {
                showAwaitingHandshakeCompletion(syncState)
            }

            is SyncState.Done -> {
                showKeySyncDone()
            }

            SyncState.Cancelled -> {
                finish()
            }

            SyncState.TimeoutError -> {
                showSomethingWentWrong()
            }

            is SyncState.Error -> {
                binding.syncStateFeedback.text = syncState.throwable.message
                binding.syncStateFeedback.setTextColor(
                    ContextCompat.getColor(this, R.color.planck_red)
                )
                showSomethingWentWrong()
            }

            is SyncState.SyncStartTimeout -> {
                showSyncStartTimeout()
            }
        }
    }

    private fun showAwaitingOtherDevice() {
        showScreen(
            waitingForSyncVisible = true,
            dismissButtonVisible = true,
        )
        binding.waitingForSyncText.setText(R.string.sync_dialog_awaiting_other_device)
    }

    private fun showSomethingWentWrong() {
        showScreen(
            description = R.string.keysync_wizard_error_message,
            currentState = getAwaitingUserStateDrawable(),
            positiveButtonText = R.string.key_import_accept,
            positiveButtonClose = true,
        ) {
            viewModel.cancelHandshake()
        }
    }

    private fun showSyncStartTimeout() {
        showScreen(
            description = R.string.sync_dialog_sync_start_timeout,
            currentState = getAwaitingUserStateDrawable(),
            positiveButtonText = R.string.key_import_accept,
            positiveButtonClose = true,
        ) {
            finish()
        }
    }

    private fun showKeySyncDone() {
        showScreen(
            description = getKeySyncDoneDescription(),
            currentState = getKeySyncDoneStateDrawable(),
            positiveButtonText = R.string.key_import_accept,
            positiveButtonClose = true,
        ) {
            finish()
        }
    }

    private fun showAwaitingHandshakeCompletion(syncState: SyncState.AwaitingHandshakeCompletion) {
        invalidateOptionsMenu()
        showScreen(
            description = R.string.keysync_wizard_waiting_message,
            ownFpr = syncState.ownFpr,
            partnerFpr = syncState.partnerFpr,
            loadingAnimation = getLoadingAnimationDrawable(),
            dismissButtonVisible = true,
        )
    }

    private fun showAwaitingUserToStartHandshake() {
        showScreen(
            description = getAwaitingUserDescription(),
            currentState = getAwaitingUserStateDrawable(),
            positiveButtonText = R.string.keysync_wizard_action_next,
            dismissButtonVisible = true,
        ) {
            viewModel.next()
        }
    }

    private fun showHandshake(syncState: SyncState.UserHandshaking) {
        invalidateOptionsMenu()
        showLangIcon()
        binding.showLongTrustwords.isVisible = syncState.trustwords.isNotBlank() && viewModel.shortTrustWords
        showScreen(
            description = R.string.keysync_wizard_handshake_message,
            ownFpr = syncState.ownFpr,
            partnerFpr = syncState.partnerFpr,
            trustwords = syncState.trustwords,
            negativeButtonVisible = true,
            dismissButtonVisible = true,
            positiveButtonText = R.string.key_import_accept,
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
        positiveButtonClose: Boolean = false,
        positiveButtonClick: () -> Unit = {},
    ) {
        binding.description.apply {
            isVisible = (description != NO_RESOURCE).also { if (it) setText(description) }
        }
        if (ownFpr.isNotBlank()) {
            showFprs(ownFpr, partnerFpr)
        } else {
            binding.fprGroup.isVisible = false
        }
        if (trustwords.isNotBlank()) {
            showTrustwords(trustwords)
        } else {
            binding.trustwordsGroup.isVisible = false
        }
        binding.currentState.apply {
            isVisible =
                (currentState != NO_RESOURCE).also { if (it) setImageResource(currentState) }
        }
        binding.loading.apply {
            isVisible = (loadingAnimation != NO_RESOURCE).also {
                if (it) indeterminateDrawable = ContextCompat.getDrawable(
                    this@PlanckSyncWizard, loadingAnimation
                )
            }
        }
        binding.waitingForSync.isVisible = waitingForSyncVisible
        binding.waitingForSyncText.isVisible = waitingForSyncVisible
        binding.syncStateFeedback.isVisible = syncStateFeedbackVisible
        binding.negativeActionButton.isVisible =
            if (IGNORE_REJECT_BUTTON) false else negativeButtonVisible
        binding.dissmissActionButton.visibility =
            if (dismissButtonVisible) View.VISIBLE else View.INVISIBLE
        binding.afirmativeActionButton.apply {
            isVisible =
                (positiveButtonText != NO_RESOURCE).also { if (it) setText(positiveButtonText) }
            if (positiveButtonClose) {
                setTextColor(
                    ThemeManager.getColorFromAttributeResource(
                        this@PlanckSyncWizard, R.attr.defaultColorOnBackground
                    )
                )
            }
            setOnClickListener { positiveButtonClick() }
        }
    }

    private fun showTrustwords(trustwords: String) {
        binding.trustwordsGroup.isVisible = true
        binding.trustwords.text = trustwords
    }

    private fun showFprs(ownFpr: String, partnerFpr: String) {
        binding.fprGroup.isVisible = true
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
}