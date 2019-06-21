package cgeo.geocaching.settings;

import cgeo.geocaching.location.IConversion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

public class BrouterThresholdPreference extends AbstractSeekbarPreference {

    public BrouterThresholdPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected String getValueString(final int progress) {
        return Settings.useImperialUnits()
            ? String.format(Locale.US, "%.1f mi", progress / IConversion.MILES_TO_KILOMETER)
            : progress + " km"
        ;
    }

    protected void saveSetting(final int progress) {
        Settings.setBrouterThreshold(progress);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        configure(1, Settings.BROUTER_THRESHOLD_MAX, Settings.getBrouterThreshold(), null);
        return super.onCreateView(parent);
    }
}
