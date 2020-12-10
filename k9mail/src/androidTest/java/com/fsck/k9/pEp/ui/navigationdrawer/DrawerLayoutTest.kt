package com.fsck.k9.pEp.ui.navigationdrawer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class DrawerLayoutTest {

    fun openDrawer() {
        // click hamburger account
        // check if drawer header is visible
        // check if drawer folders are visible
    }

    fun clickAccountBall() {
        // click first account
        // check if drawer header changed to selected account
        // check if drawer accounts changed to other accounts
        // check if drawer folders changed to selected account
    }

    fun changeAccountFolders() {
        // click button to show accounts
        // check if accounts are visible
        // click button to show folders
        // check if folders are visible
    }

    fun clickFolders() {
        // click unified account
        // check if unified messages is showing in messageList
        // click all messages
        // check if all messages is showing in messageList
        // click inbox folder
        // check if inbox is showing in messageList
        // click <last folder> after inbox
        // check if <last folder> is showing in messageList
    }

    fun clickAccountInList() {
        // click first account in list
        // check if drawer folders are visible
        // check if drawer header changed to selected account
        // check if drawer accounts changed to other accounts
        // check if drawer folders changed to selected account
    }

    fun clickAddAccount() {
        // click add account button
        // check if add account opened
    }

    fun clickSettings() {
        // click settings button
        // check if settings opened
    }
}