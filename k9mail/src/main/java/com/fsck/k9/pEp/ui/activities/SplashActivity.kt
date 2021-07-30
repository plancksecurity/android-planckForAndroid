package com.fsck.k9.pEp.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.window.WindowLayoutInfo
import com.fsck.k9.K9
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.pEp.ui.LayoutStateMonitor
import security.pEp.ui.PEpUIUtils.isUnfoldedFoldableDevice


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutStateMonitor.initialize(this)
        layoutStateMonitor.register()
    }

    private val layoutStateMonitor: LayoutStateMonitor = object : LayoutStateMonitor() {
        override fun onLayoutStateChanged(windowLayoutInfo: WindowLayoutInfo) {
            val isFoldableScreenUnfolded = windowLayoutInfo.isUnfoldedFoldableDevice()
            if (isFoldableScreenUnfolded) {
                if (K9.getSplitViewMode() == K9.SplitViewMode.ALWAYS) {
                    K9.setSplitViewMode(K9.SplitViewMode.NEVER)
                }
            }
            K9.setIsTablet(
                !isFoldableScreenUnfolded
                        && resources.configuration.smallestScreenWidthDp >=
                        K9.MINIMUM_WIDTH_FOR_SPLIT_SCREEN
            )
            SettingsActivity.actionBasicStart(this@SplashActivity)
            finish()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        layoutStateMonitor.unRegister()
    }
}
