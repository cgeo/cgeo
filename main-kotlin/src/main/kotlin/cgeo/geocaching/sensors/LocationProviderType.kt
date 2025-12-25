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

package cgeo.geocaching.sensors

import cgeo.geocaching.R

import androidx.annotation.StringRes

enum class class LocationProviderType {
    GPS(R.string.loc_gps),
    NETWORK(R.string.loc_net),
    FUSED(R.string.loc_fused),
    LOW_POWER(R.string.loc_low_power),
    HOME(R.string.loc_home),
    LAST(R.string.loc_last)

    @StringRes
    public final Int resourceId

    LocationProviderType(@StringRes final Int resourceId) {
        this.resourceId = resourceId
    }
}
