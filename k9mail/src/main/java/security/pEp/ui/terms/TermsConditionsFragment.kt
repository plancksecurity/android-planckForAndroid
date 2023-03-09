package security.pEp.ui.terms

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import com.fsck.k9.R

class TermsConditionsFragment : Fragment() {

    companion object {
        fun newInstance() = TermsConditionsFragment()
    }

    private lateinit var viewModel: TermsConditionsViewModel
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TermsConditionsViewModel::class.java)
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        if (isEnabled) {
                            isEnabled = false
                            requireActivity().finish()
                        }
                    }
                }
            }
            )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.terms_webview)
        webView.webViewClient = WebViewClient()
        viewModel.getTermsLink().let { if (it.isNotEmpty()) {webView.loadUrl(it)} else requireActivity().finish() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.terms_conditions_fragment_main, container, false)
    }

}
