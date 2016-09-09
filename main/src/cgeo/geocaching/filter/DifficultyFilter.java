package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

class DifficultyFilter extends AbstractRangeFilter {

    public static final Creator<DifficultyFilter> CREATOR = new Parcelable.Creator<DifficultyFilter>() {

        @Override
        public DifficultyFilter createFromParcel(final Parcel in) {
            return new DifficultyFilter(in);
        }

        @Override
        public DifficultyFilter[] newArray(final int size) {
            return new DifficultyFilter[size];
        }
    };

    private DifficultyFilter(@StringRes final int name, final int difficulty) {
        // do not inline the name constant. Android Lint has a bug which would lead to using the super super constructors
        // @StringRes annotation for the non-annotated difficulty parameter of this constructor.
        super(name, difficulty, Factory.DIFFICULTY_MAX);
    }

    protected DifficultyFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final float difficulty = cache.getDifficulty();
        return rangeMin <= difficulty && difficulty < rangeMax;
    }

    public static class Factory implements IFilterFactory {

        private static final int DIFFICULTY_MIN = 1;
        private static final int DIFFICULTY_MAX = 5;

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(DIFFICULTY_MAX);
            for (int difficulty = DIFFICULTY_MIN; difficulty <= DIFFICULTY_MAX; difficulty++) {
                filters.add(new DifficultyFilter(R.string.cache_difficulty, difficulty));
            }
            return filters;
        }
    }
}
