package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Filter that accepts {@link Geocache}s with a non empty personal note stored locally.
 */
public class PersonalNoteFilter extends AbstractFilter implements IFilterFactory {

    public static final Creator<PersonalNoteFilter> CREATOR = new Parcelable.Creator<PersonalNoteFilter>() {

        @Override
        public PersonalNoteFilter createFromParcel(final Parcel in) {
            return new PersonalNoteFilter(in);
        }

        @Override
        public PersonalNoteFilter[] newArray(final int size) {
            return new PersonalNoteFilter[size];
        }
    };

    protected PersonalNoteFilter() {
        super(R.string.caches_filter_personal_note);
    }

    protected PersonalNoteFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return StringUtils.isNotBlank(cache.getPersonalNote());
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Collections.<IFilter> singletonList(this);
    }
}
