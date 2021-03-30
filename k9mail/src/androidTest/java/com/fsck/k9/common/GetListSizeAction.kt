package com.fsck.k9.common

import android.view.View
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class GetListSizeAction : ViewAction {
    var size: Int = 0
        private set

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(RecyclerView::class.java)
    }

    override fun getDescription(): String {
        return "get list size"
    }

    override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView
        size = recyclerView.size
    }
}