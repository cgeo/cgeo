package cgeo.geocaching.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class WpThresholdPreference extends AbstractSeekbarPreference {

    public WpThresholdPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    protected void saveSetting(final int progress) {
        Settings.setShowWaypointsThreshold(progress);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        configure(0, Settings.SHOW_WP_THRESHOLD_MAX, Settings.getWayPointsThreshold(), null);
        return super.onCreateView(parent);
    }
}
