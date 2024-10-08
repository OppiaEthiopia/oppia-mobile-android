package androidTestFiles.activities;

import android.Manifest;

import org.digitalcampus.mobile.learning.BuildConfig;
import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.activity.AboutActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;

import static androidTestFiles.utils.UITestActionsUtils.waitForView;

@RunWith(AndroidJUnit4.class)
public class AboutActivityTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public ActivityScenarioRule<AboutActivity> aboutActivityTestRule =
            new ActivityScenarioRule<>(AboutActivity.class);

    @Test
    public void clickHelpTab() {
        waitForView(allOf(withText(R.string.tab_title_help),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                isDescendantOfA(withId(R.id.tabs_toolbar))))
                .perform(click());
    }

    @Test
    public void clickAboutTab() {

        waitForView(allOf(withText(R.string.tab_title_about_ee),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                isDescendantOfA(withId(R.id.tabs_toolbar))))
                .perform(click());

        waitForView(withId(R.id.about_versionno)).check(matches(withText(containsString(BuildConfig.VERSION_NAME))));
    }


}
