package security.pEp.ui.terms

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.R

class TermsConditionsFragment : Fragment() {

    companion object {
        fun newInstance() = TermsConditionsFragment()
    }

    private lateinit var viewModel: TermsConditionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TermsConditionsViewModel::class.java)
        viewModel.getTermsLink()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.terms_conditions_fragment_main, container, false)
    }

}