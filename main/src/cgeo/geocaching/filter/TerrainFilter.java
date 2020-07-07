package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

class TerrainFilter extends AbstractDTFilter {

    private TerrainFilter(@StringRes final int name, final float terrain) {
        // do not inline the name constant. Android Lint has a bug which would lead to using the super super constructors
        // @StringRes annotation for the non-annotated terrain parameter of this constructor.
        super(name, terrain);
    }

    protected TerrainFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final float terrain = cache.getTerrain();
        return rangeMin <= terrain && terrain < rangeMax;
    }

    public static class Factory implements IFilterFactory {
        private static final int TERRAIN_MIN = 1;
        private static final int TERRAIN_MAX = 7;

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>((TERRAIN_MAX - TERRAIN_MIN) * 2 + 1);
            for (int terrain = TERRAIN_MIN * 2; terrain <= TERRAIN_MAX * 2; terrain++) {
                filters.add(new TerrainFilter(R.string.cache_terrain, terrain / 2.0f));
            }
            return filters;
        }
    }

    public static final Creator<TerrainFilter> CREATOR
            = new Parcelable.Creator<TerrainFilter>() {

        @Override
        public TerrainFilter createFromParcel(final Parcel in) {
            return new TerrainFilter(in);
        }

        @Override
        public TerrainFilter[] newArray(final int size) {
            return new TerrainFilter[size];
        }
    };
}
