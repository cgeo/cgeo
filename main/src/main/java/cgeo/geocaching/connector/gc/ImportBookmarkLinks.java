package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

import javax.annotation.RegEx;

public class ImportBookmarkLinks extends AppCompatActivity {

    private final UrlToIdParser intentUrlParser = defaultBookmarkListUrlToIdParser();

    public static final String BOOKMARK_LIST_API_PREFIX =
            "https://www.geocaching.com/plan/api/gpx/list/";

    interface UrlToIdParser {
        @Nullable String tryExtractFromIntentUrl(@Nullable String intentUrl);
    }

    static class SingleUrlToIdParser implements UrlToIdParser {
        private final Pattern matcherPattern;
        // Android API 21 apparently does not support named capturing groups in Pattern
        // which has introduced in java 1.7...
        private final int groupToExtract;

        SingleUrlToIdParser(
                @RegEx @NonNull final String regex,
                final int groupToExtract
        ) {
            this.matcherPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.groupToExtract = groupToExtract;
        }

        @Override
        public String tryExtractFromIntentUrl(final String intentUrl) {
            if (intentUrl == null) {
                return null;
            }
            final MatcherWrapper matcher = new MatcherWrapper(this.matcherPattern, intentUrl);
            if (matcher.find()) {
                final String maybeId = matcher.group(groupToExtract);
                if (maybeId != null) {
                    return maybeId.toUpperCase();
                }
            }
            return null;
        }
    }

    static class AggregatedUrlToIdParser implements UrlToIdParser {
        private final UrlToIdParser[] parsers;

        AggregatedUrlToIdParser(final UrlToIdParser... parsers) {
            this.parsers = parsers;
        }

        @Override
        public String tryExtractFromIntentUrl(final String intentUrl) {
            if (intentUrl == null) {
                return null;
            }
            for (final UrlToIdParser parser : parsers) {
                final String id = parser.tryExtractFromIntentUrl(intentUrl);
                if (id != null) {
                    return id;
                }
            }
            return null;
        }
    }

    static UrlToIdParser defaultBookmarkListUrlToIdParser() {
        return new AggregatedUrlToIdParser(
                new SingleUrlToIdParser(
                        "^https?://(?:www\\.)?geocaching\\.com/plan/lists/([A-Z0-9]+)",
                        1
                ),
                new SingleUrlToIdParser(
                        "^https?://(?:www\\.)?coord\\.info/([A-Z0-9]+)",
                        1
                )
        );
    }

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        final String url = intent.getDataString();

        final String id = this.intentUrlParser.tryExtractFromIntentUrl(url);

        if (id != null) {
            final Intent bookmarkListIntent = makeIntentFromId(BOOKMARK_LIST_API_PREFIX, id);
            Log.i("starting import of bookmark list with id=" + id);
            startActivity(bookmarkListIntent);
        }
        finish();
    }

    Intent makeIntentFromId(final String prefix, final String id) {
        final Uri uri = Uri.parse(prefix + id);
        final Intent bookmarkListIntent = new Intent(Intent.ACTION_VIEW, uri, this, CacheListActivity.class);
        bookmarkListIntent.setDataAndType(uri, "application/xml");
        bookmarkListIntent.putExtra(Intents.EXTRA_NAME, id);
        return bookmarkListIntent;
    }

}
