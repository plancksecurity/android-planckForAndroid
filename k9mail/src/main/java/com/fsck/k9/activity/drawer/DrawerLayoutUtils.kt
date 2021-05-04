package com.fsck.k9.activity.drawer

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout

fun onDrawerClosed(block: () -> Unit): DrawerLayout.DrawerListener {
    return object : DrawerLayout.DrawerListener {
        override fun onDrawerStateChanged(newState: Int) {

        }

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

        }

        override fun onDrawerOpened(drawerView: View) {

        }

        override fun onDrawerClosed(drawerView: View) {
            block.invoke()
        }
    }
}
