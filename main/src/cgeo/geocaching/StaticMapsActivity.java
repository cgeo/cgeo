package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.LoadFlags;

import org.apache.commons.collections.CollectionUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class StaticMapsActivity extends AbstractActivity {

    private static final int MENU_REFRESH = 1;
    private final List<Bitmap> maps = new ArrayList<Bitmap>();
    private Integer waypoint_id = null;
    private String geocode = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private LinearLayout smapsView = null;
    private BitmapFactory factory = null;
    private final Handler loadMapsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (CollectionUtils.isEmpty(maps)) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    if ((waypoint_id != null && Settings.isStoreOfflineWpMaps()) || (waypoint_id == null && Settings.isStoreOfflineMaps())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(StaticMapsActivity.this);
                        builder.setMessage(R.string.err_detail_ask_store_map_static).setPositiveButton(android.R.string.yes, dialogClickListener)
                                .setNegativeButton(android.R.string.no, dialogClickListener).show();
                    } else {
                        showToast(res.getString(R.string.err_detail_not_load_map_static));
                        finish();
                    }
                    return;
                } else {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    if (inflater == null) {
                        inflater = getLayoutInflater();
                    }

                    if (smapsView == null) {
                        smapsView = (LinearLayout) findViewById(R.id.maps_list);
                    }
                    smapsView.removeAllViews();

                    for (Bitmap image : maps) {
                        if (image != null) {
                            final ImageView map = (ImageView) inflater.inflate(R.layout.map_static_item, null);
                            map.setImageBitmap(image);
                            smapsView.addView(map);
                        }
                    }
                }
            } catch (Exception e) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
                Log.e(Settings.tag, "StaticMapsActivity.loadMapsHandler: " + e.toString());
            }
        }
    };

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    downloadStaticMaps();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    showToast(res.getString(R.string.err_detail_not_load_map_static));
                    break;
            }
            finish();
        }
    };

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
            geocode = extras.getString("geocode");
            if (extras.containsKey("waypoint")) {
                waypoint_id = extras.getInt("waypoint");
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

                for (int level = 1; level <= 5; level++) {
                    try {
                        if (waypoint_id != null) {
                            final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, "wp" + waypoint_id + "_", level, false).getPath());
                            if (image != null) {
                                maps.add(image);
                            }
                        } else {
                            final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, "", level, false).getPath());
                            if (image != null) {
                                maps.add(image);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run.1: " + e.toString());
                    }
                }

                if (maps.isEmpty()) {
                    for (int level = 1; level <= 5; level++) {
                        try {
                            if (waypoint_id != null) {
                                final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, "wp" + waypoint_id + "_", level, false).getPath());
                                if (image != null) {
                                    maps.add(image);
                                }
                            } else {
                                final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, "", level, false).getPath());
                                if (image != null) {
                                    maps.add(image);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run.2: " + e.toString());
                        }
                    }
                }

                loadMapsHandler.sendMessage(new Message());
            } catch (Exception e) {
                Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run: " + e.toString());
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

    private void downloadStaticMaps() {
        final cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (waypoint_id == null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            StaticMapsProvider.storeCacheStaticMap(cache, this, true);
        } else {
            final cgWaypoint waypoint = cache.getWaypointById(waypoint_id);
            if (waypoint != null) {
                showToast(res.getString(R.string.info_storing_static_maps));
                StaticMapsProvider.storeWaypointStaticMap(cache, this, waypoint, true);
            } else {
                showToast(res.getString(R.string.err_detail_not_load_map_static));
            }
        }
    }
}