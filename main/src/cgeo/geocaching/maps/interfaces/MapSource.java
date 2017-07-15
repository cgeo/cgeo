package cgeo.geocaching.maps.interfaces;

import android.support.annotation.NonNull;

public interface MapSource {
    String getName();

    boolean isAvailable();

    int getNumericalId();

    String getId();

    @NonNull
    MapProvider getMapProvider();
}
