package cgeo.geocaching.staticmaps;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

@EActivity
@OptionsMenu(R.menu.static_maps_activity_options)
public class StaticMapsActivity extends AbstractListActivity {

    @Extra(Intents.EXTRA_DOWNLOAD) boolean download = false;
    @Extra(Intents.EXTRA_WAYPOINT_ID) Integer waypointId = null;
    @Extra(Intents.EXTRA_GEOCODE) String geocode = null;

    private Geocache cache;
    private ProgressDialog waitDialog = null;
    private final Handler loadMapsHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            Dialogs.dismiss(waitDialog);
            try {
                if (adapter.isEmpty()) {
                    if (download) {
                        final boolean succeeded = downloadStaticMaps();
                        if (succeeded) {
                            startActivity(StaticMapsActivity.this.getIntent());
                        } else {
                            showToast(res.getString(R.string.err_detail_google_maps_limit_reached));
                        }
                    } else {
                        showToast(res.getString(R.string.err_detail_not_load_map_static));
                    }
                    finish();
                }
            } catch (final Exception e) {
                Log.e("StaticMapsActivity.loadMapsHandler", e);
            }
        }
    };
    private StaticMapsAdapter adapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.staticmaps_activity);

        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (cache == null) {
            Log.e("StaticMapsActivity.onCreate: cannot find the cache " + geocode);
            finish();
            return;
        }

        setCacheTitleBar(cache);

        adapter = new StaticMapsAdapter(this);
        setListAdapter(adapter);

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.map_static_loading), true);
        waitDialog.setCancelable(true);

        (new LoadMapsThread()).start();
    }

    private class LoadMapsThread extends Thread {

        @Override
        public void run() {
            try {
                // try downloading 2 times
                for (int trials = 0; trials < 2; trials++) {
                    for (int level = 1; level <= StaticMapsProvider.MAPS_LEVEL_MAX; level++) {
                        try {
                            if (waypointId != null) {
                                final Bitmap image = StaticMapsProvider.getWaypointMap(geocode, cache.getWaypointById(waypointId), level);
                                if (image != null) {
                                    adapter.add(image);
                                }
                            } else {
                                final Bitmap image = StaticMapsProvider.getCacheMap(geocode, level);
                                if (image != null) {
                                    adapter.add(image);
                                }
                            }
                        } catch (final Exception e) {
                            Log.e("StaticMapsActivity.LoadMapsThread.run", e);
                        }
                    }
                    if (!adapter.isEmpty()) {
                        break;
                    }
                }

                loadMapsHandler.sendMessage(Message.obtain());
            } catch (final Exception e) {
                Log.e("StaticMapsActivity.LoadMapsThread.run", e);
            }
        }
    }

    @OptionsItem(R.id.menu_refresh)
    void refreshMaps() {
        downloadStaticMaps();
        restartActivity();
    }

    private boolean downloadStaticMaps() {
        if (waypointId == null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            StaticMapsProvider.storeCacheStaticMap(cache).blockingAwait();
            return cache.hasStaticMap();
        }
        final Waypoint waypoint = cache.getWaypointById(waypointId);
        if (waypoint != null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            // refresh always removes old waypoint files
            StaticMapsProvider.removeWpStaticMaps(waypoint, geocode);
            StaticMapsProvider.storeWaypointStaticMap(cache, waypoint).blockingAwait();
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint);
        }
        showToast(res.getString(R.string.err_detail_not_load_map_static));
        return false;
    }

    @Override
    public void finish() {
        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
