package cgeo.geocaching.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

abstract class AbstractClickablePreference extends Preference {

    final SettingsActivity activity;

    public AbstractClickablePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        activity = (SettingsActivity) context;
    }

    public AbstractClickablePreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        activity = (SettingsActivity) context;
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        setOnPreferenceClickListener(getOnPreferenceClickListener(activity));
        return super.onCreateView(parent);
    }

    abstract protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity);
}
