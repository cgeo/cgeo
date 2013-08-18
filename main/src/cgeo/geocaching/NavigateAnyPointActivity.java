package cgeo.geocaching;

import butterknife.InjectView;
import butterknife.Views;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class NavigateAnyPointActivity extends AbstractActivity {

    @InjectView(R.id.buttonLatitude) protected Button latButton;
    @InjectView(R.id.buttonLongitude) protected Button lonButton;
    @InjectView(R.id.current) protected Button buttonCurrent;
    @InjectView(R.id.historyList) protected ListView historyListView;
    @InjectView(R.id.distanceUnit) protected Spinner distanceUnitSelector;
    @InjectView(R.id.bearing) protected EditText bearingEditText;
    @InjectView(R.id.distance) protected EditText distanceEditText;

    private boolean changed = false;
    private List<Destination> historyOfSearchedLocations;
    private DestinationHistoryAdapter destinationHistoryAdapter;
    private TextView historyFooter;

    private static final int CONTEXT_MENU_NAVIGATE = 1;
    private static final int CONTEXT_MENU_DELETE_WAYPOINT = 2;
    private static final int CONTEXT_MENU_EDIT_WAYPOINT = 3;

    private int contextMenuItemPosition;

    private String distanceUnit = StringUtils.EMPTY;

    protected static class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.simple_way_point_longitude) protected TextView longitude;
        @InjectView(R.id.simple_way_point_latitude) protected TextView latitude;
        @InjectView(R.id.date) protected TextView date;

        public ViewHolder(View rowView) {
            super(rowView);
        }
    }

    private static class DestinationHistoryAdapter extends ArrayAdapter<Destination> {
        private LayoutInflater inflater = null;

        public DestinationHistoryAdapter(Context context,
                List<Destination> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rowView = convertView;

            ViewHolder viewHolder;
            if (rowView == null) {
                rowView = getInflater().inflate(R.layout.simple_way_point, null);
                viewHolder = new ViewHolder(rowView);
            }
            else {
                viewHolder = (ViewHolder) rowView.getTag();
            }

            fillViewHolder(viewHolder, getItem(position));

            return rowView;
        }

        private static void fillViewHolder(ViewHolder viewHolder, Destination loc) {
            String lonString = loc.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE);
            String latString = loc.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE);

            viewHolder.longitude.setText(lonString);
            viewHolder.latitude.setText(latString);
            viewHolder.date.setText(Formatter.formatShortDateTime(loc.getDate()));
        }

        private LayoutInflater getInflater() {
            if (inflater == null) {
                inflater = ((Activity) getContext()).getLayoutInflater();
            }

            return inflater;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.navigateanypoint_activity);
        Views.inject(this);

        createHistoryView();
        init();
    }

    private void createHistoryView() {
        final View pointControls = getLayoutInflater().inflate(R.layout.navigateanypoint_header, null);
        historyListView.addHeaderView(pointControls, null, false);

        // inject a second time to also find the dynamically expanded views above
        Views.inject(this);

        if (getHistoryOfSearchedLocations().isEmpty()) {
            historyListView.addFooterView(getEmptyHistoryFooter(), null, false);
        }

        historyListView.setAdapter(getDestionationHistoryAdapter());
        historyListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                final Object selection = arg0.getItemAtPosition(arg2);
                if (selection instanceof Destination) {
                    navigateTo(((Destination) selection).getCoords());
                }
            }
        });

        historyListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                menu.add(Menu.NONE, CONTEXT_MENU_NAVIGATE, Menu.NONE, res.getString(R.string.cache_menu_navigate));
                menu.add(Menu.NONE, CONTEXT_MENU_EDIT_WAYPOINT, Menu.NONE, R.string.waypoint_edit);
                menu.add(Menu.NONE, CONTEXT_MENU_DELETE_WAYPOINT, Menu.NONE, R.string.waypoint_delete);
            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int position = (null != menuInfo) ? menuInfo.position : contextMenuItemPosition;
        Object destination = historyListView.getItemAtPosition(position);

        switch (item.getItemId()) {
            case CONTEXT_MENU_NAVIGATE:
                contextMenuItemPosition = position;
                if (destination instanceof Destination) {
                    NavigationAppFactory.showNavigationMenu(this, null, null, ((Destination) destination).getCoords());
                    return true;
                }
                break;

            case CONTEXT_MENU_DELETE_WAYPOINT:
                if (destination instanceof Destination) {
                    removeFromHistory((Destination) destination);
                }
                return true;

            case CONTEXT_MENU_EDIT_WAYPOINT:
                if (destination instanceof Destination) {
                    final Geopoint gp = ((Destination) destination).getCoords();
                    latButton.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    lonButton.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
                return true;
            default:
        }

        return super.onContextItemSelected(item);
    }

    private TextView getEmptyHistoryFooter() {
        if (historyFooter == null) {
            historyFooter = (TextView) getLayoutInflater().inflate(R.layout.cacheslist_footer, null);
            historyFooter.setText(R.string.search_history_empty);
        }
        return historyFooter;
    }

    private DestinationHistoryAdapter getDestionationHistoryAdapter() {
        if (destinationHistoryAdapter == null) {
            destinationHistoryAdapter = new DestinationHistoryAdapter(this, getHistoryOfSearchedLocations());
        }
        return destinationHistoryAdapter;
    }

    private List<Destination> getHistoryOfSearchedLocations() {
        if (historyOfSearchedLocations == null) {
            // Load from database
            historyOfSearchedLocations = cgData.loadHistoryOfSearchedLocations();
        }

        return historyOfSearchedLocations;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        geoDirHandler.startGeo();
        init();
    }

    @Override
    public void onPause() {
        geoDirHandler.stopGeo();
        super.onPause();
    }

    private void init() {
        latButton.setOnClickListener(new CoordDialogListener());
        lonButton.setOnClickListener(new CoordDialogListener());

        final Geopoint coords = Settings.getAnyCoordinates();
        if (coords != null) {
            latButton.setText(coords.format(GeopointFormatter.Format.LAT_DECMINUTE));
            lonButton.setText(coords.format(GeopointFormatter.Format.LON_DECMINUTE));
        }

        buttonCurrent.setOnClickListener(new CurrentListener());

        getDestionationHistoryAdapter().notifyDataSetChanged();
        disableSuggestions(distanceEditText);

        initializeDistanceUnitSelector();
    }

    private void initializeDistanceUnitSelector() {
        if (StringUtils.isBlank(distanceUnit)) {
            if (Settings.isUseImperialUnits()) {
                distanceUnitSelector.setSelection(2); // ft
                distanceUnit = res.getStringArray(R.array.distance_units)[2];
            } else {
                distanceUnitSelector.setSelection(0); // m
                distanceUnit = res.getStringArray(R.array.distance_units)[0];
            }
        }

        distanceUnitSelector.setOnItemSelectedListener(new ChangeDistanceUnit(this));
    }

    private class CoordDialogListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            Geopoint gp = null;
            if (latButton.getText().length() > 0 && lonButton.getText().length() > 0) {
                gp = new Geopoint(latButton.getText().toString() + " " + lonButton.getText().toString());
            }
            CoordinatesInputDialog coordsDialog = new CoordinatesInputDialog(NavigateAnyPointActivity.this, null, gp, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new CoordinatesInputDialog.CoordinateUpdate() {
                @Override
                public void update(Geopoint gp) {
                    latButton.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    lonButton.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                    changed = true;
                }
            });
            coordsDialog.show();
        }
    }

    private static class ChangeDistanceUnit implements OnItemSelectedListener {

        private ChangeDistanceUnit(NavigateAnyPointActivity unitView) {
            this.unitView = unitView;
        }

        private NavigateAnyPointActivity unitView;

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            unitView.distanceUnit = (String) arg0.getItemAtPosition(arg2);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigate_any_point_activity_options, menu);
        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            boolean visible = getDestination() != null;
            menu.findItem(R.id.menu_navigate).setVisible(visible);
            menu.findItem(R.id.menu_default_navigation).setVisible(visible);
            menu.findItem(R.id.menu_caches_around).setVisible(visible);

            menu.findItem(R.id.menu_clear_history).setEnabled(!getHistoryOfSearchedLocations().isEmpty());
        } catch (Exception e) {
            // nothing
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuItem = item.getItemId();

        final Geopoint coords = getDestination();

        if (coords != null) {
            addToHistory(coords);
        }

        switch (menuItem) {
            case R.id.menu_default_navigation:
                navigateTo();
                return true;

            case R.id.menu_caches_around:
                cachesAround();
                return true;

            case R.id.menu_clear_history:
                clearHistory();
                return true;

            case R.id.menu_navigate:
                NavigationAppFactory.showNavigationMenu(this, null, null, coords);
                return true;
            default:
                return false;
        }
    }

    private void addToHistory(final Geopoint coords) {
        // Add locations to history
        final Destination loc = new Destination(coords);

        if (!getHistoryOfSearchedLocations().contains(loc)) {
            getHistoryOfSearchedLocations().add(0, loc);

            // Save location
            cgData.saveSearchedDestination(loc);

            // Ensure to remove the footer
            historyListView.removeFooterView(getEmptyHistoryFooter());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    destinationHistoryAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void removeFromHistory(Destination destination) {
        if (getHistoryOfSearchedLocations().contains(destination)) {
            getHistoryOfSearchedLocations().remove(destination);

            // Save
            cgData.removeSearchedDestination(destination);

            if (getHistoryOfSearchedLocations().isEmpty()) {
                if (historyListView.getFooterViewsCount() == 0) {
                    historyListView.addFooterView(getEmptyHistoryFooter());
                }
            }

            getDestionationHistoryAdapter().notifyDataSetChanged();

            showToast(res.getString(R.string.search_remove_destination));
        }
    }

    private void clearHistory() {
        if (!getHistoryOfSearchedLocations().isEmpty()) {
            getHistoryOfSearchedLocations().clear();

            // Save
            cgData.clearSearchedDestinations();

            if (historyListView.getFooterViewsCount() == 0) {
                historyListView.addFooterView(getEmptyHistoryFooter());
            }

            getDestionationHistoryAdapter().notifyDataSetChanged();

            showToast(res.getString(R.string.search_history_cleared));
        }
    }

    private void navigateTo() {
        navigateTo(getDestination());
    }

    private void navigateTo(Geopoint geopoint) {
        NavigationAppFactory.startDefaultNavigationApplication(1, this, geopoint);
    }

    private void cachesAround() {
        final Geopoint coords = getDestination();

        if (coords == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        cgeocaches.startActivityCoordinates(this, coords);

        finish();
    }

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                latButton.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                lonButton.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
            } catch (final Exception e) {
                Log.w("Failed to update location.");
            }
        }
    };

    private class CurrentListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final Geopoint coords = app.currentGeo().getCoords();
            if (coords == null) {
                showToast(res.getString(R.string.err_point_unknown_position));
                return;
            }

            latButton.setText(coords.format(GeopointFormatter.Format.LAT_DECMINUTE));
            lonButton.setText(coords.format(GeopointFormatter.Format.LON_DECMINUTE));

            changed = false;
        }
    }

    private Geopoint getDestination() {
        String bearingText = bearingEditText.getText().toString();
        // combine distance from EditText and distanceUnit saved from Spinner
        String distanceText = distanceEditText.getText().toString() + distanceUnit;
        String latText = latButton.getText().toString();
        String lonText = lonButton.getText().toString();

        if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
            showToast(res.getString(R.string.err_point_no_position_given));
            return null;
        }

        // get base coordinates
        Geopoint coords;
        if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
            try {
                coords = new Geopoint(latText, lonText);
            } catch (Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
                return null;
            }
        } else {
            if (app.currentGeo().getCoords() == null) {
                showToast(res.getString(R.string.err_point_curr_position_unavailable));
                return null;
            }

            coords = app.currentGeo().getCoords();
        }

        // apply projection
        if (coords != null && StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
            // bearing & distance
            double bearing;
            try {
                bearing = Double.parseDouble(bearingText);
            } catch (NumberFormatException e) {
                helpDialog(res.getString(R.string.err_point_bear_and_dist_title), res.getString(R.string.err_point_bear_and_dist));
                return null;
            }

            double distance;
            try {
                distance = DistanceParser.parseDistance(distanceText,
                        !Settings.isUseImperialUnits());
            } catch (NumberFormatException e) {
                showToast(res.getString(R.string.err_parse_dist));
                return null;
            }

            coords = coords.project(bearing, distance);
        }

        if (coords != null) {
            saveCoords(coords);
        }

        return coords;
    }

    private void saveCoords(final Geopoint coords) {
        if (!changed) {
            return;
        }
        Settings.setAnyCoordinates(coords);
    }
}
