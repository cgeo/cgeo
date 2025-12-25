// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.location.IConversion
import cgeo.geocaching.settings.Settings

import android.content.Context
import android.content.res.TypedArray

import java.util.Locale

class ProximityPreferenceUI : SeekbarUI() {
    private var highRes: Boolean = false

    public ProximityPreferenceUI(final Context context) {
        super(context)
    }

    // init() gets called by super constructor, therefore before class constructor / class variable assignments!
    override     public Unit init() {
        super.init()
        minProgress = 1
    }

    override     protected Int valueToProgressHelper(final Int value) {
        return (Int) Math.round(250.0 * Math.log10(value + 1))
    }

    override     public Int progressToValue(final Int progress) {
        val value: Int = (Int) Math.round(Math.pow(10, (Double) progress / 250.0)) - 1
        return Math.max(value, 0)
    }

    override     protected String valueToShownValue(final Int value) {
        return Settings.useImperialUnits() ? String.format(Locale.US, "%.2f", value / (highRes ? IConversion.FEET_TO_METER : IConversion.MILES_TO_KILOMETER)) : String.valueOf(value)
    }

    override     protected Int shownValueToValue(final Float shownValue) {
        return Math.round(Settings.useImperialUnits() ? shownValue * (highRes ? IConversion.FEET_TO_METER : IConversion.MILES_TO_KILOMETER) : shownValue)
    }

    override     protected String getUnitString() {
        return Settings.useImperialUnits() ? (highRes ? " ft" : " mi") : (highRes ? " m" : " km")
    }

    override     public Boolean getHasDecimals() {
        // unit settings can be changed while ProximityPreference is already initialized
        return Settings.useImperialUnits()
    }

    override     public Unit loadAdditionalAttributes(final Context context, final TypedArray attrs, final Int defStyle) {
        highRes = attrs.getBoolean(R.styleable.SeekbarPreference_highRes, false)
    }
}
