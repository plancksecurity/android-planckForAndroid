package com.fsck.k9.pEp.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import com.fsck.k9.R
import com.fsck.k9.activity.K9ActivityCommon
import com.fsck.k9.pEp.ui.tools.KeyboardUtils

class PEpSearchViewAnimationController(private val activity: Activity) {
    interface SearchAnimationCallback {
        fun onAnimationBackwardsFinished()
        fun onAnimationForwardFinished()
        fun todoIfIsAndroidLolllipop(isAndroidLollipop: Boolean)
    }

    fun showSearchView(
            searchBarMotionLayout: MotionLayout,
            searchLayout: View,
            searchInput: EditText,
            toolbar: Toolbar,
            searchAnimationCallback: SearchAnimationCallback
    ) {
        if (K9ActivityCommon.isAndroidLollipop()) {
            searchAnimationCallback.todoIfIsAndroidLolllipop(true)
        } else {
            searchAnimationCallback.todoIfIsAndroidLolllipop(false)
            searchBarMotionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
                override fun onTransitionStarted(motionLayout: MotionLayout, i: Int, i1: Int) {
                    if (i == R.id.start) {
                        searchInput.error = null
                        searchInput.hint = null
                        searchInput.isEnabled = false
                        searchInput.setText(null)
                        searchLayout.visibility = View.VISIBLE
                    } else if (i == R.id.end) {
                        toolbar.alpha = 0f
                    }
                }

                override fun onTransitionChange(motionLayout: MotionLayout, i: Int, i1: Int, v: Float) {
                    if (i == R.id.start) {
                        toolbar.alpha = 1 - v
                    } else if (i == R.id.end) {
                        toolbar.alpha = v
                    }
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout, i: Int) {
                    if (i == R.id.start) {
                        backwardsAnimationCompleted(searchLayout, toolbar, searchInput, searchAnimationCallback)
                    } else if (i == R.id.end) {
                        forwardAnimationCompleted(searchLayout, toolbar, searchInput, searchAnimationCallback)
                    }
                }

                override fun onTransitionTrigger(motionLayout: MotionLayout, i: Int, b: Boolean, v: Float) {
                    // NOP
                }
            })
            searchLayout.visibility = View.VISIBLE
            searchBarMotionLayout.transitionToEnd()
        }
    }

    fun hideSearchView(toolbar: Toolbar, searchBarMotionLayout: MotionLayout) {
        toolbar.visibility = View.VISIBLE
        searchBarMotionLayout.transitionToStart()
    }

    private fun forwardAnimationCompleted(searchLayout: View, toolbar: Toolbar, searchInput: EditText, searchAnimationCallback: SearchAnimationCallback) {
        searchLayout.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
        searchInput.isEnabled = true
        searchInput.setHint(R.string.search_action)
        setFocusOnKeyboard(searchInput)
        searchAnimationCallback.onAnimationForwardFinished()
    }

    private fun backwardsAnimationCompleted(searchLayout: View, toolbar: Toolbar, searchInput: EditText, searchAnimationCallback: SearchAnimationCallback) {
        searchLayout.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
        KeyboardUtils.hideKeyboard(searchInput)
        searchAnimationCallback.onAnimationBackwardsFinished()
    }


    private fun setFocusOnKeyboard(searchInput: EditText) {
        searchInput.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }
}