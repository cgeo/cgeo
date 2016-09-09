package cgeo.geocaching.filter;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class PersonalDataFilterFactory implements IFilterFactory {

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Arrays.<IFilter> asList(new OwnRatingFilter(), new PersonalNoteFilter(), new ModifiedFilter(), new OfflineLogFilter());
    }

}
