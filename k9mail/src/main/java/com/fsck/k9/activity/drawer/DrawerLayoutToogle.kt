package com.fsck.k9.activity.drawer

import android.app.Activity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout

class DrawerLayoutToogle : ActionBarDrawerToggle {

    private var drawerLayoutView: DrawerLayoutView? = null

    constructor(activity: Activity?, drawerLayout: DrawerLayout?, openDrawerContentDescRes: Int,
                closeDrawerContentDescRes: Int) : super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes)

    constructor(activity: Activity?, drawerLayout: DrawerLayout?, toolbar: Toolbar?,
                openDrawerContentDescRes: Int, closeDrawerContentDescRes: Int) : super(activity,
            drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes)

    fun setDrawerLayoutView(drawerLayoutView: DrawerLayoutView) {
        this.drawerLayoutView = drawerLayoutView
    }

    override fun onDrawerOpened(drawerView: View) {
        drawerLayoutView?.drawerOpened()
        super.onDrawerOpened(drawerView)
    }

    override fun onDrawerClosed(drawerView: View) {
        drawerLayoutView?.drawerClosed()
        super.onDrawerClosed(drawerView)
    }
}