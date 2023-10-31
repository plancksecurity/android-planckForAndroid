package security.planck.ui.audit

import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.databinding.AuditLogItemBinding

class AuditLogDisplayViewHolder(
    private val binding: AuditLogItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(log: String) {
        binding.auditItem.text = log
    }
}