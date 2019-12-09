package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public interface IFilter extends Parcelable {

    @NonNull
    String getName();

    /**
     * @return {@code true} if the filter accepts the cache, false otherwise
     */
    boolean accepts(@NonNull Geocache cache);

    void filter(@NonNull List<Geocache> list);

}
