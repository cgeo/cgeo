package cgeo.geocaching.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.util.SparseArray;

public abstract class AbstractList {

    public final int id;
    @NonNull
    public final String title;
    private static final SparseArray<AbstractList> LISTS = new SparseArray<>();

    public AbstractList(final int id, @NonNull final String title) {
        this.id = id;
        this.title = title;
        LISTS.put(id, this);
    }

    public abstract String getTitleAndCount();

    public abstract boolean isConcrete();

    @NonNull
    public abstract String getTitle();

    public abstract int getNumberOfCaches();

    @Nullable
    public static AbstractList getListById(final int listId) {
        return LISTS.get(listId);
    }

}
