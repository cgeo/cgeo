package cgeo.geocaching.mapcommon;

import cgeo.geocaching.mapinterfaces.ActivityImpl;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 * @author rsudev
 *
 */
public class MapBase {

	ActivityImpl mapActivity;
	
	public MapBase(ActivityImpl activity) {
		mapActivity = activity;
	}
	
	public Resources getResources() {
		return mapActivity.getResources();
	}
	
	public Activity getActivity() {
		return mapActivity.getActivity();
	}

	public void onCreate(Bundle savedInstanceState) {
		mapActivity.superOnCreate(savedInstanceState);
	}
	
	public void onResume() {
		mapActivity.superOnResume();
	}

	public void onStop() {
		mapActivity.superOnResume();
	}

	public void onPause() {
		mapActivity.superOnResume();
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
	
}
