package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.commons.lang3.StringUtils;

public class ActionBarUtils {

    private ActionBarUtils() {
        // utility class
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static void setContentView(@NonNull final AbstractNavigationBarActivity activity, final View contentView, final boolean showSpacer) {
        activity.setContentView(contentView);
        showActionBarSpacer(activity, showSpacer);
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static void setContentView(@NonNull final AbstractNavigationBarActivity activity, @LayoutRes final int layoutResID, final boolean showSpacer) {
        activity.setContentView(layoutResID);
        showActionBarSpacer(activity, showSpacer);
    }

    public static boolean toggleActionBar(@NonNull final AbstractActionBarActivity activity) {
        if (!Settings.getMapActionbarAutohide()) {
            return true;
        }
        final View actionBar = activity.getActionBarView();
        if (actionBar == null) {
            return true;
        }
        final boolean isShown = activity.actionBarIsShowing();

        if (!isShown) {
            activity.showActionBar();
        } else {
            activity.hideActionBar();
        }

        // adjust system bars appearance, depending on action bar color and visibility
        ActionBarUtils.setSystemBarAppearance(activity, !isShown);

        final View spacer = activity.findViewById(R.id.actionBarSpacer);
        final int height = !isShown ? 0 : -spacer.getHeight();
        ViewUtils.applyToView(activity.findViewById(R.id.filterbar), view -> view.animate().translationY(height).start());
        ViewUtils.applyToView(activity.findViewById(R.id.distanceinfo), view -> view.animate().translationY(height).start());
        ViewUtils.applyToView(activity.findViewById(R.id.map_progressbar), view -> view.animate().translationY(height).start());

        return !isShown;
    }

    private static void showActionBarSpacer(@NonNull final Activity activity, final boolean showSpacer) {
        activity.findViewById(R.id.actionBarSpacer).setVisibility(showSpacer ? View.VISIBLE : View.GONE);
    }

    public static void setSystemBarAppearance(@NonNull final Activity activity, final boolean isActionBarShown) {
        final Window currentWindow = activity.getWindow();
        final WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(currentWindow, currentWindow.getDecorView());

        // set light/dark system bars depending on action bar colors
        final boolean isLightSkin = Settings.isLightSkin(activity);
        if (isLightSkin) {
            windowInsetsController.setAppearanceLightStatusBars(!isActionBarShown);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        } else {
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
        }
    }

    public static void setSubtitle(@NonNull final AbstractActionBarActivity activity, @NonNull final CharSequence subtitleText) {
        final ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar == null || StringUtils.isEmpty(subtitleText)) {
            return;
        }

        final SpannableString titleString = getSpannedTitle(subtitleText, R.color.colorTextHintActionBar);
        supportActionBar.setSubtitle(titleString);
    }

    public static void setTitle(@NonNull final AbstractActionBarActivity activity, @NonNull final CharSequence titleText) {
        final ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar == null || StringUtils.isEmpty(titleText)) {
            return;
        }

        final SpannableString titleString = getSpannedTitle(titleText, R.color.colorTextActionBar);
        supportActionBar.setTitle(titleString);
    }

    // @todo remove after switching map ActionBar to Toolbar
    // workaround for colored ActionBar titles/subtitles
    // Checking for an existing span of the given class
    private static SpannableString getSpannedTitle(final CharSequence spanText, final @ColorRes int colorRes) {
        // // If a Spanned is already present, check whether a ForegroundColorSpan covers the entire text
        if (TextUtils.hasSpanCoveringWholeText(spanText, ForegroundColorSpan.class)) {
            return new SpannableString(spanText);
        }

        // Create new span with actionbar text color
        final SpannableString titleString = new SpannableString(spanText);
        final int color = ColorUtils.colorFromResource(colorRes);
        titleString.setSpan(new ForegroundColorSpan(color), 0, titleString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return titleString;
    }
}
