package security.planck.passphrase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fsck.k9.databinding.AuditLogItemBinding
import com.fsck.k9.databinding.PassphraseManagementItemBinding
import security.planck.ui.audit.AuditLogDisplayViewHolder
import javax.inject.Inject

class PassphraseManagementAdapter @Inject constructor(): ListAdapter<String, PassphraseManagementViewHolder>(PassphraseManagementDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassphraseManagementViewHolder {
        val binding = PassphraseManagementItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PassphraseManagementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PassphraseManagementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PassphraseManagementDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}