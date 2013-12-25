package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.Log;

import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;

import android.app.ProgressDialog;
import android.content.Context;
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
public class StaticMapsActivity extends AbstractActivity {

    private static final int RESULT_DOWNLOAD_FAILED = -1;
    private static final int RESULT_LOAD_FAILED = -2;
    private static final int RESULT_SUCCEEDED = 1;
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
            final int result = ((Integer) msg.obj).intValue();
            if (result == RESULT_DOWNLOAD_FAILED) {
                showToast(res.getString(R.string.err_detail_google_maps_limit_reached));
                finish();
            } else if (result == RESULT_LOAD_FAILED) {
                showToast(res.getString(R.string.err_detail_not_load_map_static));
                finish();
            } else {
                showStaticMaps();
            }
            return;
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

        loadMapsImpl(download);
    }

    private void loadMapsImpl(final boolean download) {
        if (geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load static maps.");
            finish();
            return;
        }
        this.download = download;

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.map_static_loading), true);
        waitDialog.setCancelable(true);

        (new LoadMapsThread()).start();
    }

    private class LoadMapsThread extends Thread {

        @Override
        public void run() {
            boolean succeeded = true;
            if (download) {
                succeeded = downloadStaticMaps();
            }
            if (!succeeded) {
                Message msg = Message.obtain();
                msg.obj = RESULT_DOWNLOAD_FAILED;
                loadMapsHandler.sendMessage(msg);
                return;
            }
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

                Message msg = Message.obtain();
                if (maps.isEmpty()) {
                    msg.obj = RESULT_LOAD_FAILED;
                } else {
                    msg.obj = RESULT_SUCCEEDED;
                }
                loadMapsHandler.sendMessage(msg);
            } catch (Exception e) {
                Log.e("StaticMapsActivity.LoadMapsThread.run", e);
            }
        }
    }

    @OptionsItem(R.id.menu_refresh)
    void refreshMaps() {
        loadMapsImpl(true);
    }

    private boolean downloadStaticMaps() {
        final Geocache cache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (waypointId == null) {
            StaticMapsProvider.storeCacheStaticMap(cache, true);
            return cache.hasStaticMap();
        }
        final Waypoint waypoint = cache.getWaypointById(waypointId);
        if (waypoint != null) {
            StaticMapsProvider.removeWpStaticMaps(waypoint, geocode);
            StaticMapsProvider.storeWaypointStaticMap(cache, waypoint, true);
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint);
        }
        return false;
    }

    public static void startActivity(final Context activity, final String geocode, final boolean download, final Waypoint waypoint) {
        StaticMapsActivity_.IntentBuilder_ builder = StaticMapsActivity_.intent(activity).geocode(geocode).download(download);
        if (waypoint != null) {
            builder.waypointId(waypoint.getId());
        }
        builder.start();
    }
}