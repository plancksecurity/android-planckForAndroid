package security.planck.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.fsck.k9.R

class LoadingPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        widgetLayoutResource = R.layout.preference_loading_widget
    }
    var loading: View? = null
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        loading = holder?.itemView?.findViewById(R.id.loading)
    }
}