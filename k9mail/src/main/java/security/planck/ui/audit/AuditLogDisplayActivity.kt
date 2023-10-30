package security.planck.ui.audit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Layout
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.databinding.ActivityAuditLogDisplayBinding
import com.fsck.k9.planck.infrastructure.ListState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

private const val LONGEST_ITEM_EXTRA = 100

@AndroidEntryPoint
class AuditLogDisplayActivity : K9Activity() {
    private val viewModel: AuditLogDisplayViewModel by viewModels()
    private lateinit var binding: ActivityAuditLogDisplayBinding

    @Inject
    lateinit var adapter: AuditLogDisplayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuditLogDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setUpToolbar(true)
        binding.auditLogRecyclerView.adapter = adapter
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.auditText.observe(this) { listState ->
            when (listState) {
                ListState.Loading -> {
                    startLoading()
                }

                is ListState.Ready -> {
                    setRecyclerViewWidth()
                    adapter.submitList(listState.list)
                    binding.auditLogRecyclerView.isVisible = true
                    //binding.auditLogRecyclerView.invalidate()
                    stopLoading()
                }

                is ListState.Error -> {
                    showFeedback(getErrorMessage(listState.throwable))
                    adapter.submitList(listOf(getErrorMessage(listState.throwable)))
                    stopLoading()
                }

                ListState.EmptyList -> {
                    showFeedback(getString(R.string.audit_log_display_no_logs))
                    stopLoading()
                }
            }
        }
    }

    private fun setRecyclerViewWidth() {
        binding.auditLogRecyclerView.maxWidth = Layout.getDesiredWidth(
            viewModel.longestItem, binding.feedbackText.paint
        ).roundToInt() + LONGEST_ITEM_EXTRA
    }

    private fun showFeedback(message: String) {
        binding.feedbackText.text = message
        binding.feedbackText.isVisible = true
    }

    private fun getErrorMessage(throwable: Throwable): String {
        return if (BuildConfig.DEBUG) throwable.stackTraceToString()
        else getString(R.string.audit_log_display_error, throwable.message)
    }

    private fun stopLoading() {
        binding.loading.hide()
    }

    private fun startLoading() {
        binding.loading.show()
        binding.auditLogRecyclerView.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun displayAuditLog(context: Activity) {
            context.startActivity(Intent(context, AuditLogDisplayActivity::class.java))
        }
    }
}