package cgeo.geocaching.maps.interfaces;

import android.widget.TextView;

import androidx.annotation.NonNull;

public interface MapSource {
    String getName();

    boolean isAvailable();

    int getNumericalId();

    @NonNull
    MapProvider getMapProvider();

    void setMapAttributionTo(TextView textView);

    default void releaseResources() {
        //do nothing
    }

}
