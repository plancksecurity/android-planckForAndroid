package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.fsck.k9.activity.setup.AccountSetupBasics;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ImportTest {

    @Rule
    public ActivityTestRule<AccountSetupBasics> mActivityRule =
            new ActivityTestRule<>(AccountSetupBasics.class);

    @Before
    public void setUp() throws Exception {
        File filesDir = getInstrumentation().getContext().getFilesDir();
        File settings = new File(filesDir, "settings.k9s");

        Intent intent = new Intent();
        intent.setData(Uri.parse(String.valueOf(settings.toURI())));
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, intent);

        intending(allOf(
                hasData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                hasAction(Intent.ACTION_PICK))
        ).respondWith(result);



        //Instrumentation.ActivityResult result = createImageCaptureActivityResultStub();

        //intending(hasComponent(AccountSetupBasics.class.getName())).respondWith(result);
    }

    private Instrumentation.ActivityResult createImageCaptureActivityResultStub() throws IOException {
        File filesDir = getInstrumentation().getContext().getFilesDir();
        File settings = new File(filesDir, "settings.k9s");
        Intent mResultIntent =
                new Intent("com.fsck.k9.ACTION_RETURN_FILE", Uri.parse(String.valueOf(settings.toURI())));

        return new Instrumentation.ActivityResult(Activity.RESULT_OK, mResultIntent);
    }
}
