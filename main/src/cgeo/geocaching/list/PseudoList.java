package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

public class PseudoList extends AbstractList {

    private static final int ALL_LIST_ID = 2;
    /**
     * list entry to show all caches
     */
    public static final PseudoList ALL_LIST = new PseudoList(ALL_LIST_ID, R.string.list_all_lists);

    private static final int NEW_LIST_ID = 3;
    /**
     * list entry to create a new list
     */
    public static final AbstractList NEW_LIST = new PseudoList(NEW_LIST_ID, R.string.list_menu_create);

    private static final int HISTORY_LIST_ID = 4;
    /**
     * list entry to create a new list
     */
    public static final AbstractList HISTORY_LIST = new PseudoList(HISTORY_LIST_ID, R.string.menu_history);

    /**
     * private constructor to have all instances as constants in the class
     */
    private PseudoList(int id, final int titleResourceId) {
        super(id, CgeoApplication.getInstance().getResources().getString(titleResourceId));
    }

    @Override
    public String getTitleAndCount() {
        return "<" + title + ">";
    }

    @Override
    public boolean isConcrete() {
        return false;
    }

}
