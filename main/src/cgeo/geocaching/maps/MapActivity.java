package cgeo.geocaching.maps;

import android.app.Activity;
import android.os.Bundle;

/**
 * This activity provides an entry point for external intent calls, and then forwards to the currently used map activity
 * implementation.
 */
public class MapActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(CGeoMap.getLiveMapIntent(this));
        finish();
    }
}
