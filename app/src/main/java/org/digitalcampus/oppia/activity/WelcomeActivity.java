/*
 * This file is part of OppiaMobile - https://digital-campus.org/
 *
 * OppiaMobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OppiaMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digitalcampus.oppia.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.databinding.ActivityWelcomeBinding;
import org.digitalcampus.oppia.adapter.ActivityPagerAdapter;
import org.digitalcampus.oppia.analytics.AnalyticsProvider;
import org.digitalcampus.oppia.api.ApiEndpoint;
import org.digitalcampus.oppia.application.AdminSecurityManager;
import org.digitalcampus.oppia.application.SessionManager;
import org.digitalcampus.oppia.fragments.CoursesListFragment;
import org.digitalcampus.oppia.fragments.LoginFragment;
import org.digitalcampus.oppia.fragments.RegisterFragment;
import org.digitalcampus.oppia.fragments.RememberUsernameFragment;
import org.digitalcampus.oppia.fragments.ResetPasswordFragment;
import org.digitalcampus.oppia.fragments.WelcomeFragment;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.Lang;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.repository.InterfaceLanguagesRepository;
import org.digitalcampus.oppia.task.FetchUserTask;
import org.digitalcampus.oppia.utils.UIUtils;
import org.digitalcampus.oppia.utils.ui.DrawerMenuManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class WelcomeActivity extends AppActivity {

    public static final int TAB_WELCOME = 0;
    public static final int TAB_LOGIN = 1;
    public static final int TAB_REGISTER = 2;
    public static final int TAB_RESET_PASSWORD = 3;
    public static final int TAB_REMEMBER_USERNAME = 4;

    private static Map<Integer, Integer> menuOverflowIconColor = new HashMap<>();
    static {
        menuOverflowIconColor.put(TAB_WELCOME, R.color.text_dark);
        menuOverflowIconColor.put(TAB_LOGIN, R.color.toolbar_icon_color);
        menuOverflowIconColor.put(TAB_REGISTER, R.color.toolbar_icon_color);
        menuOverflowIconColor.put(TAB_RESET_PASSWORD, R.color.text_dark);
        menuOverflowIconColor.put(TAB_REMEMBER_USERNAME, R.color.text_dark);
    }
    public interface MenuOption {
        void onOptionSelected();
    }
    private int currentTab = TAB_WELCOME;
    private ActivityWelcomeBinding binding;

    @Inject
    AnalyticsProvider analyticsProvider;

    @Inject
    ApiEndpoint apiEndpoint;
    @Inject
    InterfaceLanguagesRepository interfaceLanguagesRepository;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        getAppComponent().inject(this);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        List<Fragment> fragments = new ArrayList<>();
        List<String> tabTitles = new ArrayList<>();

        Fragment fWelcome = WelcomeFragment.newInstance();
        fragments.add(fWelcome);
        tabTitles.add(this.getString(R.string.tab_title_welcome));

        Fragment fLogin = LoginFragment.newInstance();
        fragments.add(fLogin);
        tabTitles.add(this.getString(R.string.tab_title_login));

        Fragment fRegister = RegisterFragment.newInstance();
        fragments.add(fRegister);
        tabTitles.add(this.getString(R.string.tab_title_register));

        Fragment fResetPassword = ResetPasswordFragment.newInstance();
        fragments.add(fResetPassword);
        tabTitles.add(this.getString(R.string.tab_title_reset_password));

        Fragment fRememberUsername = RememberUsernameFragment.newInstance();
        fragments.add(fRememberUsername);
        tabTitles.add(this.getString(R.string.tab_title_remember_username));

        ActivityPagerAdapter apAdapter = new ActivityPagerAdapter(this, getSupportFragmentManager(), fragments, tabTitles);
        binding.activityWelcomePager.setAdapter(apAdapter);
        binding.activityWelcomePager.setCurrentItem(currentTab);

        setMenuOverflowIconColor(currentTab);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_welcome, menu);
        return true;
    }



    private Map<String, String> LangCodesWithNames() {
        String[] langs = interfaceLanguagesRepository.getLanguageOptions();
        List<String> langCodes = Arrays.asList(langs);
        if (langs.length > 0) {

            return langCodes.stream().collect(Collectors.toMap(
                    langCode -> new Locale(langCode).getDisplayLanguage(new Locale(langCode)),
                    langCode -> langCode));
        }
        return null;
    }
    private void showInterfaceLanguageSelectDialog(Map<String, String> languages) {
        if (languages == null || languages.isEmpty()) {
            return; // Return early if there are no languages to show
        }

        UIUtils.createInterfaceLanguageDialog(this, languages, prefs, () -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_main);
            if (fragment instanceof CoursesListFragment) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_main, new CoursesListFragment())
                        .commit();
            }
            return false;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_settings) {
            AdminSecurityManager.with(this).checkAdminPermission(itemId, () -> {
                Intent i = new Intent(this, PrefsActivity.class);
                startActivity(i);
            });
            return true;
        } else if (itemId == R.id.menu_about) {
            Intent iA = new Intent(this, AboutActivity.class);
            startActivity(iA);
            return true;
        } else if (itemId == R.id.menu_privacy) {
            Intent iA = new Intent(this, PrivacyActivity.class);
            startActivity(iA);
            return true;
        }
        else if (itemId == R.id.menu_interface_language_logout) { // Make sure this ID matches your menu item
            Map<String, String> interfaceLanguages = LangCodesWithNames();
            showInterfaceLanguageSelectDialog(interfaceLanguages); // Call the dialog function
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void switchTab(int tab) {
        binding.activityWelcomePager.setCurrentItem(tab);
        this.currentTab = tab;

        setMenuOverflowIconColor(tab);
    }

    private void setMenuOverflowIconColor(int tab) {
        // Change color of 3 dots icon to ensure contrast
        Toolbar toolbar = findViewById(R.id.toolbar);
        int colorInt = ContextCompat.getColor(this, menuOverflowIconColor.get(tab));
        toolbar.getOverflowIcon().setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void onBackPressed() {
        if (currentTab == TAB_WELCOME) {
            super.onBackPressed();
        } else {
            switchTab(TAB_WELCOME);
        }
    }

    public void onSuccessUserAccess(User user, boolean firstLogin) {

        boolean fromViewDigest = getIntent().getBooleanExtra(ViewDigestActivity.EXTRA_FROM_VIEW_DIGEST, false);

        SessionManager.loginUser(this, user);

        if (fromViewDigest) {
            setResult(Activity.RESULT_OK);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_FIRST_LOGIN, firstLogin);
            startActivity(intent);
        }

		if (analyticsProvider.shouldShowOptOutRationale(this)){
            overridePendingTransition(0, 0);
            startActivity(new Intent(this, AnalyticsOptinActivity.class));
        }

        new FetchUserTask().updateLoggedUserProfile(this, apiEndpoint, user);

        finish();
    }
}

