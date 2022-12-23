package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;

import java.util.List;

public class PocketQueryListActivity extends AbstractListActivity {

    public PocketQueryListActivity() {
        title = R.string.menu_lists_pocket_queries;
        progressInfo = R.string.search_pocket_loading;
        errorReadingList = R.string.err_read_pocket_query_list;
    }

    @Override
    protected boolean getFiltersetting() {
        return Settings.getPqShowDownloadableOnly();
    }

    @Override
    protected void setFiltersetting(final boolean value) {
        Settings.setPqShowDownloadableOnly(value);
    }

    @Override
    protected List<GCList> getList() {
        return GCParser.searchPocketQueries();
    }

    @Override
    boolean alwaysShow(final GCList list) {
        return list.isDownloadable();
    }
}
