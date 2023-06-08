package com.fsck.k9.planck.ui.activities.test

import android.util.Log
import androidx.test.filters.AbstractFilter
import org.junit.runner.Description

class CucumberTestFilter : AbstractFilter() {
    override fun describe(): String {
        return "Filter Cucumber tests"
    }

    override fun evaluateTest(description: Description?): Boolean {
        return description != null &&
                (description.testClass == null || description.testClass.name.contains("cucumber"))
    }
}

class NormalTestFilter : AbstractFilter() {
    override fun describe(): String {
        return "Filter out Cucumber and Screenshot tests"
    }

    override fun evaluateTest(description: Description?): Boolean {
        val packageName = description?.testClass?.`package`?.name
        Log.e("EFA-53", "PACKAGE NAME: $packageName")
        return !packageName.isNullOrBlank() && !description.className.contains("Screenshot") && !packageName.contains("cucumber")
    }
}

class ScreenshotTestFilter : AbstractFilter() {
    override fun describe(): String {
        return "Filter out Cucumber and normal tests"
    }

    override fun evaluateTest(description: Description?): Boolean {
        val packageName = description?.testClass?.`package`?.name
        Log.e("EFA-53", "PACKAGE NAME: $packageName")
        return packageName == "com.fsck.k9.ui" && description.className.contains("Screenshot") && !description.className.contains("Base")
    }
}
