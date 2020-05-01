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
import java.util.Objects;

public class CountryFilterFactory implements IFilterFactory {


    static class CountryFilter extends AbstractFilter {
        private @NonNull String location;

        public static final Creator<CountryFilter> CREATOR = new Creator<CountryFilter>() {

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
            this.location = Objects.requireNonNull(location);
        }

        CountryFilter(final Parcel in) {
            super(in);
            location = Objects.requireNonNull(in.readString());
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(location);
        }

        @Override
        public boolean accepts(final @NonNull Geocache cache) {
            final @NonNull String location = cache.getLocation();

            if (this.location.isEmpty()) {
                return location.isEmpty();
            } else {
                return location.endsWith(this.location);
            }
        }

    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {

        final Map<String, IFilter> filters = new LinkedHashMap<>();

        for (final String country : DataStore.getAllStoredCountries()) {

            if (!filters.containsKey(country)) {
                final String countryDisplay = country.isEmpty() ? CgeoApplication.getInstance().getString(R.string.caches_filter_empty) : country;

                filters.put(country, new CountryFilter(country, countryDisplay));
            }
        }

        return new ArrayList<>(filters.values());
    }

}
