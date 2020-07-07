package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

class DifficultyFilter extends AbstractDTFilter {

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

    private DifficultyFilter(@StringRes final int name, final float difficulty) {
        // do not inline the name constant. Android Lint has a bug which would lead to using the super super constructors
        // @StringRes annotation for the non-annotated difficulty parameter of this constructor.
        super(name, difficulty);
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
            final List<IFilter> filters = new ArrayList<>((DIFFICULTY_MAX - DIFFICULTY_MIN) * 2 + 1);
            for (int difficulty = DIFFICULTY_MIN * 2; difficulty <= DIFFICULTY_MAX * 2; difficulty++) {
                filters.add(new DifficultyFilter(R.string.cache_difficulty, difficulty / 2.0f));
            }
            return filters;
        }
    }
}
