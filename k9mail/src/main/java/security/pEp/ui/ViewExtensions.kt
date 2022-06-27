package security.pEp.ui

import android.view.View
import androidx.core.view.ViewCompat

/**
 * Performs the given action when this view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnLayout
 */
inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    })
}

/**
 * Performs the given action when this view is laid out. If the view has been laid out and it
 * has not requested a layout, the action will be performed straight away, otherwise the
 * action will be performed after the view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnNextLayout
 */
inline fun View.doOnLayout(crossinline action: (view: View) -> Unit) {
    if (ViewCompat.isLaidOut(this) && !isLayoutRequested) {
        action(this)
    } else {
        doOnNextLayout {
            action(it)
        }
    }
}
