package cgeo.geocaching.settings;

import cgeo.geocaching.location.IConversion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

public abstract class AbstractDistanceBeepPreference extends AbstractSeekbarPreference {
    private int maxSeekbarLength = 0;   // initialized in onCreateView
    private boolean first = true;

    public AbstractDistanceBeepPreference(final Context context, final AttributeSet attrs, final boolean first) {
        super(context, attrs);
        this.first = first;
    }

    private int valueToProgressHelper(final int value) {
        return (int) Math.round(25.0 * Math.log10(value + 1));
    }

    private int valueToProgress(final int value) {
        final int progress = valueToProgressHelper(value);
        return progress < 0 ? 0 : progress > maxSeekbarLength ? maxSeekbarLength : progress;
    }

    private int progressToValue(final int progress) {
        final int value = (int) Math.pow(10, Double.valueOf(progress) / 25.0) - 1;
        return value < 0 ? 0 : value > Settings.DISTANCE_BEEP_MAX ? Settings.DISTANCE_BEEP_MAX : value;
    }

    protected String getValueString(final int progress) {
        final int value = progressToValue(progress);
        return Settings.useImperialUnits()
            ? String.format(Locale.US, "%.2f mi", value / (1000 * IConversion.MILES_TO_KILOMETER))
            : value + " m"
        ;
    }

    protected void saveSetting(final int progress) {
        Settings.setDistanceBeepThreshold(first, progressToValue(progress));
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        maxSeekbarLength = valueToProgressHelper(Settings.DISTANCE_BEEP_MAX);
        configure(1, maxSeekbarLength, valueToProgress(Settings.getDistanceBeepThreshold(first)), first ? "1" : "2");
        return super.onCreateView(parent);
    }
}
