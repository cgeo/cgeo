package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.PocketQueryHistory;
import cgeo.geocaching.utils.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BookmarkListActivity extends AbstractListActivity {

    private static final String ARTIFICIAL_IGNORE_LIST_GUID = "artificial-ignore-list";

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
        final List<GCList> bookmarkLists = GCParser.searchBookmarkLists();
        if (bookmarkLists == null) {
            return null;
        }

        // Create artificial ignore list entry and prepend it
        final List<GCList> lists = new ArrayList<>();
        lists.add(createArtificialIgnoreListEntry());
        lists.addAll(bookmarkLists);
        return lists;
    }

    @Nullable
    private static GCList createArtificialIgnoreListEntry() {
        try {
            // Create a special GCList for the artificial ignore list
            // guid: special marker to identify it as artificial
            // name: displayed as "< Ignore List >"
            // count: 0 (will be populated on sync)
            // downloadable: false (not a real list)
            // bookmarkList: true (treat as bookmark list in UI)
            return new GCList(ARTIFICIAL_IGNORE_LIST_GUID, "< Ignore List >", 0, false, 0, -1, true, null, null);
        } catch (final Exception e) {
            Log.e("BookmarkListActivity.createArtificialIgnoreListEntry: Failed to create artificial ignore list entry", e);
            return null;
        }
    }

    /**
     * Check if a GCList is the artificial ignore list entry.
     */
    static boolean isArtificialIgnoreList(final GCList list) {
        return list != null && ARTIFICIAL_IGNORE_LIST_GUID.equals(list.getGuid());
    }

    @Override
    boolean alwaysShow(final GCList list) {
        // Always show the artificial ignore list entry
        if (isArtificialIgnoreList(list)) {
            return true;
        }
        return PocketQueryHistory.isNew(list);
    }

    @Override
    boolean supportMultiPreview() {
        // Now we are able to parse bookmark lists without download, but only for single list
        return false;
    }
}
