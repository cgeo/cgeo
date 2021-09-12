package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloadSelectorActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

/**
 * Preference which shows a dialog containing textual explanation.
 * Similar to InfoPreference, but with map icon, positive button to forward, negative button to cancel dialog
 *
 */
public class InfoPreferenceMap extends AbstractInfoPreference {

    private final Activity activity;

    public InfoPreferenceMap(final Context context) {
        this(context, null);
    }

    public InfoPreferenceMap(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public InfoPreferenceMap(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        activity = (Activity) context;
        init(activity, R.layout.preference_map_icon, this::startActivity);
    }

    private void startActivity() {
        activity.startActivity(new Intent(activity, DownloadSelectorActivity.class));
    }
}
