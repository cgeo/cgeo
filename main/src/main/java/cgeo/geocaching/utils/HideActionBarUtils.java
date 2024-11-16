package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class HideActionBarUtils {

    private HideActionBarUtils() {
        // utility class
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static void setContentView(@NonNull final AbstractNavigationBarActivity activity, final View contentView, final boolean showSpacer) {
        setStableLayout(activity, showSpacer);
        activity.setContentView(contentView);
        showActionBarSpacer(activity, showSpacer);
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static void setContentView(@NonNull final AbstractNavigationBarActivity activity, @LayoutRes final int layoutResID, final boolean showSpacer) {
        setStableLayout(activity, showSpacer);
        activity.setContentView(layoutResID);
        showActionBarSpacer(activity, showSpacer);
    }

    public static boolean toggleActionBar(@NonNull final AbstractNavigationBarActivity activity) {
        if (!Settings.getMapActionbarAutohide()) {
            return true;
        }
        final boolean actionBarShowing = toggleActionBarHelper(activity);
        final View spacer = activity.findViewById(R.id.actionBarSpacer);
        ViewUtils.applyToView(activity.findViewById(R.id.filterbar), view -> view.animate().translationY(actionBarShowing ? 0 : -spacer.getHeight()).start());
        ViewUtils.applyToView(activity.findViewById(R.id.distanceinfo), view -> view.animate().translationY(actionBarShowing ? 0 : -spacer.getHeight()).start());
        ViewUtils.applyToView(activity.findViewById(R.id.map_progressbar), view -> view.animate().translationY(actionBarShowing ? 0 : -spacer.getHeight()).start());
        return actionBarShowing;
    }

    private static boolean toggleActionBarHelper(@NonNull final AbstractNavigationBarActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) {
            return false;
        }
        if (actionBar.isShowing()) {
            actionBar.hide();
            return false;
        } else {
            actionBar.show();
            return true;
        }
    }

    private static void showActionBarSpacer(@NonNull final Activity activity, final boolean showSpacer) {
        if (Settings.getMapActionbarAutohide()) {
            activity.findViewById(R.id.actionBarSpacer).setVisibility(showSpacer ? View.VISIBLE : View.GONE);
        }
    }

    private static void setStableLayout(@NonNull final AbstractNavigationBarActivity activity, final boolean showSpacer) {
        if (showSpacer && Settings.getMapActionbarAutohide()) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public static void adaptLayoutForActionBarHelper(final AppCompatActivity activity, @Nullable final Boolean actionBarShowing, @Nullable final View compassRose) {
        if (compassRose == null) {
            return;
        }

        int minHeight = 0;

        Boolean abs = actionBarShowing;
        if (actionBarShowing == null) {
            final ActionBar actionBar = activity.getSupportActionBar();
            abs = actionBar != null && actionBar.isShowing();
        }
        if (abs) {
            minHeight = activity.findViewById(R.id.actionBarSpacer).getHeight();
        }

        final View filterbar = activity.findViewById(R.id.filter_bar);
        if (filterbar != null) {
            minHeight += filterbar.getHeight();
        }

        View v = activity.findViewById(R.id.distanceinfo);
        if (v.getVisibility() != View.VISIBLE) {
            v = activity.findViewById(R.id.target);
        }
        if (v.getVisibility() == View.VISIBLE) {
            minHeight += v.getHeight();
        }

        final int finalMinHeight = minHeight;
        activity.runOnUiThread(() -> compassRose.animate().translationY(finalMinHeight).start());
    }
}
