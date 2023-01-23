package security.pEp.ui.intro

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import com.fsck.k9.databinding.FragmentIntroSecondBinding
import com.fsck.k9.databinding.FragmentIntroThirdBinding
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.ui.fragments.PEpFragment
import com.fsck.k9.pEp.ui.tools.ThemeManager
import com.fsck.k9.ui.contacts.ContactPictureLoader
import com.github.paolorotolo.appintro.AppIntro
import foundation.pEp.jniadapter.Rating
import security.pEp.ui.PEpUIUtils
import security.pEp.ui.permissions.PermissionsActivity
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

class WelcomeMessage : AppIntro() {

    var automatic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.appThemeResourceId)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.decorView.systemUiVisibility =
                    FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
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
class IntroFirstFragment : PEpFragment() {

    private lateinit var binding: FragmentIntroFirstBinding
    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentIntroFirstBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
        startTexts()
    }

    private fun startTexts() {
        val primaryColorARGB = PEpUIUtils.getColorAsString(requireContext(), R.color.colorPrimary)
        with(binding){
            headerText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_1, primaryColorARGB), HtmlCompat.FROM_HTML_MODE_LEGACY)
            secondText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_2, primaryColorARGB), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun startImage() {
        val address = Address("A")
        with(binding){
            contactBadge.setPepRating(Rating.pEpRatingReliable, true)
            contactsPictureLoader.setContactPicture(contactBadge, address)
        }
    }
}

class IntroSecondFragment : Fragment() {
    private lateinit var binding: FragmentIntroSecondBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentIntroSecondBinding.inflate(inflater, container, false)
        return binding.root;
    }

}

class IntroThirdFragment : Fragment() {
    private lateinit var binding: FragmentIntroThirdBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentIntroThirdBinding.inflate(inflater, container, false)
        return binding.root;
    }

}

class IntroFourthFragment : PEpFragment() {
    private lateinit var binding: FragmentIntroFourthBinding

    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentIntroFourthBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
    }

    private fun startImage() {
        var address = Address("A")
        with(binding){
            secureBadge.setPepRating(foundation.pEp.jniadapter.Rating.pEpRatingReliable, true)
            contactsPictureLoader.setContactPicture(secureBadge, address)
            address = Address("B")
            secureTrustedBadge.setPepRating(foundation.pEp.jniadapter.Rating.pEpRatingTrusted, true)
            contactsPictureLoader.setContactPicture(secureTrustedBadge, address)
        }
    }

}

