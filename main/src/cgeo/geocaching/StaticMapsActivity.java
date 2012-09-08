package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class StaticMapsActivity extends AbstractActivity {

    private static final String EXTRAS_WAYPOINT = "waypoint";
    private static final String EXTRAS_DOWNLOAD = "download";
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final int MENU_REFRESH = 1;
    private final List<Bitmap> maps = new ArrayList<Bitmap>();
    private boolean download = false;
    private Integer waypoint_id = null;
    private String geocode = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private LinearLayout smapsView = null;
    private BitmapFactory factory = null;
    private final Handler loadMapsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
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
                Log.e("StaticMapsActivity.loadMapsHandler: " + e.toString());
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
                final ImageView map = (ImageView) inflater.inflate(R.layout.map_static_item, null);
                map.setImageBitmap(image);
                smapsView.addView(map);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.map_static);
        setTitle(res.getString(R.string.map_static_title));

        // get parameters
        final Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            download = extras.getBoolean(EXTRAS_DOWNLOAD, false);
            geocode = extras.getString(EXTRAS_GEOCODE);
            if (extras.containsKey(EXTRAS_WAYPOINT)) {
                waypoint_id = extras.getInt(EXTRAS_WAYPOINT);
            }
        }

        if (geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load static maps.");
            finish();
            return;
        }

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.map_static_loading), true);
        waitDialog.setCancelable(true);

        (new LoadMapsThread()).start();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private class LoadMapsThread extends Thread {

        @Override
        public void run() {
            try {
                if (factory == null) {
                    factory = new BitmapFactory();
                }

                // try downloading 2 times
                for (int trials = 0; trials < 2; trials++) {
                    for (int level = 1; level <= 5; level++) {
                        try {
                            if (waypoint_id != null) {
                                final Bitmap image = StaticMapsProvider.getWaypointMap(geocode, waypoint_id, level);
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
                            Log.e("StaticMapsActivity.LoadMapsThread.run: " + e.toString());
                        }
                    }
                    if (!maps.isEmpty()) {
                        break;
                    }
                }

                loadMapsHandler.sendMessage(Message.obtain());
            } catch (Exception e) {
                Log.e("StaticMapsActivity.LoadMapsThread.run: " + e.toString());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, res.getString(R.string.cache_offline_refresh));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_REFRESH) {
            downloadStaticMaps();
            restartActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean downloadStaticMaps() {
        final cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (waypoint_id == null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            StaticMapsProvider.storeCacheStaticMap(cache, this, true);
            return cache.hasStaticMap();
        }
        final cgWaypoint waypoint = cache.getWaypointById(waypoint_id);
        if (waypoint != null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            StaticMapsProvider.storeWaypointStaticMap(cache, this, waypoint, true);
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint_id);
        }
        showToast(res.getString(R.string.err_detail_not_load_map_static));
        return false;
    }

    public static void startActivity(final Context activity, final String geocode, final boolean download, final cgWaypoint waypoint) {
        final Intent intent = new Intent(activity, StaticMapsActivity.class);
        // if resuming our app within this activity, finish it and return to the cache activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(EXTRAS_GEOCODE, geocode);
        if (download) {
            intent.putExtra(EXTRAS_DOWNLOAD, true);
        }
        if (waypoint != null) {
            intent.putExtra(EXTRAS_WAYPOINT, waypoint.getId());
        }
        activity.startActivity(intent);
    }
}