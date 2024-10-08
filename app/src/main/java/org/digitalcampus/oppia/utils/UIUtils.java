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

package org.digitalcampus.oppia.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.activity.ScorecardActivity;
import org.digitalcampus.oppia.analytics.Analytics;
import org.digitalcampus.oppia.application.App;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.Lang;
import org.digitalcampus.oppia.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

public class UIUtils {

    public static final String TAG = UIUtils.class.getSimpleName();
    private static final String EXCEPTION = "Exception:";
    private static int pointsToSubstractForAnimationSaved;

    private UIUtils() {
        throw new IllegalStateException("Utility class");
    }
    private static final Map<String, String> iso6391ToIso6392Map = new HashMap<>();

    static {
        iso6391ToIso6392Map.put("de", "deu"); // German
        iso6391ToIso6392Map.put("fr", "fra"); // French
        iso6391ToIso6392Map.put("en", "eng"); // English
        iso6391ToIso6392Map.put("am", "amh"); // Amharic
        iso6391ToIso6392Map.put("om", "orm"); // Oromo
        iso6391ToIso6392Map.put("ti", "tir"); // Tigrinya
        iso6391ToIso6392Map.put("so", "som"); // Somali
        iso6391ToIso6392Map.put("aa", "aar"); // Afar
        iso6391ToIso6392Map.put("wal", "wal"); // Wolaytta
        iso6391ToIso6392Map.put("sid", "sid"); // Sidamo
        iso6391ToIso6392Map.put("gaz", "tig"); // Gazi
        iso6391ToIso6392Map.put("tig", "tig"); // Tigre
        iso6391ToIso6392Map.put("kam", "kan"); // Kambaata
        iso6391ToIso6392Map.put("hau", "hau"); // Hadiyya
        iso6391ToIso6392Map.put("gur", "gur"); // Gurage
        iso6391ToIso6392Map.put("gwi", "gwi"); // Gwich'in
        iso6391ToIso6392Map.put("sah", "sah"); // Saho
        iso6391ToIso6392Map.put("bla", "bla"); // Berta
        iso6391ToIso6392Map.put("meb", "meb"); // Me'en
        iso6391ToIso6392Map.put("gdo", "gdo"); // Gedeo
        iso6391ToIso6392Map.put("tsd", "tsd"); // Tsamai
        iso6391ToIso6392Map.put("tum", "tum"); // Tumtum
        iso6391ToIso6392Map.put("bta", "bta"); // Berta
    }

    // Method to map ISO 639-1 language codes to ISO 639-2 codes
    public static String mapISO6391toISO6392(String iso6391Code) {
        return iso6391ToIso6392Map.get(iso6391Code);
    }
    public static void showUserData(Menu menu, final Context ctx, final Course courseInContext) {
        showUserData(menu, ctx, courseInContext, false, -1);
    }

    public static void showUserData(Menu menu, final Context ctx, final Course courseInContext, boolean animateBgPoints) {
        showUserData(menu, ctx, courseInContext, animateBgPoints, -1);
    }

    public static void showUserData(Menu menu, final Context ctx, final Course courseInContext, boolean animateBgPoints, int pointsToSubstractForAnimation) {
        if (menu == null) {
            return;
        }

        if (pointsToSubstractForAnimation > -1) {
            pointsToSubstractForAnimationSaved = pointsToSubstractForAnimation;
        }

        Log.i(TAG, "showUserData: --> pointsToSubstractForAnimationSaved: " + pointsToSubstractForAnimationSaved);

        MenuItem pointsItem = menu.findItem(R.id.points);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        //Get User from AppModule with dagger
        App app = (App) ctx.getApplicationContext();
        User u = app.getComponent().getUser();

        Log.d(TAG, "Username: " + u.getUsername() + " | Points:" + u.getPoints());

        if (pointsItem == null) {
            return;
        }

        final TextView points = pointsItem.getActionView().findViewById(R.id.userpoints);

        if (points == null) {
            return;
        }

        if (animateBgPoints) {
            int colorFrom = ContextCompat.getColor(ctx, R.color.white);
            int colorTo = ContextCompat.getColor(ctx, R.color.points_badge);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(1000); // milliseconds
            colorAnimation.addUpdateListener(animator ->
                    points.getBackground().setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat((int) animator.getAnimatedValue(), BlendModeCompat.SRC_OVER)));
            colorAnimation.start();
        }


        boolean scoringEnabled = prefs.getBoolean(PrefsActivity.PREF_SCORING_ENABLED, true);
        if (scoringEnabled) {
            points.setVisibility(View.VISIBLE);
            points.setText(String.valueOf(u.getPoints() - pointsToSubstractForAnimationSaved));
            points.setOnClickListener(view -> {
                Intent i = new Intent(ctx, ScorecardActivity.class);
                Bundle tb = new Bundle();
                tb.putString(ScorecardActivity.TAB_TARGET, ScorecardActivity.TAB_TARGET_POINTS);
                if (courseInContext != null) {
                    tb.putSerializable(Course.TAG, courseInContext);
                }
                i.putExtras(tb);
                ctx.startActivity(i);
            });
        } else {
            points.setVisibility(View.GONE);
        }

    }

    /**
     * @param ctx
     * @param title
     * @param msg
     * @return
     */
    public static AlertDialog showAlert(Context ctx, int title, int msg) {
        return UIUtils.showAlert(ctx, ctx.getString(title), ctx.getString(msg), ctx.getString(R.string.close));
    }

    /**
     * @param ctx
     * @param title
     * @param msg
     * @return
     */
    public static AlertDialog showAlert(Context ctx, int title, int msg, int btnText) {
        return UIUtils.showAlert(ctx, ctx.getString(title), ctx.getString(msg), ctx.getString(btnText));
    }

    /**
     * @param ctx
     * @param res
     * @param msg
     * @return
     */
    public static AlertDialog showAlert(Context ctx, int res, String msg) {
        return UIUtils.showAlert(ctx, ctx.getString(res), msg, ctx.getString(R.string.close));
    }

    public static AlertDialog showAlert(Context ctx, String title, String msg) {
        return UIUtils.showAlert(ctx, title, msg, ctx.getString(R.string.close));
    }

    /**
     * @param ctx
     * @param title
     * @param msg
     * @return
     */
    public static AlertDialog showAlert(Context ctx, String title, String msg, String btnText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(btnText, (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    /**
     * @param ctx
     * @param title
     * @param msg
     * @param funct
     * @return
     */
    public static void showAlert(Context ctx, int title, int msg, Callable<Boolean> funct) {
        UIUtils.showAlert(ctx, ctx.getString(title), ctx.getString(msg), funct);
    }

    public static void showAlert(Context ctx, int title, int msg, int btnText, Callable<Boolean> funct) {
        UIUtils.showAlert(ctx, ctx.getString(title), ctx.getString(msg), ctx.getString(btnText), funct);
    }

    /**
     * @param ctx
     * @param res
     * @param msg
     * @param funct
     * @return
     */
    public static void showAlert(Context ctx, int res, CharSequence msg, Callable<Boolean> funct) {
        UIUtils.showAlert(ctx, ctx.getString(res), msg, funct);
    }

    public static void showAlert(Context ctx, String title, CharSequence msg, final Callable<Boolean> funct) {
        UIUtils.showAlert(ctx, title, msg, ctx.getString(R.string.close), funct);
    }

    /**
     * @param ctx
     * @param title
     * @param msg
     * @param funct
     * @return
     */
    public static void showAlert(Context ctx, String title, CharSequence msg, String btnText, final Callable<Boolean> funct) {
        if (ctx instanceof Activity) {
            Activity activity = (Activity) ctx;
            if (activity.isFinishing()) {
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setNeutralButton(btnText, (dialog, id) -> dialog.cancel());
        builder.setOnCancelListener(dialog -> {
            try {
                funct.call();
            } catch (Exception e) {
                Analytics.logException(e);
                Log.d(TAG, EXCEPTION, e);
            }

        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    /**
     * @param ctx
     * @param langs
     * @param prefs
     * @param funct
     */
    public static void createLanguageDialog(Context ctx, List<Lang> langs, final SharedPreferences prefs, final Callable<Boolean> funct) {
        ArrayList<String> langStringList = new ArrayList<>();
        final ArrayList<Lang> languagesList = new ArrayList<>();

        // make sure there aren't any duplicates
        for (Lang lang : langs) {
            boolean found = false;
            for (Lang ln : languagesList) {
                if (ln.getLanguage().equalsIgnoreCase(lang.getLanguage())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                languagesList.add(lang);
            }
        }

        int prefLangPosition = -1;
        int i = 0;

        String prefLanguage = prefs.getString(PrefsActivity.PREF_CONTENT_LANGUAGE, Locale.getDefault().getLanguage());
        for (Lang lang : languagesList) {
            Locale locale = new Locale(lang.getLanguage());
            String langDisp = locale.getDisplayLanguage(locale);
            langStringList.add(langDisp);
            if (lang.getLanguage().equalsIgnoreCase(prefLanguage)) {
                prefLangPosition = i;
            }
            i++;
        }

        // only show if at least one language
        if (i > 0) {
            ArrayAdapter<String> arr = new ArrayAdapter<>(ctx, android.R.layout.select_dialog_singlechoice, langStringList);
            AlertDialog mAlertDialog = new AlertDialog.Builder(ctx)
                    .setSingleChoiceItems(arr, prefLangPosition, (dialog, whichButton) -> {
                        String newLang = languagesList.get(whichButton).getLanguage();
                        Editor editor = prefs.edit();
                        editor.putString(PrefsActivity.PREF_CONTENT_LANGUAGE, newLang);
                        editor.apply();
                        dialog.dismiss();
                        try {
                            funct.call();
                        } catch (Exception e) {
                            Analytics.logException(e);
                            Log.d(TAG, EXCEPTION, e);
                        }
                    }).setTitle(ctx.getString(R.string.change_content_language))
                    .setNegativeButton(ctx.getString(R.string.cancel), (dialog, which) -> {
                        // do nothing
                    }).create();
            mAlertDialog.show();
        }
    }

    public static String getPreferredLanguage(SharedPreferences prefs) {
       return prefs.getString(PrefsActivity.PREF_CONTENT_LANGUAGE, Locale.getDefault().getDisplayLanguage());

    }
    public static void hideSoftKeyboard(Activity activity) {
        hideSoftKeyboard(activity.getWindow());
    }
    public static void createInterfaceLanguageDialog(
            Context ctx,
            Map<String, String> languagesList,
            final SharedPreferences prefs,
            final Callable<Boolean> funct
    ) {
        ArrayList<String> langStringList = new ArrayList<>(languagesList.keySet());

        int prefLangPosition = -1;
        int i = 0;

        String prefLanguage = prefs.getString(PrefsActivity.PREF_INTERFACE_LANGUAGE, Locale.getDefault().getLanguage());

        // Find the preferred language position in the list
        for (String language : langStringList) {
            if (languagesList.get(language).equalsIgnoreCase(prefLanguage)) {
                prefLangPosition = i;
                break; // Exit the loop once the preferred language is found
            }
            i++;
        }

        // Ensure there's at least one language to display
        if (langStringList.size() > 0) {
            // Create a single-choice dialog for selecting the language
            ArrayAdapter<String> arr = new ArrayAdapter<>(
                    ctx,
                    android.R.layout.select_dialog_singlechoice,
                    langStringList
            );

            AlertDialog mAlertDialog = new AlertDialog.Builder(ctx)
                    .setSingleChoiceItems(arr, prefLangPosition, (dialog, whichButton) -> {
                        String selectedLang = langStringList.get(whichButton);
                        String newLang = languagesList.get(selectedLang); // Get the language code

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(PrefsActivity.PREF_INTERFACE_LANGUAGE, newLang);
                        editor.apply();
                        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags((String) newLang);
                        // Call this on the main thread as it may require Activity.restart()
                        AppCompatDelegate.setApplicationLocales(appLocale);

                        dialog.dismiss(); // Close the dialog after selection

                        try {
                            funct.call(); // Invoke the callback function
                        } catch (Exception e) {
                            // Handle exception
                            Log.e("LanguageInterfaceDialog", "Error executing callback", e);
                        }
                    })
                    .setTitle(ctx.getString(R.string.change_interface_language))
                    .setNegativeButton(ctx.getString(R.string.cancel), (dialog, which) -> {
                        // User cancelled; do nothing
                    })
                    .create();

            // Show the dialog
            mAlertDialog.show();
        }
    }
    public static void hideSoftKeyboard(Dialog dialog) {
        hideSoftKeyboard(dialog.getWindow());
    }

    public static void hideSoftKeyboard(Window window) {

        View view = window.getCurrentFocus();
        hideSoftKeyboard(view);
    }

    public static void hideSoftKeyboard(View view) {

        try {
            InputMethodManager imm =
                    (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) { }
    }

    private static final int BULLET_SPAN_GAP_WIDTH = 20;

    public static CharSequence getFromHtmlAndTrim(String text) {
        SpannableStringBuilder html = new SpannableStringBuilder(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT));
        BulletSpan[] spans = html.getSpans(0, html.length(), BulletSpan.class);
        for (BulletSpan span : spans) {
            int spanStart = html.getSpanStart(span);
            int spanEnd = html.getSpanEnd(span);
            html.removeSpan(span);
            html.setSpan(new BulletSpan(BULLET_SPAN_GAP_WIDTH) {
                @Override
                public int getLeadingMargin(boolean first) {
                    return BULLET_SPAN_GAP_WIDTH * 3;
                }

                @Override
                public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
                    if (first) {
                        final float yPosition = ((top + bottom) / 1.9f);
                        final float xPosition = x + dir * BULLET_SPAN_GAP_WIDTH;
                        canvas.drawCircle(xPosition, yPosition, 8, paint);
                    }
                }
            }, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int start = 0;
        int end = html.length();

        while (start < end && Character.isWhitespace(html.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(html.charAt(end - 1))) {
            end--;
        }
        return html.subSequence(start, end);
    }

    public static void showChangeTextSizeDialog(Context context, final Runnable callback) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String[] textSizeValues = context.getResources().getStringArray(R.array.TextSizeValues);
        String selectedFontSize = prefs.getString(PrefsActivity.PREF_TEXT_SIZE, "16");
        int checkedPosition = 0;
        for (int i = 0; i < textSizeValues.length; i++) {
            String textSizeValue = textSizeValues[i];
            if (TextUtilsJava.equals(textSizeValue, selectedFontSize)) {
                checkedPosition = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setSingleChoiceItems(R.array.TextSize, checkedPosition, (dialog, whichButton) -> {

                    String newTextSize = textSizeValues[whichButton];
                    prefs.edit().putString(PrefsActivity.PREF_TEXT_SIZE, newTextSize).commit();

                    try {
                        if (callback != null) {
                            callback.run();
                        }
                    } catch (Exception e) {
                        Analytics.logException(e);
                        Log.d(TAG, EXCEPTION, e);
                    }

                    dialog.dismiss();

                }).setTitle(context.getString(R.string.menu_change_text_size))
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();

    }
}
