package androidTestFiles.widgets.quiz;

import static androidx.fragment.app.testing.FragmentScenario.launchInContainer;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidTestFiles.utils.UITestActionsUtils.waitForView;

import static org.hamcrest.CoreMatchers.allOf;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.activity.CourseActivity;
import org.digitalcampus.oppia.model.Activity;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.Lang;
import org.digitalcampus.oppia.widgets.QuizWidget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import androidTestFiles.utils.FileUtils;
import androidTestFiles.utils.TestUtils;
import androidTestFiles.utils.parent.BaseTest;

@RunWith(AndroidJUnit4.class)
public class BaselineQuizTest {

    private static final String BASELINE_QUIZ_JSON = BaseTest.PATH_QUIZZES + "/baseline_quiz.json";
    private static final String QUESTION_TITLE_1 = "The capital of Finland is Helsinki";
    private static final String CORRECT_ANSWER_1 = "True";
    private static final String INCORRECT_ANSWER_1 = "False";

    private static final String QUESTION_TITLE_2 = "The sky is blue";
    private static final String CORRECT_ANSWER_2 = "True";
    private static final String INCORRECT_ANSWER_2 = "False";

    private Activity act;
    private Bundle args;

    @Before
    public void setup() throws Exception {
        // Setting up before every test
        act = new Activity();
        String quizContent = FileUtils.getStringFromFile(
                InstrumentationRegistry.getInstrumentation().getContext(),
                BASELINE_QUIZ_JSON);

        ArrayList<Lang> contents = new ArrayList<>();
        contents.add(new Lang("en", quizContent));
        act.setContents(contents);
        act.setDigest("2aadd06f629370028539ee0cac50f738528cr0s2p80m0");
        args = new Bundle();
        args.putSerializable(Activity.TAG, act);
        args.putSerializable(Course.TAG, new Course(""));
        args.putBoolean(CourseActivity.BASELINE_TAG, true);
    }

    @Test
    public void allCorrect() {
        launchInContainer(QuizWidget.class, args, R.style.Oppia_ToolbarTheme);
        waitForView(withId(R.id.take_quiz_btn)).perform(click());
        waitForView(allOf(withId(R.id.question_text), isCompletelyDisplayed()))
                .check(matches(withText(QUESTION_TITLE_1)));

        waitForView(withText(CORRECT_ANSWER_1)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        waitForView(withId(R.id.question_text))
                .check(matches(withText(QUESTION_TITLE_2)));

        waitForView(withText(CORRECT_ANSWER_2)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        String actual = TestUtils.getCurrentActivity().getString(R.string.widget_quiz_results_score, (float) 100);
        waitForView(withId(R.id.quiz_results_score))
                .check(matches(withText(actual)));

        // TODO check that the feedback is *not* displayed

        waitForView(withId(R.id.quiz_exit_button))
                .check(matches(withText(R.string.widget_quiz_continue)));

    }

    @Test
    public void partiallyCorrect() throws InterruptedException {
        launchInContainer(QuizWidget.class, args, R.style.Oppia_ToolbarTheme);
        waitForView(withId(R.id.take_quiz_btn)).perform(click());
        waitForView(withId(R.id.question_text))
                .check(matches(withText(QUESTION_TITLE_1)));

        waitForView(withText(INCORRECT_ANSWER_1)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        waitForView(withId(R.id.question_text))
                .check(matches(withText(QUESTION_TITLE_2)));

        waitForView(withText(CORRECT_ANSWER_2)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        String actual = TestUtils.getCurrentActivity().getString(R.string.widget_quiz_results_score, (float) 50);
        waitForView(withId(R.id.quiz_results_score))
                .check(matches(withText(actual)));

        // TODO check that the feedback is *not* displayed

        waitForView(withId(R.id.quiz_exit_button))
                .check(matches(withText(R.string.widget_quiz_continue)));
    }

    @Test
    public void allIncorrect() {
        launchInContainer(QuizWidget.class, args, R.style.Oppia_ToolbarTheme);

        waitForView(withId(R.id.take_quiz_btn)).perform(click());
        waitForView(withId(R.id.question_text))
                .check(matches(withText(QUESTION_TITLE_1)));

        waitForView(withText(INCORRECT_ANSWER_1)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        waitForView(withId(R.id.question_text))
                .check(matches(withText(QUESTION_TITLE_2)));

        waitForView(withText(INCORRECT_ANSWER_2)).perform(click());
        waitForView(withId(R.id.mquiz_next_btn)).perform(click());

        String actual = TestUtils.getCurrentActivity().getString(R.string.widget_quiz_results_score, (float) 0);
        waitForView(withId(R.id.quiz_results_score))
                .check(matches(withText(actual)));

        // TODO check that the feedback is *not* displayed

        waitForView(withId(R.id.quiz_exit_button))
                .check(matches(withText(R.string.widget_quiz_continue)));
    }

}
