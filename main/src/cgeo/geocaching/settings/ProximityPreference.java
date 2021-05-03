package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.location.IConversion;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import java.util.Locale;

public class ProximityPreference extends SeekbarPreference {

    private boolean highRes = false;

    public ProximityPreference(final Context context) {
        this(context, null);
    }

    public ProximityPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public ProximityPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProximityPreference);
        highRes = a.getBoolean(R.styleable.ProximityPreference_highRes, false);
        a.recycle();
    }

    // init() gets called by super constructor, therefore before class constructor / class variable assignments!
    @Override
    protected void init() {
        minProgress = 1;
        maxProgress = valueToProgressHelper(Settings.getKeyInt(R.integer.proximitynotification_distance_max));
    }

    @Override
    protected int valueToProgressHelper(final int value) {
        return (int) Math.round(250.0 * Math.log10(value + 1));
    }

    @Override
    protected int progressToValue(final int progress) {
        final int value = (int) Math.pow(10, (double) progress / 250.0) - 1;
        return Math.max(value, 0);
    }

    @Override
    protected String valueToShownValue(final int value) {
        return Settings.useImperialUnits() ? String.format(Locale.US, "%.2f", value / (highRes ? IConversion.FEET_TO_METER : IConversion.MILES_TO_KILOMETER)) : String.valueOf(value);
    }

    @Override
    protected int shownValueToValue(final float shownValue) {
        return Math.round(Settings.useImperialUnits() ? shownValue * (highRes ? IConversion.FEET_TO_METER : IConversion.MILES_TO_KILOMETER) : shownValue);
    }

    @Override
    protected String getUnitString() {
        return Settings.useImperialUnits() ? (highRes ? " ft" : " mi") : (highRes ? " m" : " km");
    }

    @Override
    protected boolean useDecimals() {
        // unit settings can be changed while ProximityPreference is already initialized
        return Settings.useImperialUnits();
    }
}
