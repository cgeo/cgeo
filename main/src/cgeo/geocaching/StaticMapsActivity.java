package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.apache.commons.collections4.CollectionUtils;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

@EActivity
@OptionsMenu(R.menu.static_maps_activity_options)
public class StaticMapsActivity extends AbstractActionBarActivity {

    private static final String EXTRAS_WAYPOINT = "waypoint";
    private static final String EXTRAS_DOWNLOAD = "download";
    private static final String EXTRAS_GEOCODE = "geocode";

    @Extra(EXTRAS_DOWNLOAD) boolean download = false;
    @Extra(EXTRAS_WAYPOINT) Integer waypointId = null;
    @Extra(EXTRAS_GEOCODE) String geocode = null;

    private final List<Bitmap> maps = new ArrayList<Bitmap>();
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private LinearLayout smapsView = null;
    private final Handler loadMapsHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }
            try {
                if (CollectionUtils.isEmpty(maps)) {
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
                } else {
                    showStaticMaps();
                }
            } catch (Exception e) {
                Log.e("StaticMapsActivity.loadMapsHandler", e);
            }
        }
    };

    /**
     * Shows the static maps.
     */
    private void showStaticMaps() {
        if (inflater == null) {
            inflater = getLayoutInflater();
        }

        if (smapsView == null) {
            smapsView = (LinearLayout) findViewById(R.id.maps_list);
        }
        smapsView.removeAllViews();

        for (final Bitmap image : maps) {
            if (image != null) {
                final ImageView map = (ImageView) inflater.inflate(R.layout.staticmaps_activity_item, null);
                map.setImageBitmap(image);
                smapsView.addView(map);
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.staticmaps_activity);

        if (geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load static maps.");
            finish();
            return;
        }

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
                                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                                final Bitmap image = StaticMapsProvider.getWaypointMap(geocode, cache.getWaypointById(waypointId), level);
                                if (image != null) {
                                    maps.add(image);
                                }
                            } else {
                                final Bitmap image = StaticMapsProvider.getCacheMap(geocode, level);
                                if (image != null) {
                                    maps.add(image);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("StaticMapsActivity.LoadMapsThread.run", e);
                        }
                    }
                    if (!maps.isEmpty()) {
                        break;
                    }
                }

                loadMapsHandler.sendMessage(Message.obtain());
            } catch (Exception e) {
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
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (waypointId == null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            RxUtils.waitForCompletion(StaticMapsProvider.storeCacheStaticMap(cache));
            return cache.hasStaticMap();
        }
        final Waypoint waypoint = cache.getWaypointById(waypointId);
        if (waypoint != null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            // refresh always removes old waypoint files
            StaticMapsProvider.removeWpStaticMaps(waypoint, geocode);
            RxUtils.waitForCompletion(StaticMapsProvider.storeWaypointStaticMap(cache, waypoint));
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint);
        }
        showToast(res.getString(R.string.err_detail_not_load_map_static));
        return false;
    }
}