package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.Geopoint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.List;

/**
 * Immutable, rendering-only snapshot of a single Wherigo zone, handed over by the {@code :wherigo}
 * module through {@link WherigoZoneProvider}. Keeping this a plain data holder (no OpenWIG types)
 * is what lets {@link WherigoLayer} live in the base app without depending on the feature module.
 */
public final class WherigoZoneInfo {

    @NonNull public final String name;
    @NonNull public final List<Geopoint> points;
    @ColorInt public final int color;

    public WherigoZoneInfo(@NonNull final String name, @NonNull final List<Geopoint> points, @ColorInt final int color) {
        this.name = name;
        this.points = points;
        this.color = color;
    }
}
