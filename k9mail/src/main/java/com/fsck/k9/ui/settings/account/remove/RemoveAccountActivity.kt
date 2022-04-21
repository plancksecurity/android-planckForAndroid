package com.fsck.k9.ui.settings.account.remove

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.databinding.ActivityRemoveAccountBinding
import com.fsck.k9.pEp.manualsync.WizardActivity
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class RemoveAccountActivity : WizardActivity() {
    @Inject
    lateinit var presenter: RemoveAccountPresenter
    @Inject
    lateinit var view: RemoveAccountView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityRemoveAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpFloatingWindow()
        view.initialize(
            binding,
            onAcceptButtonClicked = { presenter.onAcceptButtonClicked() },
            onCancelButtonClicked = { presenter.onCancelButtonClicked() }
        )

        intent.extras?.let {
            val model = getViewModel()
            val accountUuid = it.getString(EXTRA_ACCOUNT_UUID, "")
            val scopeProvider = object: CoroutineScopeProvider {
                override fun getScope(): CoroutineScope = model.viewModelScope

            }
            presenter.initialize(
                view,
                model,
                scopeProvider,
                accountRemoveViewDelegate,
                accountUuid,
                savedInstanceState == null
            )
        } ?: finish()
    }

    private fun getViewModel() =
        ViewModelProvider(this).get(RemoveAccountViewModel::class.java)

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private val accountRemoveViewDelegate = object: RemoveAccountViewDelegate {
        override fun accountRemoved() {
            val intent = Intent()
            intent.putExtra(EXTRA_ACCOUNT_DELETED, true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        override fun finish() {
            this@RemoveAccountActivity.finish()
        }
    }

    override fun setUpFloatingWindow() {
        super.setUpFloatingWindow()
        val layoutParams = window.attributes
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.attributes = layoutParams
    }

    companion object {
        const val EXTRA_ACCOUNT_DELETED = "extra_account_deleted"
        const val ACTIVITY_REQUEST_REMOVE_ACCOUNT = 10013

        private const val EXTRA_ACCOUNT_UUID = "account_uuid"

        fun start(activity: FragmentActivity, account: Account) {
            val intent = Intent(activity, RemoveAccountActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
            activity.startActivityForResult(
                intent, ACTIVITY_REQUEST_REMOVE_ACCOUNT
            )
        }
    }
}