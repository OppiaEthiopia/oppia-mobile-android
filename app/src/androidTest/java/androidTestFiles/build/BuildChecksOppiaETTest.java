package androidTestFiles.build;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.digitalcampus.mobile.learning.BuildConfig;
import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.application.App;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BuildChecksOppiaETTest {

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        //We load the default prefs in case they were not loaded
        App.loadDefaultPreferenceValues(context, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Test
    public void checkDefaultSettingsParameters() {

        String oppiaServerDefault = context.getString(R.string.prefServerDefault);
        String oppiaServerHost = context.getString(R.string.oppiaServerHost);

        assertEquals("https://training-ee.moh.gov.et/", oppiaServerDefault);
        assertEquals("training-ee.moh.gov.et", oppiaServerHost);

        assertEquals(false, BuildConfig.ADMIN_PROTECT_SETTINGS);
        assertEquals(true, BuildConfig.ADMIN_PROTECT_ADVANCED_SETTINGS);
        assertEquals(true, BuildConfig.ADMIN_PROTECT_SECURITY_SETTINGS);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_SERVER);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_ACTIVITY_SYNC);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_ACTIVITY_EXPORT);
        assertEquals(true, BuildConfig.ADMIN_PROTECT_COURSE_DELETE);
        assertEquals(true, BuildConfig.ADMIN_PROTECT_COURSE_RESET);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_COURSE_INSTALL);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_COURSE_UPDATE);

        assertEquals(true, BuildConfig.ADMIN_PROTECT_NOTIFICATIONS);
        assertEquals(true, BuildConfig.ADMIN_PROTECT_ENABLE_REMINDER_NOTIFICATIONS);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_REMINDER_INTERVAL);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_REMINDER_DAYS);
        assertEquals(false, BuildConfig.ADMIN_PROTECT_REMINDER_TIME);

        assertEquals(true, BuildConfig.MENU_ALLOW_MONITOR);
        assertEquals(true, BuildConfig.MENU_ALLOW_SETTINGS);
        assertEquals(true, BuildConfig.MENU_ALLOW_COURSE_DOWNLOAD);
        assertEquals(true, BuildConfig.MENU_ALLOW_SYNC);
        assertEquals(true, BuildConfig.MENU_ALLOW_LOGOUT);
        assertEquals(false, BuildConfig.MENU_ALLOW_EDIT_PROFILE);
        assertEquals(true, BuildConfig.MENU_ALLOW_CHANGE_PASSWORD);
        assertEquals(true, BuildConfig.MENU_ALLOW_LANGUAGE);

        assertEquals(1, BuildConfig.DOWNLOAD_COURSES_DISPLAY);

        assertEquals(true, BuildConfig.START_COURSEINDEX_COLLAPSED);

        assertEquals(true, BuildConfig.METADATA_INCLUDE_NETWORK);
        assertEquals(true, BuildConfig.METADATA_INCLUDE_APP_INSTANCE_ID);
        assertEquals(true, BuildConfig.METADATA_INCLUDE_MANUFACTURER_MODEL);
        assertEquals(true, BuildConfig.METADATA_INCLUDE_WIFI_ON);
        assertEquals(true, BuildConfig.METADATA_INCLUDE_NETWORK_CONNECTED);
        assertEquals(true, BuildConfig.METADATA_INCLUDE_BATTERY_LEVEL);

        assertEquals(true, BuildConfig.OFFLINE_REGISTER_ENABLED);
        assertEquals(false, BuildConfig.DELETE_ACCOUNT_ENABLED);
        assertEquals(false, BuildConfig.SESSION_EXPIRATION_ENABLED);
        assertEquals(600, BuildConfig.SESSION_EXPIRATION_TIMEOUT);

        assertEquals(false, BuildConfig.SHOW_COURSE_DESCRIPTION);
        assertEquals(false, BuildConfig.ALLOW_REGISTER_USER);
        assertEquals(true, BuildConfig.SHOW_GAMIFICATION_EVENTS);

        assertEquals("threshold", BuildConfig.GAMIFICATION_MEDIA_CRITERIA);
        assertEquals(100, BuildConfig.GAMIFICATION_DEFAULT_MEDIA_THRESHOLD);
        assertEquals(true, BuildConfig.GAMIFICATION_MEDIA_SHOULD_REACH_END);

        assertEquals("TIME_SPENT", BuildConfig.PAGE_COMPLETED_METHOD);
        assertEquals(10, BuildConfig.PAGE_COMPLETED_TIME_SPENT);
        assertEquals(125, BuildConfig.PAGE_COMPLETED_WPM);

        assertEquals(true, BuildConfig.SHOW_GAMIFICATION_EVENTS);
        assertEquals("2", BuildConfig.GAMIFICATION_POINTS_ANIMATION);
        assertEquals(2, BuildConfig.DURATION_GAMIFICATION_POINTS_VIEW);

        assertEquals("DAILY", BuildConfig.DEFAULT_REMINDER_INTERVAL);
        assertEquals("09:00", BuildConfig.DEFAULT_REMINDER_TIME);
        assertEquals("2,3,4,5,6", BuildConfig.DEFAULT_REMINDER_DAYS);

        assertEquals("NONE", context.getString(R.string.prefUpdateActivityOnLoginDefault));
    }
}
