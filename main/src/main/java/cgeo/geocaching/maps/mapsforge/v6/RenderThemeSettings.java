package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.SettingsActivity;

import android.os.Bundle;
import android.view.MenuItem;

public class RenderThemeSettings extends AbstractActivity {

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMixin.setDisplayHomeAsUpEnabled(this, true);
        setContentView(R.layout.layout_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, new RenderThemeSettingsFragment())
                .commit();
        SettingsActivity.hideRightColumnInLandscapeMode(this);
    }

}
