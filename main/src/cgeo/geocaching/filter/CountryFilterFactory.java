package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CountryFilterFactory implements IFilterFactory {

    private static String getDisplayName(final @NonNull String country) {
        if (country.isEmpty()) {
            return CgeoApplication.getInstance().getString(R.string.caches_filter_empty);
        } else {
            return country;
        }
    }

    static class CountryFilter extends AbstractFilter {
        private @NonNull String country;

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

        CountryFilter(final String country) {
            super(getDisplayName(country));
            this.country = Objects.requireNonNull(country);
        }

        CountryFilter(final Parcel in) {
            super(in);
            country = Objects.requireNonNull(in.readString());
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(country);
        }

        @Override
        public boolean accepts(final @NonNull Geocache cache) {
            final @NonNull String location = cache.getLocation();

            if (this.country.isEmpty()) {
                return location.isEmpty();
            } else {
                return location.endsWith(this.country);
            }
        }

        @Override
        public int hashCode() {
            return country.hashCode();
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return country.equals(((CountryFilter) obj).country);
        }
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {

        final Set<CountryFilter> filters = new LinkedHashSet<>();

        for (final String country : DataStore.getAllStoredCountries()) {
            filters.add(new CountryFilter(country));
        }

        return new ArrayList<>(filters);
    }

}
