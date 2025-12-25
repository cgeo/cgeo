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

import java.util.List

class PocketQueryListActivity : AbstractListActivity() {

    public PocketQueryListActivity() {
        title = R.string.menu_lists_pocket_queries
        progressInfo = R.string.search_pocket_loading
        errorReadingList = R.string.err_read_pocket_query_list
        warnNoSelectedList = R.string.warn_pocket_query_select
        switchLabel = R.string.pq_only_dl
    }

    override     protected Boolean getFiltersetting() {
        return Settings.getPqShowDownloadableOnly()
    }

    override     protected Unit setFiltersetting(final Boolean value) {
        Settings.setPqShowDownloadableOnly(value)
    }

    override     protected List<GCList> getList() {
        return GCParser.searchPocketQueries()
    }

    override     Boolean alwaysShow(final GCList list) {
        return list.isDownloadable()
    }

    override     Boolean supportMultiPreview() {
        return true
    }
}
