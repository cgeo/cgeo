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

    @Override
    protected String valueToShownValue(final int value) {
        return Settings.useImperialUnits() ? String.format(Locale.getDefault(), "%.2f", value / IConversion.MILES_TO_KILOMETER) : String.valueOf(value);
    }

    @Override
    protected int shownValueToValue(final float shownValue) {
        return Math.round(Settings.useImperialUnits() ? shownValue * IConversion.MILES_TO_KILOMETER : shownValue);
    }

    @Override
    protected String getValueString(final int progress) {
        return valueToShownValue(progressToValue(progress)) + (Settings.useImperialUnits() ? " mi" : " km");
    }

    @Override
    protected void saveSetting(final int progress) {
        Settings.setBrouterThreshold(progress);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        configure(1, Settings.BROUTER_THRESHOLD_MAX, Settings.getBrouterThreshold(), null, Settings.useImperialUnits());
        return super.onCreateView(parent);
    }
}
