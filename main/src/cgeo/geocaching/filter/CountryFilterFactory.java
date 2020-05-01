package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryFilterFactory implements IFilterFactory {


    static class CountryFilter extends AbstractFilter {
        private String location = null;

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

        protected CountryFilter(final Parcel in) {
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

        // which countries do we have?
        final Map<String, String> tempUnsorted = new HashMap<>();
        final String separator = ", ";
        for (final String location : DataStore.getAllStoredLocations()) {
            final int indexOfSeparator = location.lastIndexOf(separator);
            final int subStringIndex = indexOfSeparator == -1 ? 0 : indexOfSeparator + separator.length();
            final String countryKey = location.substring(subStringIndex);
            if (!tempUnsorted.containsKey(countryKey)) {
                tempUnsorted.put(countryKey, countryKey);
            }
        }

        // sort them
        final List<String> tempSorted = new ArrayList<>(tempUnsorted.size());
        for (final String countryKey : tempUnsorted.keySet()) {
            tempSorted.add(countryKey);
        }
        Collections.sort(tempSorted);

        // create filters and return sorted filter list
        final List<IFilter> sorted = new ArrayList<>();
        for (final String countryKey : tempSorted) {
            sorted.add(new CountryFilter(countryKey, countryKey.isEmpty() ? CgeoApplication.getInstance().getString(R.string.caches_filter_empty) : countryKey));
        }
        return sorted;
    }

}
