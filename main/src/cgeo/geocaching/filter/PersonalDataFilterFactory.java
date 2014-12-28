package cgeo.geocaching.filter;

import java.util.Arrays;
import java.util.List;

public class PersonalDataFilterFactory implements IFilterFactory {

    @Override
    public List<? extends IFilter> getFilters() {
        return Arrays.asList(new OwnRatingFilter(), new PersonalNoteFilter(), new ModifiedFilter());
    }

}
