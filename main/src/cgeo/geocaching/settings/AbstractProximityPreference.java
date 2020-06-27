package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.location.IConversion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

public abstract class AbstractProximityPreference extends AbstractSeekbarPreference {
    private int maxSeekbarLength = 0;   // initialized in onCreateView
    private boolean farDistance = true;

    public AbstractProximityPreference(final Context context, final AttributeSet attrs, final boolean farDistance) {
        super(context, attrs);
        this.farDistance = farDistance;
    }

    @Override
    protected int valueToProgressHelper(final int value) {
        return (int) Math.round(250.0 * Math.log10(value + 1));
    }

    @Override
    protected int valueToProgress(final int value) {
        final int progress = valueToProgressHelper(value);
        return progress < 0 ? 0 : Math.min(progress, maxSeekbarLength);
    }

    @Override
    protected int progressToValue(final int progress) {
        final int value = (int) Math.pow(10, Double.valueOf(progress) / 250.0) - 1;
        return value < 0 ? 0 : Math.min(value, Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE);
    }

    @Override
    protected String valueToShownValue(final int value) {
        return Settings.useImperialUnits() ? String.format(Locale.getDefault(), "%.2f", value / (1000 * IConversion.MILES_TO_KILOMETER)) : String.valueOf(value);
    }

    @Override
    protected int shownValueToValue(final float shownValue) {
        return Math.round(Settings.useImperialUnits() ? shownValue * 1000 * IConversion.MILES_TO_KILOMETER : shownValue);
    }

    @Override
    protected String getValueString(final int progress) {
        return valueToShownValue(progressToValue(progress)) + (Settings.useImperialUnits() ? " mi" : " m");
    }

    @Override
    protected void saveSetting(final int progress) {
        Settings.setProximityNotificationThreshold(farDistance, progressToValue(progress));
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        maxSeekbarLength = valueToProgressHelper(Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE);
        configure(1, maxSeekbarLength, valueToProgress(Settings.getProximityNotificationThreshold(farDistance)), farDistance ? getContext().getString(R.string.far_distance) : getContext().getString(R.string.near_distance), Settings.useImperialUnits());
        return super.onCreateView(parent);
    }
}
