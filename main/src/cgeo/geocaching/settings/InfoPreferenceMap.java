package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Preference which shows a dialog containing textual explanation.
 * Similar to InfoPreference, but with map icon, positive button to forward, negative button to cancel dialog
 *
 */
public class InfoPreferenceMap extends AbstractInfoPreference {

    public InfoPreferenceMap(final Context context) {
        super(context);
        init(context, R.layout.preference_map_icon, true);
    }

    public InfoPreferenceMap(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, R.layout.preference_map_icon, true);
    }

    public InfoPreferenceMap(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, R.layout.preference_map_icon, true);
    }

}
