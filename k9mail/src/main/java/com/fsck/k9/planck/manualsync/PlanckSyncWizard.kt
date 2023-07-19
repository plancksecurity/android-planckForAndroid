package com.fsck.k9.planck.manualsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.fsck.k9.R
import com.fsck.k9.databinding.ActivityImportWizzardFromPgpBinding
import com.fsck.k9.planck.ui.tools.ThemeManager

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

    private fun setupViews() {

    }

    private fun observeViewModel() {
        viewModel.syncState.observe(this) {
            renderSyncState(it)
        }
    }

    private fun renderSyncState(syncState: SyncScreenState) {
        when (syncState) {
            SyncScreenState.Idle -> {}
            SyncScreenState.AwaitingHandshakeStart -> {
                binding.waitingForSync.isVisible = true
            }

            SyncScreenState.HandshakeReadyAwaitingUser -> {
                showAwaitingUserToStartHandshake()
            }

            is SyncScreenState.UserHandshaking -> {
                showHandshake(syncState)
            }

            is SyncScreenState.AwaitingHandshakeCompletion -> {
                showAwaitingHandshakeCompletion()
            }

            is SyncScreenState.Done -> {
                showKeySyncDone()
            }
        }
    }

    private fun showKeySyncDone() {
        binding.description.setText(getKeySyncDoneDescription())
        binding.currentState.setImageResource(getKeySyncDoneStateDrawable())
        binding.loading.isVisible = false
        binding.dissmissActionButton.isVisible = false
        binding.afirmativeActionButton.isVisible = true
        binding.afirmativeActionButton.setTextColor(
            ThemeManager.getColorFromAttributeResource(
                this,
                R.attr.defaultColorOnBackground
            )
        )
        binding.afirmativeActionButton.setOnClickListener { finish() }
        binding.currentState.isVisible = true
    }

    private fun showAwaitingHandshakeCompletion() {
        invalidateOptionsMenu()
        binding.description.setText(R.string.keysync_wizard_waiting_message)
        binding.trustwordsContainer.isVisible = false
        binding.loading.isVisible = true
        binding.afirmativeActionButton.isVisible = false
        binding.negativeActionButton.isVisible = false
        binding.dissmissActionButton.setOnClickListener { viewModel.cancelHandshake() }
    }

    private fun showAwaitingUserToStartHandshake() {
        binding.waitingForSync.isVisible = false
        binding.description.setText(getAwaitingUserDescription())
        binding.currentState.setImageResource(getAwaitingUserStateDrawable())
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.next()
        }
    }

    private fun showHandshake(syncState: SyncScreenState.UserHandshaking) {
        invalidateOptionsMenu()
        showLangIcon()
        binding.description.setText(R.string.keysync_wizard_handshake_message)
        binding.afirmativeActionButton.setText(R.string.key_import_accept)
        binding.afirmativeActionButton.setOnClickListener { viewModel.acceptHandshake() }
        binding.fprContainer.isVisible = true
        binding.trustwordsContainer.isVisible = true
        binding.trustwords.text = syncState.trustwords
        binding.currentState.isVisible = false
        binding.negativeActionButton.isVisible = true
        binding.negativeActionButton.setOnClickListener { viewModel.rejectHandshake() }
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

    companion object {
        fun startKeySync(context: Activity) {
            val intent = Intent(context, PlanckSyncWizard::class.java)
            context.startActivity(intent)
        }
    }
}