package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

class DifficultyFilter extends AbstractRangeFilter {

    public DifficultyFilter(final int difficulty) {
        super(R.string.cache_difficulty, difficulty);
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
                filters.add(new DifficultyFilter(difficulty));
            }
            return filters;
        }
    }

    public static final Creator<DifficultyFilter> CREATOR
            = new Parcelable.Creator<DifficultyFilter>() {

        @Override
        public DifficultyFilter createFromParcel(final Parcel in) {
            return new DifficultyFilter(in);
        }

        @Override
        public DifficultyFilter[] newArray(final int size) {
            return new DifficultyFilter[size];
        }
    };
}
