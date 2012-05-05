package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.EnumSet;

public class cgeowaypoint extends AbstractActivity implements IObserver<IGeoData> {

    private static final int MENU_ID_NAVIGATION = 0;
    private static final int MENU_ID_CACHES_AROUND = 5;
    private static final int MENU_ID_DEFAULT_NAVIGATION = 2;
    private static final int MENU_ID_OPEN_GEOCACHE = 6;
    private cgWaypoint waypoint = null;
    private int id = -1;
    private ProgressDialog waitDialog = null;
    private Handler loadWaypointHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
                waitDialog = null;
            }

            if (waypoint == null) {
                showToast(res.getString(R.string.err_waypoint_load_failed));
                finish();
                return;
            }

            final TextView identification = (TextView) findViewById(R.id.identification);
            final TextView coords = (TextView) findViewById(R.id.coordinates);
            final ImageView defaultNavigation = (ImageView) findViewById(R.id.defaultNavigation);
            final View separator = findViewById(R.id.separator);

            final View headline = findViewById(R.id.headline);
            registerNavigationMenu(headline);

            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(Html.fromHtml(waypoint.getName()).toString());
            } else {
                setTitle(res.getString(R.string.waypoint_title));
            }

            if (!waypoint.getPrefix().equalsIgnoreCase("OWN")) {
                identification.setText(waypoint.getPrefix() + "/" + waypoint.getLookup());
            } else {
                identification.setText(res.getString(R.string.waypoint_custom));
            }
            registerNavigationMenu(identification);
            waypoint.setIcon(res, identification);

            if (waypoint.getCoords() != null) {
                coords.setText(Html.fromHtml(waypoint.getCoords().toString()), TextView.BufferType.SPANNABLE);
                defaultNavigation.setVisibility(View.VISIBLE);
                separator.setVisibility(View.VISIBLE);
            } else {
                coords.setText(res.getString(R.string.waypoint_unknown_coordinates));
                defaultNavigation.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
            }
            registerNavigationMenu(coords);

            if (StringUtils.isNotBlank(waypoint.getNote())) {
                final TextView note = (TextView) findViewById(R.id.note);
                note.setText(Html.fromHtml(waypoint.getNote()), TextView.BufferType.SPANNABLE);
                registerNavigationMenu(note);
            }

            final Button buttonEdit = (Button) findViewById(R.id.edit);
            buttonEdit.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    cgeowaypointadd.startActivityEditWaypoint(cgeowaypoint.this, id);
                }
            });

            if (waypoint.isUserDefined()) {
                final Button buttonDelete = (Button) findViewById(R.id.delete);
                buttonDelete.setOnClickListener(new deleteWaypointListener());
                buttonDelete.setVisibility(View.VISIBLE);
            }
        }

        private void registerNavigationMenu(View view) {
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    registerForContextMenu(v);
                    if (navigationPossible()) {
                        openContextMenu(v);
                    }
                }
            });
        }
    };

    public cgeowaypoint() {
        super("c:geo-waypoint-details");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.waypoint);
        setTitle(R.string.waypoint_title);

        // get parameters
        Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            id = extras.getInt("waypoint");
        }

        if (id <= 0) {
            showToast(res.getString(R.string.err_waypoint_unknown));
            finish();
            return;
        }

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
        waitDialog.setCancelable(true);

        (new loadWaypoint()).start();

        ImageView defaultNavigationImageView = (ImageView) findViewById(R.id.defaultNavigation);
        defaultNavigationImageView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startDefaultNavigation2();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        app.addGeoObserver(this);

        if (waitDialog == null) {
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new loadWaypoint()).start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        app.deleteGeoObserver(this);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ID_DEFAULT_NAVIGATION, 0, NavigationAppFactory.getDefaultNavigationApplication().getName()).setIcon(R.drawable.ic_menu_compass); // default navigation tool
        menu.add(0, MENU_ID_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(R.drawable.ic_menu_mapmode);
        menu.add(0, MENU_ID_CACHES_AROUND, 0, res.getString(R.string.cache_menu_around)).setIcon(R.drawable.ic_menu_rotate); // caches around
        menu.add(0, MENU_ID_OPEN_GEOCACHE, 0, res.getString(R.string.waypoint_menu_open_cache)).setIcon(R.drawable.ic_menu_mylocation); // open geocache

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            boolean visible = waypoint != null && waypoint.getCoords() != null;
            menu.findItem(MENU_ID_NAVIGATION).setVisible(visible);
            menu.findItem(MENU_ID_DEFAULT_NAVIGATION).setVisible(visible);
            menu.findItem(MENU_ID_CACHES_AROUND).setVisible(visible);

            boolean openGeocache = waypoint != null && StringUtils.isNotEmpty(waypoint.getGeocode());
            menu.findItem(MENU_ID_OPEN_GEOCACHE).setVisible(openGeocache);
        } catch (Exception e) {
            // nothing
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_DEFAULT_NAVIGATION:
                goDefaultNavigation(null);
                return true;
            case MENU_ID_CACHES_AROUND:
                cachesAround();
                return true;
            case MENU_ID_OPEN_GEOCACHE:
                goToGeocache();
                return true;
            case MENU_ID_NAVIGATION:
                NavigationAppFactory.showNavigationMenu(this, null, waypoint, null);
                return true;
            default:
                return false;
        }
    }

    private void cachesAround() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        cgeocaches.startActivityCoordinates(this, waypoint.getCoords());

        finish();
    }

    private void goToGeocache() {
        if (waypoint == null || waypoint.getGeocode() == null) {
            showToast(res.getString(R.string.err_waypoint_open_cache_failed));
            return;
        }

        CacheDetailActivity.startActivity(this, waypoint.getGeocode());

        finish();
    }

    private class loadWaypoint extends Thread {

        @Override
        public void run() {
            try {
                waypoint = app.loadWaypoint(id);

                loadWaypointHandler.sendMessage(Message.obtain());
            } catch (Exception e) {
                Log.e("cgeowaypoint.loadWaypoint.run: " + e.toString());
            }
        }
    }

    @Override
    public void update(final IGeoData geo) {
        // nothing
    }

    private class deleteWaypointListener implements View.OnClickListener {

        public void onClick(View arg0) {
            String geocode = waypoint.getGeocode();
            cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            if (null != cache && cache.deleteWaypoint(waypoint)) {
                app.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));

                StaticMapsProvider.removeWpStaticMaps(id, geocode);

                finish();
            } else {
                showToast(res.getString(R.string.err_waypoint_delete_failed));
            }
        }
    }

    /**
     * @param view
     *            this method is also referenced from XML layout
     */
    public void goDefaultNavigation(View view) {
        if (!navigationPossible()) {
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(this, null, waypoint, null);
    }

    /**
     * Tries to navigate to the {@link cgCache} of this activity.
     */
    private void startDefaultNavigation2() {
        if (!navigationPossible()) {
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication2(this, null, waypoint, null);
    }

    private boolean navigationPossible() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return false;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (navigationPossible()) {
            menu.setHeaderTitle(res.getString(R.string.cache_menu_navigate));
            NavigationAppFactory.addMenuItems(menu, waypoint);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean handled = onOptionsItemSelected(item);
        if (handled) {
            return true;
        }
        return NavigationAppFactory.onMenuItemSelected(item, this, waypoint);
    }

    public static void startActivity(final Context context, final int waypointId) {
        Intent popupIntent = new Intent(context, cgeowaypoint.class);
        popupIntent.putExtra("waypoint", waypointId);
        context.startActivity(popupIntent);
    }
}
