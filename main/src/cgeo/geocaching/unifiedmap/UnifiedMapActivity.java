package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

public class UnifiedMapActivity extends AbstractBottomNavigationActivity {

    private AbstractUnifiedMap map = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeMapSource(Settings.getTileProvider());
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        TileProviderFactory.addMapviewMenuItems(this, menu);
//        MapProviderFactory.addMapViewLanguageMenuItems(menu);     // available for mapsforge offline maps only

//        initMyLocationSwitchButton(MapProviderFactory.createLocSwitchMenuItem(this, menu));
//        FilterUtils.initializeFilterMenu(this, this);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (false) {
            // @todo: add other menu options
        } else {
            final AbstractTileProvider tileProvider = TileProviderFactory.getTileProvider(id);
            if (tileProvider != null) {
                item.setChecked(true);
                changeMapSource(tileProvider);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void changeMapSource(final AbstractTileProvider newSource) {
        final AbstractUnifiedMap oldMap = map;
        if (oldMap != null) {
            oldMap.prepareForTileSourceChange();
        }
        map = newSource.getMap();
        if (map != oldMap) {
            map.init(this);
        }
        map.setTileSource(newSource);
        Settings.setTileProvider(newSource);

        // adjust zoom to be in allowed zoom range for current map
        final int currentZoom = map.getCurrentZoom();
        if (currentZoom < map.getZoomMin()) {
            map.setZoom(map.getZoomMin());
        } else if (currentZoom > map.getZoomMax()) {
            map.setZoom(map.getZoomMax());
        }
    }

    // Lifecycle methods

    @Override
    public void onPause() {
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        super.onDestroy();
    }

    // Bottom navigation methods

    @Override
    public int getSelectedBottomItemId() {
        return MENU_MAP;    // @todo
    }

}
