package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

/**
 * Filter that accepts {@link Geocache}s with a non empty personal note stored locally.
 */
public class PersonalNoteFilter extends AbstractFilter implements IFilterFactory {

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
    public List<PersonalNoteFilter> getFilters() {
        return Collections.singletonList(this);
    }

    public static final Creator<PersonalNoteFilter> CREATOR
            = new Parcelable.Creator<PersonalNoteFilter>() {

        @Override
        public PersonalNoteFilter createFromParcel(final Parcel in) {
            return new PersonalNoteFilter(in);
        }

        @Override
        public PersonalNoteFilter[] newArray(final int size) {
            return new PersonalNoteFilter[size];
        }
    };
}
