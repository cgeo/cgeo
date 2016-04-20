package cgeo.geocaching.maps.interfaces;

import org.eclipse.jdt.annotation.NonNull;

public interface MapSource {
    String getName();

    boolean isAvailable();

    int getNumericalId();

    @NonNull
    MapProvider getMapProvider();
}
