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

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.connector.gc.util.AggregatedUrlToIdParser
import cgeo.geocaching.connector.gc.util.SingleUrlToIdParser
import cgeo.geocaching.connector.gc.util.UrlToIdParser
import cgeo.geocaching.utils.Log

import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Optional

class ImportBookmarkLinks : AbstractActivity() {

    public static val BOOKMARK_LIST_API_PREFIX: String =
            "https://www.geocaching.com/api/live/v1/gpx/list/"
    private final UrlToIdParser intentUrlParser

    ImportBookmarkLinks(final UrlToIdParser urlToIdParser) {
        this.intentUrlParser = urlToIdParser
    }

    public ImportBookmarkLinks() {
        this(defaultBookmarkListUrlToIdParser())
    }

    static UrlToIdParser defaultBookmarkListUrlToIdParser() {
        return AggregatedUrlToIdParser(
                SingleUrlToIdParser(
                        "^https?://(?:www\\.)?geocaching\\.com/plan/lists/(?<identifier>[A-Z0-9]+)",
                        1
                ),
                SingleUrlToIdParser(
                        "^https?://(?:www\\.)?coord\\.info/(?<identifier>[A-Z0-9]+)",
                        1
                )
        )
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        Optional.ofNullable(getIntent())
                .map(Intent::getDataString)
                .map(this.intentUrlParser::tryExtractFromIntentUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(bookmarkListId -> makeIntentFromId(BOOKMARK_LIST_API_PREFIX, bookmarkListId))
                .ifPresent(this::startActivity)
        finish()
    }

    Intent makeIntentFromId(final String apiUrlPrefix, final String bookmarkListId) {
        val uri: Uri = Uri.parse(apiUrlPrefix + bookmarkListId)
        val bookmarkListIntent: Intent = Intent(
                Intent.ACTION_VIEW,
                uri,
                this,
                CacheListActivity.class
        )
        bookmarkListIntent.setDataAndType(uri, "application/xml")
        bookmarkListIntent.putExtra(Intents.EXTRA_NAME, bookmarkListId)
        Log.i("starting import of bookmark list with id=" + bookmarkListId)
        return bookmarkListIntent
    }
}
