package cgeo.geocaching.settings;

import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

abstract class AbstractClickablePreference extends Preference {

    final SettingsActivity activity;

    public AbstractClickablePreference(final SettingsActivity activity, final AttributeSet attrs) {
        super(activity, attrs);
        this.activity = activity;
    }

    public AbstractClickablePreference(final SettingsActivity activity, final AttributeSet attrs, final int defStyle) {
        super(activity, attrs, defStyle);
        this.activity = activity;
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        setOnPreferenceClickListener(getOnPreferenceClickListener(activity));
        return super.onCreateView(parent);
    }

    abstract protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity);
}
