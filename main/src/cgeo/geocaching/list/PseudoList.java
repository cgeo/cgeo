package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

public class PseudoList extends AbstractList {

    public static final int ALL_LIST_ID = 2;
    public static final PseudoList ALL_LIST = new PseudoList(ALL_LIST_ID, R.string.list_all_lists);

    public PseudoList(int id, final int titleResourceId) {
        super(id, CgeoApplication.getInstance().getResources().getString(titleResourceId));
    }

    @Override
    public String getTitleAndCount() {
        return "<" + title + ">";
    }

}
