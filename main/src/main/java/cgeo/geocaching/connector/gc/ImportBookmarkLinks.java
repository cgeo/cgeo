package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

public class ImportBookmarkLinks extends AppCompatActivity {

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        final String url = intent.getDataString();
        // check url
        final Pattern p = Pattern.compile("http[s]:\\/\\/(www\\.)?geocaching\\.com\\/plan\\/lists\\/([A-Z0-9]+)(\\?)");
        final MatcherWrapper matcher = new MatcherWrapper(p, url);
        while (matcher.find()) {
            // list id given?
            if (matcher.groupCount() >= 2) {
                final Uri uri = Uri.parse("https://www.geocaching.com/plan/api/gpx/list/" + matcher.group(2));
                Log.i("starting import of bookmark list with id=" + matcher.group(2));
                final Intent cachesIntent = new Intent(Intent.ACTION_VIEW, uri, this, CacheListActivity.class);
                cachesIntent.setDataAndType(uri, "application/zip");
                cachesIntent.putExtra(Intents.EXTRA_NAME, matcher.group(2));
                startActivity(cachesIntent);
            }
        }
        finish();
    }
}
