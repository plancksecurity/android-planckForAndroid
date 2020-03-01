package com.fsck.k9.matchers

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class BackgroundColourMatcher(@field:ColorRes @param:ColorRes private val mExpectedColourResId: Int) :
        TypeSafeMatcher<View>(View::class.java) {
    private var mColorFromView = 0
    override fun matchesSafely(item: View): Boolean {
        if (item.background == null) {
            return false
        }
        val resources = item.context.resources
        val colourFromResources = ResourcesCompat.getColor(resources, mExpectedColourResId, null)
        mColorFromView = (item.background as ColorDrawable).color
        return mColorFromView == colourFromResources
    }

    override fun describeTo(description: Description) {
        description.appendText("Color did not match $mExpectedColourResId was $mColorFromView")
    }

}

fun withBackgroundColour(@ColorRes expectedColor: Int): Matcher<View> {
    return BackgroundColourMatcher(expectedColor)
}