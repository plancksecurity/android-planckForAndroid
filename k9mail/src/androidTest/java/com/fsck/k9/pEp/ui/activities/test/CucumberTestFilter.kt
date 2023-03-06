package com.fsck.k9.pEp.ui.activities.test
/*
import androidx.test.internal.runner.filters.ParentFilter
import org.junit.runner.Description

class CucumberTestFilter : ParentFilter() {
    override fun describe(): String {
        return "Filter Cucumber tests"
    }

    override fun evaluateTest(description: Description?): Boolean {
        return description != null &&
                (description.testClass == null || description.testClass.name.contains("cucumber"))
    }
}

class NonCucumberTestFilter : ParentFilter() {
    override fun describe(): String {
        return "Filter out Cucumber tests"
    }

    override fun evaluateTest(description: Description?): Boolean {
        val packageName = description?.testClass?.`package`?.name
        return !packageName.isNullOrBlank() && !packageName.contains("cucumber")
    }
}
*/