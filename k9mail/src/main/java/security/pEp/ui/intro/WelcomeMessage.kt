package security.pEp.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.helper.ContactPicture
import com.fsck.k9.mail.Address
import com.github.paolorotolo.appintro.AppIntro
import foundation.pEp.jniadapter.Rating
import kotlinx.android.synthetic.main.fragment_intro_first.*
import kotlinx.android.synthetic.main.fragment_intro_fourth.*
import security.pEp.ui.permissions.PermissionsActivity
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


fun Context.startWelcomeActivity(context: Context) =
        Intent(context, WelcomeMessage::class.java).let(this::startActivity)

class WelcomeMessage : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(IntroFirstFragment())
        addSlide(IntroSecondFragment())
        addSlide(IntroThirdFragment())
        addSlide(IntroFourthFragment())

        showSkipButton(true)
        setAppIntroColors()
        isProgressButtonEnabled = true
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

class IntroFirstFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startImage()
        startTexts();
    }

    private fun startTexts() {

    }

    private fun startImage() {
        val address = Address("A")
        contactBadge.setPepRating(Rating.pEpRatingReliable, true)
        ContactPicture.getGrayPictureLoader(context).loadContactPicture(address, contactBadge)
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

class IntroFourthFragment : Fragment() {

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
        ContactPicture.getGrayPictureLoader(context).loadContactPicture(address, secureBadge)
        address = Address("B")
        secureTrustedBadge.setPepRating(Rating.pEpRatingTrusted, true)
        ContactPicture.getGrayPictureLoader(context).loadContactPicture(address, secureTrustedBadge)
    }

}

