package cgeo.geocaching.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IGeoObject {

    /**
     * @return Geocode like GCxxxx
     */
    @NonNull
    String getGeocode();

    @Nullable
    String getName();

}
