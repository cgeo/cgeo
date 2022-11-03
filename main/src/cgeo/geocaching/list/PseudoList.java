package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public abstract class PseudoList extends AbstractList {

    private static final int ALL_LIST_ID = 2;
    /**
     * list entry to show all caches
     */
    public static final PseudoList ALL_LIST = new PseudoList(ALL_LIST_ID, R.string.list_all_lists) {
        @Override
        public int getNumberOfCaches() {
            return DataStore.getAllCachesCount();
        }
    };

    private static final int NEW_LIST_ID = 3;
    /**
     * list entry to create a new list
     */
    public static final AbstractList NEW_LIST = new PseudoList(NEW_LIST_ID, R.string.list_menu_create) {
        @Override
        public int getNumberOfCaches() {
            return -1;
        }
    };

    private static final int HISTORY_LIST_ID = 4;
    /**
     * list entry to show log history
     */
    public static final AbstractList HISTORY_LIST = new PseudoList(HISTORY_LIST_ID, R.string.menu_history) {
        @Override
        public int getNumberOfCaches() {
            return DataStore.getAllStoredCachesCount(HISTORY_LIST_ID);
        }
    };

    /**
     * private constructor to have all instances as constants in the class
     */
    private PseudoList(final int id, @StringRes final int titleResourceId) {
        super(id, CgeoApplication.getInstance().getString(titleResourceId));
    }

    @Override
    public String getTitleAndCount() {
        return "<" + title + ">";
    }

    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    @Override
    public boolean isConcrete() {
        return false;
    }

}
