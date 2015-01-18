package cgeo.geocaching.filter;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class PersonalDataFilterFactory implements IFilterFactory {

    @Override
    @NonNull
    public List<? extends IFilter> getFilters() {
        return Arrays.asList(new OwnRatingFilter(), new PersonalNoteFilter(), new ModifiedFilter());
    }

}
