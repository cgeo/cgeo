package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.PocketQueryHistory;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.List;

public class BookmarkListActivity extends AbstractListActivity {

    public BookmarkListActivity() {
        title = R.string.menu_lists_bookmarklists;
        progressInfo = R.string.search_bookmark_list;
        errorReadingList = R.string.err_read_bookmark_list;
        warnNoSelectedList = R.string.warn_bookmarklist_select;
        switchLabel = R.string.lists_only_new;
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
        final List<GCList> lists = new ArrayList<>();
        lists.add(GCList.createOwnUnpublished(LocalizationUtils.getString(R.string.list_own_unpublished_caches)));
        final List<GCList> bookmarkLists = GCParser.searchBookmarkLists();
        if (bookmarkLists != null) {
            lists.addAll(bookmarkLists);
        }
        return lists;
    }

    @Override
    boolean alwaysShow(final GCList list) {
        return list.isOwnUnpublished() || PocketQueryHistory.isNew(list);
    }

    @Override
    boolean supportMultiPreview() {
        // Now we are able to parse bookmark lists without download, but only for single list
        return false;
    }
}
