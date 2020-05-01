package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class CountryFilterFactory implements IFilterFactory {


    static class CountryFilter extends AbstractFilter {
        private String location;

        public static final Creator<CountryFilter> CREATOR
                = new Creator<CountryFilter>() {

            @Override
            public CountryFilter createFromParcel(final Parcel in) {
                return new CountryFilter(in);
            }

            @Override
            public CountryFilter[] newArray(final int size) {
                return new CountryFilter[size];
            }
        };

        CountryFilter(final String location, final String title) {
            super(title);
            this.location = location;
        }

        CountryFilter(final Parcel in) {
            super(in);
            location = in.readString();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(location);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            final @NonNull String location = cache.getLocation();

            if (this.location.isEmpty()) {
                return null == location || location.isEmpty();
            } else {
                return null != location && location.endsWith(this.location);
            }
        }

    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {

        final Map<String, IFilter> filters = new LinkedHashMap<>();

        final String separator = ", ";

        final SortedSet<String> locations = DataStore.getAllStoredLocations();

        for (final String location : locations) {

            // extract the country as the last comma separated part in the location
            final int indexOfSeparator = location.lastIndexOf(separator);
            final int keySubstringIndex = indexOfSeparator == -1 ? 0 : indexOfSeparator;

            final String countryKey = location.substring(keySubstringIndex);

            if (!filters.containsKey(countryKey)) {
                final String countryDisplay = countryKey.isEmpty() ? CgeoApplication.getInstance().getString(R.string.caches_filter_empty) : countryKey;

                filters.put(countryKey, new CountryFilter(countryKey, countryDisplay));
            }
        }

        return new ArrayList<>(filters.values());
    }

}
