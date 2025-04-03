package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.content.res.TypedArray;

import java.util.Locale;

public class ProximityPreferenceUI extends SeekbarUI {
    private boolean highRes = false;

    public ProximityPreferenceUI(final Context context) {
        super(context);
    }

    // init() gets called by super constructor, therefore before class constructor / class variable assignments!
    @Override
    public void init() {
        super.init();
        minProgress = 1;
    }

    @Override
    protected int valueToProgressHelper(final int value) {
        return (int) Math.round(250.0 * Math.log10(value + 1));
    }

    @Override
    public int progressToValue(final int progress) {
        final int value = (int) Math.round(Math.pow(10, (double) progress / 250.0)) - 1;
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
    public boolean getHasDecimals() {
        // unit settings can be changed while ProximityPreference is already initialized
        return Settings.useImperialUnits();
    }

    @Override
    public void loadAdditionalAttributes(final Context context, final TypedArray attrs, final int defStyle) {
        highRes = attrs.getBoolean(R.styleable.SeekbarPreference_highRes, false);
    }
}
