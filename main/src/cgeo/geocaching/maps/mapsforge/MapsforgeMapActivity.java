package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMap;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.utils.Log;

import org.mapsforge.android.maps.MapActivity;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.io.File;
import java.util.ArrayList;

public class MapsforgeMapActivity extends MapActivity implements MapActivityImpl {

    private AbstractMap mapBase;
    private static int selectMapSourceMenuId = AbstractMap.MENU_MAP_SPECIFIC_MIN;

    public MapsforgeMapActivity() {
        mapBase = new CGeoMap(this);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        mapBase.onCreate(icicle);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        mapBase.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mapBase.onDestroy();
    }

    @Override
    protected void onPause() {
        mapBase.onPause();
    }

    @Override
    protected void onResume() {
        mapBase.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean retval = mapBase.onCreateOptionsMenu(menu);
        Resources res = this.getResources();

        SubMenu subMenuSelectSource = menu.addSubMenu(Menu.NONE, selectMapSourceMenuId, Menu.NONE, res.getString(R.string.map_select_offline_map_source));
        subMenuSelectSource.setHeaderTitle(res.getString(R.string.map_select_offline_map_source));

        try {
            ArrayList<String> mapDatabaseList = mapBase.getMapView().getMapDatabaseList();
            if (mapDatabaseList.size() > 0) {
                for (int i = 0; i < mapDatabaseList.size(); i++) {
                    MenuItem itm = subMenuSelectSource.add(AbstractMap.MENU_MAP_SPECIFIC_MIN
                            , AbstractMap.MENU_MAP_SPECIFIC_MIN + i + 1
                            , Menu.NONE
                            , mapDatabaseList.get(i));
                    itm.setCheckable(true);
                    itm.setChecked((new File(mapBase.getMapView().getCurrentMapDatabase())).getName().equals(mapDatabaseList.get(i)));
                }
                subMenuSelectSource.setGroupCheckable(AbstractMap.MENU_MAP_SPECIFIC_MIN, true, true);
            }
        } catch (Exception e) {
            Log.e("MapforgeMapActivity.onCreateOptionsMenu: " + e);
        }

        return retval;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() > selectMapSourceMenuId && item.getItemId() <= AbstractMap.MENU_MAP_SPECIFIC_MAX) {
            mapBase.getMapView().setMapDatabase(item.getTitle().toString());
            item.setChecked(true);
            return true;
        }
        return mapBase.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(selectMapSourceMenuId).setEnabled(mapBase.getMapView().isMapDatabaseSwitchSupported());
        return mapBase.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        mapBase.onStop();
    }

    @Override
    public void superOnCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean superOnCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void superOnDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean superOnOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void superOnResume() {
        super.onResume();
    }

    @Override
    public void superOnStop() {
        super.onStop();
    }

    @Override
    public void superOnPause() {
        super.onPause();
    }

    @Override
    public boolean superOnPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    // close activity and open homescreen
    @Override
    public void goHome(View view) {
        mapBase.goHome(view);
    }

    // open manual entry
    @Override
    public void goManual(View view) {
        mapBase.goManual(view);
    }
}
