package cgeo.geocaching.maps;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 * @author rsudev
 *
 */
public abstract class AbstractMap {

	MapActivityImpl mapActivity;
	
	protected AbstractMap(MapActivityImpl activity) {
		mapActivity = activity;
	}
	
	public Resources getResources() {
		return mapActivity.getResources();
	}
	
	public Activity getActivity() {
		return mapActivity.getMapActivity();
	}

	public void onCreate(Bundle savedInstanceState) {
		mapActivity.superOnCreate(savedInstanceState);
	}
	
	public void onResume() {
		mapActivity.superOnResume();
	}

	public void onStop() {
		mapActivity.superOnStop();
	}

	public void onPause() {
		mapActivity.superOnPause();
	}

	public void onDestroy() {
		mapActivity.superOnDestroy();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return mapActivity.superOnCreateOptionsMenu(menu);
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return mapActivity.superOnPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return mapActivity.superOnOptionsItemSelected(item);
	}
	
	public abstract void goHome(View view);

	public abstract void goManual(View view);

}
