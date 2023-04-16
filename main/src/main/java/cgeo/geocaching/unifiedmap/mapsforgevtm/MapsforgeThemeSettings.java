package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

public class MapsforgeThemeSettings extends AppCompatActivity {

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
        super.onCreate(savedInstanceState);

        ActivityMixin.setDisplayHomeAsUpEnabled(this, true);
        setContentView(R.layout.layout_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, new MapsforgeThemeSettingsFragment())
                .commit();
    }

}
