package com.fsck.k9.common

import android.view.View
import com.fsck.k9.R
import android.widget.TextView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class GetNavigationAccountEmailAction : ViewAction {
    var text: CharSequence? = null
        private set

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(TextView::class.java)
    }

    override fun getDescription(): String {
        return "get navigation account email"
    }

    override fun perform(uiController: UiController, view: View) {
        val textView = view.findViewById(R.id.account_email) as TextView
        text = textView.text
    }
}