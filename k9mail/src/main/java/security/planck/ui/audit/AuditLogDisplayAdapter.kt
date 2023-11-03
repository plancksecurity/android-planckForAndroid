package security.planck.ui.audit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fsck.k9.databinding.AuditLogItemBinding
import javax.inject.Inject

class AuditLogDisplayAdapter @Inject constructor() :
    ListAdapter<String, AuditLogDisplayViewHolder>(
        AuditLogDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditLogDisplayViewHolder {
        val binding = AuditLogItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuditLogDisplayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuditLogDisplayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AuditLogDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}