package cgeo.geocaching.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * This is just a dummy preference, to be able check for the type.
 * <p>
 * Use it exactly as an EditTextPreference
 *
 * @see NewSettingsActivity - search for EditPasswordPreference
 * @author koem
 */
public class EditPasswordPreference extends EditTextPreference {

    public EditPasswordPreference(Context context) {
        super(context);
    }

    public EditPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPasswordPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
