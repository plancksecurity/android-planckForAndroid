package com.fsck.k9.ui.settings.account

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.fsck.k9.R
import com.fsck.k9.databinding.SignaturePreferenceBinding


class SignaturePreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private lateinit var binding: SignaturePreferenceBinding

    init {
        layoutResource = R.layout.signature_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = SignaturePreferenceBinding.bind(holder.itemView)
        val default = context.getString(R.string.default_signature)
        binding.resetButton.setOnClickListener {
            binding.signatureEdit.setText(default)
            if (callChangeListener(default)) {
                persistString(default)
            }
        }
        val currentValue = getPersistedString(default)
        binding.signatureEdit.setText(currentValue)

        binding.signatureEdit.doAfterTextChanged {
            if (callChangeListener(it!!.toString())) {
                persistString(it.toString())
            }
        }
    }
}