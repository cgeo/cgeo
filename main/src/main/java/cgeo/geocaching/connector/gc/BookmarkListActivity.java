package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.PocketQueryHistory;

import java.util.List;

public class BookmarkListActivity extends AbstractListActivity {

    public BookmarkListActivity() {
        title = R.string.menu_lists_bookmarklists;
        progressInfo = R.string.search_bookmark_list;
        errorReadingList = R.string.err_read_bookmark_list;
    }

    @Override
    protected boolean getFiltersetting() {
        return Settings.getBookmarklistsShowNewOnly();
    }

    @Override
    protected void setFiltersetting(final boolean value) {
        Settings.setBookmarklistsShowNewOnly(value);
    }

    @Override
    protected List<GCList> getList() {
        return GCParser.searchBookmarkLists();
    }

    @Override
    boolean alwaysShow(final GCList list) {
        return PocketQueryHistory.isNew(list);
    }

}
