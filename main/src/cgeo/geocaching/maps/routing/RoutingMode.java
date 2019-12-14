package cgeo.geocaching.maps.routing;

import androidx.annotation.NonNull;

/**
 * Mapping of routing modes and {@link BRouterServiceConnection} implementation dependent parameter values.
 */
public enum RoutingMode {
    STRAIGHT("straight"),
    WALK("foot"),
    BIKE("bicycle"),
    CAR("motorcar");

    @NonNull
    public final String parameterValue;

    RoutingMode(@NonNull final String parameterValue) {
        this.parameterValue = parameterValue;
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
