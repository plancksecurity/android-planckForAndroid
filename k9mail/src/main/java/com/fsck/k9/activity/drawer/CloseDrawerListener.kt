package com.fsck.k9.activity.drawer

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout

abstract class CloseDrawerListener : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }
}