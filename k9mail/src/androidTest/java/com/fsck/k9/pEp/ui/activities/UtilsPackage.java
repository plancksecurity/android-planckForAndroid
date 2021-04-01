package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Checks;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import timber.log.Timber;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isA;


public class UtilsPackage {

    public static class RecyclerViewMatcher {
        private final int recyclerViewId;

        RecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        public Matcher<View> atPosition(final int position) {
            return atPositionOnView(position, -1);
        }

        public Matcher<View> atPositionOnView(final int position, final int targetViewId) {

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
                        View listOrRecyclerView = view.getRootView().findViewById(recyclerViewId);
                        if(listOrRecyclerView == null || listOrRecyclerView.getId() != recyclerViewId) {
                            return false;
                        }
                        else if(listOrRecyclerView instanceof ListView) {
                            childView = ((ListView) listOrRecyclerView).getChildAt(position);
                        }
                        else if(listOrRecyclerView instanceof RecyclerView) {
                            childView = ((RecyclerView) listOrRecyclerView).getChildAt(position);
                        }
                        else {
                            throw new IllegalArgumentException("Expected ListView or RecyclerView but got "
                                    + listOrRecyclerView.getClass().getSimpleName());
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

    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new RecyclerViewMatcher(recyclerViewId);
    }

    private static Matcher<View> getElementFromMatchAtPosition(final Matcher<View> matcher, final int position) {
        return new BaseMatcher<View>() {
            int counter = 0;

            @Override
            public boolean matches(final Object item) {
                if (matcher.matches(item)) {
                    if (counter == position) {
                        counter++;
                        return true;
                    }
                    counter++;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Element at hierarchy position " + position);
            }
        };
    }

    public static Matcher<View> withListSize(final int size) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(final View view) {
                return ((ListView) view).getCount() == size;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("ListView should have " + size + " items");
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

    public static Matcher<View> withTextColor(final int expectedId) {
        return new BoundedMatcher<View, TextView>(TextView.class) {

            @Override
            protected boolean matchesSafely(TextView textView) {
                int colorId = ContextCompat.getColor(textView.getContext(), expectedId);
                return textView.getCurrentTextColor() == colorId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text color: ");
                description.appendValue(expectedId);
            }
        };
    }

    public static int getViewColorHSV(ViewInteraction interaction) {
        final int[] color = new int[1];
        interaction.perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "getting color from View";
            }

            @Override
            public void perform(UiController uiController, View view) {
                color[0] = 0;
                color[0] = ((ColorDrawable) view.getBackground()).getColor();
            }
        });
        return color[0];
    }

    public static Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup
                        && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
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
                size[0] = ((ListView) view).getCount();
                return size[0] != 0;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public static boolean hasValueEqualTo(ViewInteraction interaction, final String content) {
        final boolean[] value = {false};
        try {
            if (interaction != null) {
                interaction.perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(TextView.class);
                    }

                    @Override
                    public String getDescription() {
                        return "check for existence";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        value[0] = ((TextView) view).getText().toString().equals(content);
                    }
                });
            }
        } catch (Exception ex){
            Timber.e("Can not compare TextView with " + content);
        }
        return value[0];
    }
    public static boolean containstText(ViewInteraction interaction, final String content) {
        final boolean[] value = {false};
        try {
            if (interaction != null) {
                interaction.perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(TextView.class);
                    }

                    @Override
                    public String getDescription() {
                        return "check for existence";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        value[0] = ((TextView) view).getText().toString().toLowerCase().contains(content.toLowerCase());
                    }
                });
            }
        } catch (Exception ex){
            Timber.e("Can not compare TextView with " + content);
        }
        return value[0];
    }

    static Matcher<View> valuesAreEqual(final String firstValue, final String secondValue) {

        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Comparing values: " + firstValue + "  and  " + secondValue);
            }

            @Override
            public boolean matchesSafely(View view) {
                return firstValue.equals(secondValue);
            }
        };
    }

    public static Matcher<View> containsText(final String firstValue, final String secondValue) {

        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Assert text contains string: " + firstValue + "  and  " + secondValue);
            }

            @Override
            public boolean matchesSafely(View view) {
                return firstValue.contains(secondValue);
            }
        };
    }

    static ViewAction getTextInElement(String textToReturn[], int position) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                textToReturn[position] = "empty";
                TextView tv = (TextView) view;
                textToReturn[position] = tv.getText().toString();
            }

            @Override
            public String getDescription() {
                return "TextView";
            }
        };
    }

    public static String getTextFromView(ViewInteraction interaction) {
        final String[] stringHolder = {null};
        interaction.perform(new ViewAction() {
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
                TextView tv = (TextView) view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    public static ViewAction saveSizeInInt(int size[], int position) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(ListView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                size[position] = ((ListView) view).getCount();
            }

            @Override
            public String getDescription() {
                return "List size";
            }
        };
    }

    public static void waitUntilIdle() {
        new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for idle";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static boolean exists(ViewInteraction interaction) {
        try {
            if (interaction != null) {
                interaction.perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(View.class);
                    }

                    @Override
                    public String getDescription() {
                        return "check for existence";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        // no op, if this is run, then the execution will continue after .perform(...)
                    }
                });
            }
            return true;

        } catch (AmbiguousViewMatcherException ex) {
            // if there's any interaction later with the same matcher, that'll fail anyway
            return true; // we found more than one
        } catch (NoMatchingViewException | NoMatchingRootException ex) {
            return false;
        }
    }

    public static boolean viewIsDisplayed(int viewId)
    {
        final boolean[] isDisplayed = {true};
        onView(withId(viewId)).withFailureHandler((error, viewMatcher) -> isDisplayed[0] = false).check(matches(isCompletelyDisplayed()));
        return isDisplayed[0];
    }

    public static boolean viewIsDisplayed(ViewInteraction viewInteraction)
    {
        final boolean[] isDisplayed = {true};
        viewInteraction.withFailureHandler((error, viewMatcher) -> isDisplayed[0] = false).check(matches(isCompletelyDisplayed()));
        return isDisplayed[0];
    }

    static boolean viewWithTextIsDisplayed(String viewText)
    {
        final boolean[] isDisplayed = {true};
        onView(withText(viewText)).withFailureHandler((error, viewMatcher) -> isDisplayed[0] = false).check(matches(isDisplayed()));
        return isDisplayed[0];
    }

    public static ViewAction setChecked(final boolean checked) {
        return new ViewAction() {
            @Override
            public BaseMatcher<View> getConstraints() {
                return new BaseMatcher<View>() {
                    @Override
                    public boolean matches(Object item) {
                        return isA(Checkable.class).matches(item);
                    }

                    @Override
                    public void describeMismatch(Object item, Description mismatchDescription) {}

                    @Override
                    public void describeTo(Description description) {}
                };
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                Checkable checkableView = (Checkable) view;
                checkableView.setChecked(checked);
            }
        };
    }

    public static ViewAction setTextInTextView(final String value){
        return new ViewAction() {
            @SuppressWarnings("unchecked")
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).setText(value);
            }

            @Override
            public String getDescription() {
                return "replace text";
            }
        };
    }

    public static ViewAction appendTextInTextView(final String value){
        return new ViewAction() {
            @SuppressWarnings("unchecked")
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).append(value);
            }

            @Override
            public String getDescription() {
                return "replace text";
            }
        };
    }
}