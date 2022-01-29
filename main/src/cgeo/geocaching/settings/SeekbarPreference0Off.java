package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Displays a regular seekbar with 0="Off"
 * You can set android:defaultValue and app:max attributes.
 */
public class SeekbarPreference0Off extends SeekbarPreference {

    public SeekbarPreference0Off(final Context context) {
        this(context, null);
    }

    public SeekbarPreference0Off(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SeekbarPreference0Off(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        minProgress = 0;
        hasDecimals = false;
    }

    @Override
    protected String getValueString(final int progress) {
        if (progress == 0) {
            return context.getString(R.string.switch_off);
        }
        return super.getValueString(progress);
    }

}
