package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Preference which shows a dialog containing textual explanation. The dialog has two buttons, where one will open a
 * hyper link with more detailed information.
 * <p>
 * The URL for the hyper link and the text are given as custom attributes in the preference XML definition.
 * </p>
 *
 */
public class InfoPreference extends AbstractInfoPreference {

    public InfoPreference(final Context context) {
        super(context);
        init(context, R.layout.preference_info_icon, false);
    }

    public InfoPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, R.layout.preference_info_icon, false);
    }

    public InfoPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, R.layout.preference_info_icon, false);
    }

}
