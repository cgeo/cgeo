package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.PersistableFolder;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper activity for WhereYouGo to request the current mf map dir
 * Only returns if there is a map file set or forceAndFeedback=true
 */
public class QueryMapFile extends AppCompatActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getIntent().getExtras();
        final boolean forceAndFeedback = null != bundle && bundle.getBoolean(getString(R.string.cgeo_queryMapFile_actionParam));

        String mapFile;
        try {
            mapFile = PersistableFolder.OFFLINE_MAPS.getUri().toString();
        } catch (NullPointerException e) {
            mapFile = null;
        }
        if (forceAndFeedback || StringUtils.isNotEmpty(mapFile)) {
            try {
                final Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setComponent(new ComponentName(getString(R.string.package_whereyougo), getString(R.string.whereyougo_action_Mapsforge)));
                intent.putExtra(getString(R.string.cgeo_queryMapFile_resultParam), mapFile);
                intent.putExtra(getString(R.string.cgeo_queryMapFile_actionParam), forceAndFeedback);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // oops? shouldn't happen, as we have been called from WhereYouGo
            }
        }

        finish();
    }
}
