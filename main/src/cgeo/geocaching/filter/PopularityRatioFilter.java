package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * filters caches by popularity ratio (favorites per find in %).
 */
class PopularityRatioFilter extends AbstractFilter {
    private final int minRatio;

    public static final Creator<PopularityRatioFilter> CREATOR = new Parcelable.Creator<PopularityRatioFilter>() {

        @Override
        public PopularityRatioFilter createFromParcel(final Parcel in) {
            return new PopularityRatioFilter(in);
        }

        @Override
        public PopularityRatioFilter[] newArray(final int size) {
            return new PopularityRatioFilter[size];
        }
    };

    PopularityRatioFilter(@NonNull final String name, final int minRatio) {
        super(name);
        this.minRatio = minRatio;
    }

    protected PopularityRatioFilter(final Parcel in) {
        super(in);
        minRatio = in.readInt();
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final int finds = cache.getFindsCount();

        if (finds == 0) {   // Prevent division by zero
            return false;
        }

        final int favorites = cache.getFavoritePoints();
        final float ratio = 100.0f * favorites / finds;
        return ratio > minRatio;
    }

    public static class Factory implements IFilterFactory {

        private static final int[] RATIOS = { 10, 20, 30, 40, 50, 75 };

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(RATIOS.length);
            for (final int minRange : RATIOS) {
                final String name = CgeoApplication.getInstance().getResources().getString(R.string.more_than_percent_favorite_points, minRange);
                filters.add(new PopularityRatioFilter(name, minRange));
            }
            return filters;
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(minRatio);
    }
}
