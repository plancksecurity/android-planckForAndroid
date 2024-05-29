package security.planck.passphrase

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.databinding.AuditLogItemBinding
import com.fsck.k9.databinding.PassphraseManagementItemBinding

class PassphraseManagementViewHolder(
    private val binding: PassphraseManagementItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(email: String) {
        binding.passphraseEmail.text = email
        binding.enablePassphrase.setOnCheckedChangeListener { _, isChecked ->
            binding.passphraseContainer.isVisible = isChecked
        }
    }
}