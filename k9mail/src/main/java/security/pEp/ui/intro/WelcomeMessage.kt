package security.pEp.ui.intro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.ui.fragments.PEpFragment
import com.fsck.k9.ui.contacts.ContactPictureLoader
import com.github.paolorotolo.appintro.AppIntro
import foundation.pEp.jniadapter.Rating
import kotlinx.android.synthetic.main.fragment_intro_first.*
import kotlinx.android.synthetic.main.fragment_intro_fourth.*
import security.pEp.ui.permissions.PermissionsActivity
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import javax.inject.Inject


private const val AUTOMATIC: String = "automatic"
fun Activity.startWelcomeMessage() =
        Intent(this, WelcomeMessage::class.java).let(this::startActivity)

fun Activity.startTutorialMessage() {
    val intent = Intent(this, WelcomeMessage::class.java)
    intent.putExtra(AUTOMATIC, false)
    startActivity(intent)
}

class WelcomeMessage : AppIntro() {

    var automatic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.decorView.systemUiVisibility =
                    FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
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
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val white = ContextCompat.getColor(this, R.color.white)
        val lightGray = ContextCompat.getColor(this, R.color.light_gray)
        setIndicatorColor(primaryColor, lightGray)
        setSeparatorColor(white)
        setColorSkipButton(primaryColor)
        setColorDoneText(primaryColor)
        setNextArrowColor(primaryColor)
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
        if (automatic)
            if (K9.isShallRequestPermissions()) {
                PermissionsActivity.actionAskPermissions(this)
            } else {
                AccountSetupBasics.actionNewAccount(this)
            }
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

}
class IntroFirstFragment : PEpFragment() {

    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
        startTexts()
    }

    private fun startTexts() {
        headerText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_1),HtmlCompat.FROM_HTML_MODE_LEGACY)
        secondText.text = HtmlCompat.fromHtml(getString(R.string.intro_frag_first_text_2),HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun startImage() {
        val address = Address("A")
        contactBadge.setPepRating(Rating.pEpRatingReliable, true)
        contactsPictureLoader.setContactPicture(contactBadge, address)
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

class IntroFourthFragment : PEpFragment() {

    @Inject
    lateinit var contactsPictureLoader: ContactPictureLoader

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_fourth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
    }

    private fun startImage() {
        var address = Address("A")
        secureBadge.setPepRating(Rating.pEpRatingReliable, true)
        contactsPictureLoader.setContactPicture(secureBadge, address)
        address = Address("B")
        secureTrustedBadge.setPepRating(Rating.pEpRatingTrusted, true)
        contactsPictureLoader.setContactPicture(secureTrustedBadge, address)
    }

}

