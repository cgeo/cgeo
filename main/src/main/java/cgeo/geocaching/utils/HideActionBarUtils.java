package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

public class HideActionBarUtils {

    private HideActionBarUtils() {
        // utility class
    }

    public static boolean toggleActionBar(@NonNull final AbstractNavigationBarActivity activity) {
        if (!Settings.getMapActionbarAutohide()) {
            return true;
        }
        final boolean actionBarShowing = toggleActionBarHelper(activity);
        final View spacer = activity.findViewById(R.id.actionBarSpacer);
        ViewUtils.applyToView(activity.findViewById(R.id.filterbar), view -> view.setVisibility(actionBarShowing ? View.VISIBLE : View.GONE));
        ViewUtils.applyToView(activity.findViewById(R.id.distanceinfo), view -> view.animate().translationY(actionBarShowing ? 0 : -spacer.getHeight()).start());
        ViewUtils.applyToView(activity.findViewById(R.id.map_progressbar), view -> view.animate().translationY(actionBarShowing ? 0 : -spacer.getHeight()).start());
        return actionBarShowing;
    }

    private static boolean toggleActionBarHelper(@NonNull final AbstractNavigationBarActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) {
            return false;
        }
        final View navigation = activity.findViewById(R.id.activity_navigationBar);
        if (actionBar.isShowing()) {
            actionBar.hide();
            navigation.setVisibility(View.GONE);
            return false;
        } else {
            actionBar.show();
            navigation.setVisibility(View.VISIBLE);
            return true;
        }
    }

}
