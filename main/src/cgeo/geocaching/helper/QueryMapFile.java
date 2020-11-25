package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Helper activity for WhereYouGo to request the current mf map dir
 * Only returns if there is a map file set or forceAndFeedback=true
 */
public class QueryMapFile extends Activity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getIntent().getExtras();
        final boolean forceAndFeedback = null != bundle && bundle.getBoolean(getString(R.string.cgeo_queryMapFile_actionParam));

        final String mapFile = Settings.getMapFileDirectory();
        if (forceAndFeedback || (mapFile != null && !"".equals(mapFile))) {
            try {
                final Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setComponent(new ComponentName(getString(R.string.whereyougo_package), getString(R.string.whereyougo_action_Mapsforge)));
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
