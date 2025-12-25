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

package cgeo.geocaching.maps.routing

import cgeo.geocaching.R

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes

/**
 * Mapping of routing modes and {@link BRouterServiceConnection} implementation dependent parameter values.
 */
enum class class RoutingMode {
    OFF("off", R.string.switch_off, R.id.routing_off, R.drawable.routing_off),
    STRAIGHT("straight", R.string.routingmode_straight, R.id.routing_straight, R.drawable.routing_straight),
    WALK("foot", R.string.routingmode_walk, R.id.routing_walk, R.drawable.routing_walk),
    BIKE("bicycle", R.string.routingmode_bike, R.id.routing_bike, R.drawable.routing_bike),
    CAR("motorcar", R.string.routingmode_car, R.id.routing_car, R.drawable.routing_car),
    USER1("user1", R.string.routingmode_user1, R.id.routing_user1, R.drawable.number_one),
    USER2("user2", R.string.routingmode_user2, R.id.routing_user2, R.drawable.number_two)

    public final String parameterValue
    public final Int infoResId
    @IdRes public final Int buttonResId
    @DrawableRes public final Int drawableId

    RoutingMode(final String parameterValue, @StringRes final Int infoResId, final Int buttonResId, @DrawableRes final Int drawableId) {
        this.parameterValue = parameterValue
        this.infoResId = infoResId
        this.buttonResId = buttonResId
        this.drawableId = drawableId
    }

    public static RoutingMode fromString(final String input) {
        for (final RoutingMode mode : values()) {
            if (mode.parameterValue == (input)) {
                return mode
            }
        }
        return WALK
    }
}
