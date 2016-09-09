package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class PopularityFilter extends AbstractFilter {
    private final int minFavorites;

    public static final Creator<PopularityFilter> CREATOR
            = new Parcelable.Creator<PopularityFilter>() {

        @Override
        public PopularityFilter createFromParcel(final Parcel in) {
            return new PopularityFilter(in);
        }

        @Override
        public PopularityFilter[] newArray(final int size) {
            return new PopularityFilter[size];
        }
    };

    PopularityFilter(@NonNull final String name, final int minFavorites) {
        super(name);
        this.minFavorites = minFavorites;
    }

    protected PopularityFilter(final Parcel in) {
        super(in);
        minFavorites = in.readInt();
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getFavoritePoints() > minFavorites;
    }

    public static class Factory implements IFilterFactory {

        private static final int[] FAVORITES = { 10, 20, 50, 100, 200, 500 };

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(FAVORITES.length);
            for (final int minRange : FAVORITES) {
                final String range = "> " + minRange;
                final String name = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.favorite_points, minRange, range);
                filters.add(new PopularityFilter(name, minRange));
            }
            return filters;
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(minFavorites);
    }
}
