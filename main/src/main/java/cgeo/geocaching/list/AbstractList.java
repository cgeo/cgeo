package cgeo.geocaching.list;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class AbstractList {

    public final int id;
    @NonNull
    public final String title;
    public final int markerId;
    private static final SparseArray<AbstractList> LISTS = new SparseArray<>();

    public AbstractList(final int id, @NonNull final String title, @NonNull final int markerId) {
        this.id = id;
        this.title = title;
        this.markerId = markerId;
        LISTS.put(id, this);
    }

    public abstract String getTitleAndCount();

    public abstract boolean isConcrete();

    @NonNull
    public abstract String getTitle();

    public abstract int getNumberOfCaches();

    public void updateNumberOfCaches() {
    }

    @Nullable
    public static AbstractList getListById(final int listId) {
        return LISTS.get(listId);
    }

    @NonNull
    public String toString() {
        return getTitleAndCount();
    }

}
