package cgeo.geocaching.filter;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class PersonalDataFilterFactory implements IFilterFactory {

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Arrays.asList(
                new OwnRatingFilter(),
                new PersonalNoteFilter(),
                new ModifiedFilter(),
                new OfflineLogFilter(),
                new GcvoteFilter(),
                new OwnWaypointFilter());
    }

}
