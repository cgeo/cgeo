package cgeo.geocaching.maps;

import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This activity provides an entry point for external intent calls, and then forwards to the currently used map activity
 * implementation.
 */
public class MapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(DefaultMap.getLiveMapIntent(this, Settings.getMapProvider().getMapClass()));
        finish();
    }
}
