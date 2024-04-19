package security.planck.ui.intro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import androidx.annotation.AttrRes
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.K9ActivityCommon
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.databinding.FragmentIntroFirstBinding
import com.fsck.k9.databinding.FragmentIntroFourthBinding
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.ui.tools.ThemeManager
import com.fsck.k9.ui.contacts.ContactPictureLoader
import com.github.paolorotolo.appintro.AppIntro
import dagger.hilt.android.AndroidEntryPoint
import foundation.pEp.jniadapter.Rating
import security.planck.ui.PlanckUIUtils
import security.planck.ui.permissions.PermissionsActivity
import javax.inject.Inject


private const val AUTOMATIC: String = "automatic"
fun Activity.startWelcomeMessage() =
        Intent(this, WelcomeMessage::class.java).let(this::startActivity)

fun Activity.startTutorialMessage() {
    val intent = Intent(this, WelcomeMessage::class.java)
    intent.putExtra(AUTOMATIC, false)
    startActivity(intent)
}

fun startOnBoarding(activity: Activity) {
    if (K9.isShallRequestPermissions()) {
        PermissionsActivity.actionAskPermissions(activity)
    } else {
        AccountSetupBasics.actionNewAccount(activity)
    }
}

@AndroidEntryPoint
class WelcomeMessage : AppIntro() {

    var automatic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.appThemeResourceId)
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
                FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        K9ActivityCommon.configureNavigationBar(this)
        addSlide(IntroFirstFragment())
        addSlide(IntroSecondFragment())
        addSlide(IntroThirdFragment())
        addSlide(IntroFourthFragment())

        showSkipButton(true)
        setAppIntroColors()
        isProgressButtonEnabled = true

        loadIntentData()
    }

    private fun loadIntentData() {
        automatic = intent.getBooleanExtra(AUTOMATIC, true)
    }

    private fun setAppIntroColors() {
        val primaryColor = getColorFromAttributeResource(R.attr.introPrimaryColor)
        val separatorColor = getColorFromAttributeResource(R.attr.introSeparatorColor)
        val indicatorColor = getColorFromAttributeResource(R.attr.introIndicatorColor)
        setIndicatorColor(primaryColor, indicatorColor)
        setSeparatorColor(separatorColor)
        setColorSkipButton(primaryColor)
        setColorDoneText(primaryColor)
        setNextArrowColor(primaryColor)
    }

    private fun getColorFromAttributeResource(@AttrRes resource: Int): Int {
        return ThemeManager.getColorFromAttributeResource(this, resource)
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        nextAction()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        nextAction()
    }

    private fun nextAction() {
        if (automatic) {
            startOnBoarding(this)
        }
        finish()
    }

}

@AndroidEntryPoint
class IntroFirstFragment : Fragment() {
    private var _binding: FragmentIntroFirstBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIntroFirstBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
        startTexts()
    }

    private fun startTexts() {
        val primaryColorARGB = PlanckUIUtils.getColorAsString(requireContext(), R.color.colorPrimary)
        binding.headerText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_1, primaryColorARGB), HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.secondText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_2, primaryColorARGB), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun startImage() {
        val address = Address("A")
        binding.contactBadge.setPlanckRating(Rating.pEpRatingReliable, true)
        contactsPictureLoader.setContactPicture(binding.contactBadge, address)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class IntroSecondFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_second, container, false)
    }

}

class IntroThirdFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_third, container, false)
    }

}

@AndroidEntryPoint
class IntroFourthFragment : Fragment() {
    private var _binding: FragmentIntroFourthBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIntroFourthBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
    }

    private fun startImage() {
        var address = Address("A")
        binding.secureBadge.setPlanckRating(Rating.pEpRatingReliable, true)
        contactsPictureLoader.setContactPicture(binding.secureBadge, address)
        address = Address("B")
        binding.secureTrustedBadge.setPlanckRating(Rating.pEpRatingTrusted, true)
        contactsPictureLoader.setContactPicture(binding.secureTrustedBadge, address)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}

