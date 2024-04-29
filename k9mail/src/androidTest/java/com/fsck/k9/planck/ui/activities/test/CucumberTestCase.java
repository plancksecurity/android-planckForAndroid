package com.fsck.k9.planck.ui.activities.test;

import android.os.Bundle;

import com.fsck.k9.BuildConfig;

import org.junit.Ignore;

import io.cucumber.android.runner.CucumberAndroidJUnitRunner;
import io.cucumber.junit.CucumberAndroidJUnitArguments;
import io.cucumber.junit.CucumberOptions;


/**
 * This class configures the Cucumber test framework and Java glue code
 *
 * Flavors' support: When you have multiple flavors the best configuration is to follow this steps:
 * 1- Create a copy of this file on each flavor's specific test package and delete the original file
 *      i.e. androidTestFlavor/java/com/neoranga55/cleanguitestarchitecture/test/CucumberTestCase.java
 * 2- Modify the original report path to include the flavor /mnt/sdcard/cucumber-reports/FLAVOR/cucumber-html-report
 * 3- Tag your scenarios in the feature files with new specific tags for each flavor and include them in the flavor's version of this file
 *      i.e. Add tag @flavor-one to a test scenario and modify the flavor's CucumberTestCase.java with tags={"~@manual", "@flavor-one"}
 */
@CucumberOptions(features = {"features/cucumber_tests"},
        glue = {"com.fsck.k9.planck.ui.activities.cucumber.steps"}
         ,tags={"~@ignore"}
)
@Ignore("Only to be run in Cucumber")
public class CucumberTestCase extends CucumberAndroidJUnitRunner {
        private static final String ARG_TEST_TYPE = "testType";
        private static final String ARG_FILTER = "filter";
        @Override
        public void onCreate(Bundle bundle) {
                Bundle args = bundle != null
                        ? bundle
                        : new Bundle();
                TestType testType = TestType.valueOf(args.getString(ARG_TEST_TYPE, TestType.normal.name()));
                args.remove(ARG_TEST_TYPE);
                args.putString(
                        CucumberAndroidJUnitArguments.Args.USE_DEFAULT_ANDROID_RUNNER,
                        String.valueOf(testType != TestType.cucumber)
                );
                String filterClassName;
                switch (testType) {
                        case cucumber:
                                filterClassName = CucumberTestFilter.class.getName();
                                break;
                        case screenshot:
                                filterClassName = ScreenshotTestFilter.class.getName();
                                break;
                        default:
                                filterClassName = NormalTestFilter.class.getName();
                }
                args.putString(ARG_FILTER, filterClassName);

                super.onCreate(args);
        }

        enum TestType {
                normal,
                cucumber,
                screenshot
        }
}