package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.location.Geopoint;

import android.content.Context;

public interface ILayer {
    void init(IProviderGeoItemLayer<?> provider);

    void destroy();

    default boolean handleTap(Context context, Geopoint tapped) {
        return false;
    }
}
