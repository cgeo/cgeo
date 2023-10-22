package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Mapping of routing modes and {@link BRouterServiceConnection} implementation dependent parameter values.
 */
public enum RoutingMode {
    OFF("off", R.string.switch_off, R.id.routing_off, R.drawable.routing_off),
    STRAIGHT("straight", R.string.routingmode_straight, R.id.routing_straight, R.drawable.routing_straight),
    WALK("foot", R.string.routingmode_walk, R.id.routing_walk, R.drawable.routing_walk),
    BIKE("bicycle", R.string.routingmode_bike, R.id.routing_bike, R.drawable.routing_bike),
    CAR("motorcar", R.string.routingmode_car, R.id.routing_car, R.drawable.routing_car),
    USER1("user1", R.string.routingmode_user1, R.id.routing_user1, R.drawable.number_one),
    USER2("user2", R.string.routingmode_user2, R.id.routing_user2, R.drawable.number_two);

    @NonNull
    public final String parameterValue;
    public final int infoResId;
    @IdRes public final int buttonResId;
    @DrawableRes public final int drawableId;

    RoutingMode(@NonNull final String parameterValue, @StringRes final int infoResId, final int buttonResId, @DrawableRes final int drawableId) {
        this.parameterValue = parameterValue;
        this.infoResId = infoResId;
        this.buttonResId = buttonResId;
        this.drawableId = drawableId;
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
