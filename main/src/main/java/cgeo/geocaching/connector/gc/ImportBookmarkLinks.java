package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.connector.gc.util.AggregatedUrlToIdParser;
import cgeo.geocaching.connector.gc.util.SingleUrlToIdParser;
import cgeo.geocaching.connector.gc.util.UrlToIdParser;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Optional;

public class ImportBookmarkLinks extends AppCompatActivity {

    public static final String BOOKMARK_LIST_API_PREFIX =
            "https://www.geocaching.com/plan/api/gpx/list/";
    private final UrlToIdParser intentUrlParser;

    ImportBookmarkLinks(@NonNull final UrlToIdParser urlToIdParser) {
        this.intentUrlParser = urlToIdParser;
    }

    public ImportBookmarkLinks() {
        this(defaultBookmarkListUrlToIdParser());
    }

    static UrlToIdParser defaultBookmarkListUrlToIdParser() {
        return new AggregatedUrlToIdParser(
                new SingleUrlToIdParser(
                        "^https?://(?:www\\.)?geocaching\\.com/plan/lists/(?<identifier>[A-Z0-9]+)",
                        1
                ),
                new SingleUrlToIdParser(
                        "^https?://(?:www\\.)?coord\\.info/(?<identifier>[A-Z0-9]+)",
                        1
                )
        );
    }

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Optional.ofNullable(getIntent())
                .map(Intent::getDataString)
                .map(this.intentUrlParser::tryExtractFromIntentUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(bookmarkListId -> makeIntentFromId(BOOKMARK_LIST_API_PREFIX, bookmarkListId))
                .ifPresent(this::startActivity);
        finish();
    }

    Intent makeIntentFromId(final String apiUrlPrefix, final String bookmarkListId) {
        final Uri uri = Uri.parse(apiUrlPrefix + bookmarkListId);
        final Intent bookmarkListIntent = new Intent(
                Intent.ACTION_VIEW,
                uri,
                this,
                CacheListActivity.class
        );
        bookmarkListIntent.setDataAndType(uri, "application/xml");
        bookmarkListIntent.putExtra(Intents.EXTRA_NAME, bookmarkListId);
        Log.i("starting import of bookmark list with id=" + bookmarkListId);
        return bookmarkListIntent;
    }
}
