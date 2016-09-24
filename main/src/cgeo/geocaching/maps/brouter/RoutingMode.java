package cgeo.geocaching.maps.brouter;

import android.support.annotation.NonNull;

public enum RoutingMode {
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
