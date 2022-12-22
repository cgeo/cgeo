package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

public class RenderThemeSettings extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(Settings.isLightSkin(this) ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        ActivityMixin.setDisplayHomeAsUpEnabled(this, true);
        setContentView(R.layout.layout_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, new RenderThemeSettingsFragment())
                .commit();
    }

}
