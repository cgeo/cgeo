package cgeo.geocaching.maps.interfaces;

import androidx.annotation.NonNull;

public interface MapSource {
    String getName();

    boolean isAvailable();

    int getNumericalId();

    @NonNull
    MapProvider getMapProvider();
}
