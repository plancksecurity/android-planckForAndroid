package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.intent.Checks;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.util.HumanReadables;
import android.support.test.espresso.util.TreeIterables;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.concurrent.TimeoutException;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;


public class UtilsPackage {

    public static class RecyclerViewMatcher {
        private final int recyclerViewId;

        RecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        Matcher<View> atPosition(final int position) {
            return atPositionOnView(position, -1);
        }

        Matcher<View> atPositionOnView(final int position, final int targetViewId) {

            return new TypeSafeMatcher<View>() {
                Resources resources = null;
                View childView;

                public void describeTo(Description description) {
                    String idDescription = Integer.toString(recyclerViewId);
                    if (this.resources != null) {
                        try {
                            idDescription = this.resources.getResourceName(recyclerViewId);
                        } catch (Resources.NotFoundException var4) {
                            idDescription = String.format("%s (resource name not found)",
                                    recyclerViewId);
                        }
                    }

                    description.appendText("with id: " + idDescription);
                }

                public boolean matchesSafely(View view) {

                    this.resources = view.getResources();

                    if (childView == null) {
                        RecyclerView recyclerView =
                                view.getRootView().findViewById(recyclerViewId);
                        if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                            childView = recyclerView.findViewHolderForAdapterPosition(position).itemView;
                        }
                        else {
                            return false;
                        }
                    }

                    if (targetViewId == -1) {
                        return view == childView;
                    } else {
                        View targetView = childView.findViewById(targetViewId);
                        return view == targetView;
                    }

                }
            };
        }
    }
    private static Matcher<View> getElementFromMatchAtPosition(final Matcher<View> matcher, final int position) {
        return new BaseMatcher<View>() {
            int counter = 0;
            @Override
            public boolean matches(final Object item) {
                if (matcher.matches(item)) {
                    if(counter == position) {
                        counter++;
                        return true;
                    }
                    counter++;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Element at hierarchy position "+position);
            }
        };
    }

        public static Matcher<View> withListSize(final int size) {
            return new TypeSafeMatcher<View> () {
                @Override public boolean matchesSafely (final View view) {
                    return ((ListView) view).getCount () == size;
                }

                @Override public void describeTo (final Description description) {
                    description.appendText ("ListView should have " + size + " items");
                }
            };
        }

    public static Matcher<View> withBackgroundColor(final int colorId) {
        Checks.checkNotNull(colorId);
        int colorFromResource = ContextCompat.getColor(getTargetContext(), colorId);
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public boolean matchesSafely(View view) {
                int backGroundColor = ((ColorDrawable) view.getBackground()).getColor();
                return colorFromResource == backGroundColor;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public static Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup
                        && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup)parent).getChildAt(position));
            }
            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public static Matcher<View> returnElementsSize(int size[]) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                size[0] = ((ListView) view).getCount ();
                return size[0]!=0;
            }
            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public static Matcher<View> hasValueEqualTo(final String content) {

        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Has EditText/TextView the value:  " + content);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof TextView) && !(view instanceof EditText)) {
                    return false;
                }
                if (view != null) {
                    String text;
                    if (view instanceof TextView) {
                        text = ((TextView) view).getText().toString();
                    } else {
                        text = ((EditText) view).getText().toString();
                    }

                    return (text.equalsIgnoreCase(content));
                }
                return false;
            }
        };
    }

    static ViewAction getTextInElement(String textToReturn[], int position){
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                textToReturn[position] = "empty";
                TextView tv = (TextView)view;
                textToReturn[position] = tv.getText().toString();
            }

            @Override
            public String getDescription() {
                return "TextView";
            }
        };
    }

    String getText(final Matcher<View> matcher) {
        final String[] stringHolder = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    static ViewAction saveSizeInInt(int size[], int position){
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(ListView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                size[position] = ((ListView) view).getCount ();
            }

            @Override
            public String getDescription() {
                return "List size";
            }
        };
    }

    public static ViewAction waitId(final int viewId, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific view with id <" + viewId + "> during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                final Matcher<View> viewMatcher = withId(viewId);

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }
}