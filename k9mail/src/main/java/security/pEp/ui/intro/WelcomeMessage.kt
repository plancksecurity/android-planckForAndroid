package security.pEp.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.github.paolorotolo.appintro.AppIntro

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
        isProgressButtonEnabled = true
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

}

