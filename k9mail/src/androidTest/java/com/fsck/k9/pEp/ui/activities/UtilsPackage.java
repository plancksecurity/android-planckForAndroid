package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.FailureHandler;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.platform.app.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isA;


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
                        } else {
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
    public static Matcher<View> valuesAreEqual(final String firstValue, final String secondValue) {

        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Comparing values: " + firstValue + "  and  " + secondValue);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (firstValue.equals(secondValue)) {
                    return true;
                }
                return false;
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

    String getText(final Matcher<View> matcher) {
        final String[] stringHolder = {null};
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
                TextView tv = (TextView) view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    static ViewAction saveSizeInInt(int size[], int position) {
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

    public static ViewAction waitUntilIdle() {
        return new ViewAction() {
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
        } catch (NoMatchingViewException ex) {
            return false;
        } catch (NoMatchingRootException ex) {
            // optional depending on what you think "exists" means
            return false;
        }
    }

    public static boolean viewIsDisplayed(int viewId)
    {
        final boolean[] isDisplayed = {true};
        onView(withId(viewId)).withFailureHandler(new FailureHandler() {
            @Override
            public void handle(Throwable error, Matcher<View> viewMatcher)
            {
                isDisplayed[0] = false;
            }
        }).check(matches(isDisplayed()));
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
}