package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Mapping of routing modes and {@link BRouterServiceConnection} implementation dependent parameter values.
 */
public enum RoutingMode {
    OFF("off", R.string.switch_off, R.id.routing_off),
    STRAIGHT("straight", R.string.routingmode_straight, R.id.routing_straight),
    WALK("foot", R.string.routingmode_walk, R.id.routing_walk),
    BIKE("bicycle", R.string.routingmode_bike, R.id.routing_bike),
    CAR("motorcar", R.string.routingmode_car, R.id.routing_car);

    @NonNull
    public final String parameterValue;
    public final int infoResId;
    public final int buttonResId;

    RoutingMode(@NonNull final String parameterValue, @StringRes final int infoResId, final int buttonResId) {
        this.parameterValue = parameterValue;
        this.infoResId = infoResId;
        this.buttonResId = buttonResId;
    }

    public static RoutingMode fromString(@NonNull final String input) {
        for (final RoutingMode mode : values()) {
            if (mode.parameterValue.equals(input)) {
                return mode;
            }
        }
        return WALK;
    }
}
