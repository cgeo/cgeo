package cgeo.geocaching.maps.interfaces;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.tuple.ImmutablePair;

public interface MapSource {
    String getName();

    boolean isAvailable();

    default String getId() {
        return this.getClass().getName();
    }

    int getNumericalId();

    @NonNull
    MapProvider getMapProvider();

    ImmutablePair<String, Boolean> calculateMapAttribution(Context ctx);

}
