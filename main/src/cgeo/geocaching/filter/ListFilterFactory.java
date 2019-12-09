package cgeo.geocaching.filter;

import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ListFilterFactory implements IFilterFactory {
    static class ListFilter extends AbstractFilter {
        private int listId = 0;

        public static final Creator<ListFilter> CREATOR
                = new Parcelable.Creator<ListFilter>() {

            @Override
            public ListFilter createFromParcel(final Parcel in) {
                return new ListFilter(in);
            }

            @Override
            public ListFilter[] newArray(final int size) {
                return new ListFilter[size];
            }
        };

        ListFilter(final int listId, final String title) {
            super(title);
            this.listId = listId;
        }

        protected ListFilter(final Parcel in) {
            super(in);
            listId = in.readInt();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(listId);
        }
        
        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.getLists().contains(listId);
        }

    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        final List<IFilter> filters = new ArrayList<>(6);
        final List<AbstractList> storedLists = StoredList.UserInterface.getMenuLists(true, PseudoList.NEW_LIST.id);
        for (final AbstractList temp : storedLists) {
            filters.add(new ListFilter(temp.id, temp.title));
        }
        return filters;
    }

}
