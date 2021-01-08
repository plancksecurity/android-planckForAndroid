package com.fsck.k9.common

import android.view.View
import androidx.core.view.ScrollingView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class ScrollParentScrollViewAction : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(RecyclerView::class.java)
    }

    override fun getDescription(): String {
        return "scroll to bottom"
    }

    override fun perform(uiController: UiController, view: View) {
        var scrollView = view.parent
        while (scrollView !is ScrollingView) scrollView = scrollView.parent
        (scrollView as NestedScrollView).fullScroll(View.FOCUS_DOWN)
    }
}