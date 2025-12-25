// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.models.GCList
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.extension.PocketQueryHistory

import java.util.List

class BookmarkListActivity : AbstractListActivity() {

    public BookmarkListActivity() {
        title = R.string.menu_lists_bookmarklists
        progressInfo = R.string.search_bookmark_list
        errorReadingList = R.string.err_read_bookmark_list
        warnNoSelectedList = R.string.warn_bookmarklist_select
        switchLabel = R.string.lists_only_new
    }

    override     protected Boolean getFiltersetting() {
        return Settings.getBookmarklistsShowNewOnly()
    }

    override     protected Unit setFiltersetting(final Boolean value) {
        Settings.setBookmarklistsShowNewOnly(value)
    }

    override     protected List<GCList> getList() {
        return GCParser.searchBookmarkLists()
    }

    override     Boolean alwaysShow(final GCList list) {
        return PocketQueryHistory.isNew(list)
    }

    override     Boolean supportMultiPreview() {
        // Now we are able to parse bookmark lists without download, but only for single list
        return false
    }
}
