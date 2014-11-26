package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.location.DistanceParser;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.NavigationActionProvider;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action0;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
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

public class NavigateAnyPointActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate, INavigationSource {

    @InjectView(R.id.historyList) protected ListView historyListView;

    // list header fields are optional, due to being expanded later than the list itself
    @Optional @InjectView(R.id.buttonLatitude) protected Button latButton;
    @Optional @InjectView(R.id.buttonLongitude) protected Button lonButton;
    @Optional @InjectView(R.id.distance) protected EditText distanceEditText;
    @Optional @InjectView(R.id.distanceUnit) protected Spinner distanceUnitSelector;
    @Optional @InjectView(R.id.current) protected Button buttonCurrent;
    @Optional @InjectView(R.id.bearing) protected EditText bearingEditText;

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

        public ViewHolder(final View rowView) {
            super(rowView);
        }
    }

    private static class DestinationHistoryAdapter extends ArrayAdapter<Destination> {
        private LayoutInflater inflater = null;

        public DestinationHistoryAdapter(final Context context,
                final List<Destination> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rowView = convertView;

            final ViewHolder viewHolder;
            if (rowView == null) {
                rowView = getInflater().inflate(R.layout.simple_way_point, parent, false);
                viewHolder = new ViewHolder(rowView);
            }
            else {
                viewHolder = (ViewHolder) rowView.getTag();
            }

            fillViewHolder(viewHolder, getItem(position));

            return rowView;
        }

        private static void fillViewHolder(final ViewHolder viewHolder, final Destination loc) {
            final String lonString = loc.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE);
            final String latString = loc.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE);

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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.navigateanypoint_activity);
        ButterKnife.inject(this);

        createHistoryView();
        init();
    }

    private void createHistoryView() {
        final View pointControls = getLayoutInflater().inflate(R.layout.navigateanypoint_header, historyListView, false);
        historyListView.addHeaderView(pointControls, null, false);

        // inject a second time to also find the dynamically expanded views above
        ButterKnife.inject(this);

        if (getHistoryOfSearchedLocations().isEmpty()) {
            historyListView.addFooterView(getEmptyHistoryFooter(), null, false);
        }

        historyListView.setAdapter(getDestionationHistoryAdapter());
        historyListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2,
                    final long arg3) {
                final Object selection = arg0.getItemAtPosition(arg2);
                if (selection instanceof Destination) {
                    navigateTo(((Destination) selection).getCoords());
                }
            }
        });

        historyListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(final ContextMenu menu, final View v,
                    final ContextMenuInfo menuInfo) {
                menu.add(Menu.NONE, CONTEXT_MENU_NAVIGATE, Menu.NONE, res.getString(R.string.cache_menu_navigate));
                menu.add(Menu.NONE, CONTEXT_MENU_EDIT_WAYPOINT, Menu.NONE, R.string.waypoint_edit);
                menu.add(Menu.NONE, CONTEXT_MENU_DELETE_WAYPOINT, Menu.NONE, R.string.waypoint_delete);
            }
        });
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int position = (null != menuInfo) ? menuInfo.position : contextMenuItemPosition;
        final Object destination = historyListView.getItemAtPosition(position);

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
            historyFooter = (TextView) getLayoutInflater().inflate(R.layout.cacheslist_footer, historyListView, false);
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
            historyOfSearchedLocations = DataStore.loadHistoryOfSearchedLocations();
        }

        return historyOfSearchedLocations;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA));
        init();
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
            if (Settings.useImperialUnits()) {
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
        public void onClick(final View arg0) {
            Geopoint gp = null;
            if (latButton.getText().length() > 0 && lonButton.getText().length() > 0) {
                gp = new Geopoint(latButton.getText().toString() + " " + lonButton.getText().toString());
            }
            final CoordinatesInputDialog coordsDialog = CoordinatesInputDialog.getInstance(null, gp, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.show(getSupportFragmentManager(),"wpedit_dialog");
        }

    }
    @Override
    public void updateCoordinates(final Geopoint gp) {
        latButton.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        lonButton.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
        changed = true;
    }

    private static class ChangeDistanceUnit implements OnItemSelectedListener {

        private ChangeDistanceUnit(final NavigateAnyPointActivity unitView) {
            this.unitView = unitView;
        }

        private final NavigateAnyPointActivity unitView;

        @Override
        public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2,
                final long arg3) {
            unitView.distanceUnit = (String) arg0.getItemAtPosition(arg2);
        }

        @Override
        public void onNothingSelected(final AdapterView<?> arg0) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.navigate_any_point_activity_options, menu);
        final MenuItem menuItem = menu.findItem(R.id.menu_default_navigation);
        menuItem.setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
        final NavigationActionProvider navAction = (NavigationActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if (navAction != null) {
            navAction.setNavigationSource(this);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            final boolean visible = getDestination() != null;
            menu.findItem(R.id.menu_navigate).setVisible(visible);
            menu.findItem(R.id.menu_default_navigation).setVisible(visible);
            menu.findItem(R.id.menu_caches_around).setVisible(visible);

            menu.findItem(R.id.menu_clear_history).setVisible(!getHistoryOfSearchedLocations().isEmpty());
        } catch (final RuntimeException ignored) {
            // nothing
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int menuItem = item.getItemId();
        final Geopoint coords = getDestinationAndAddToHistory();
        switch (menuItem) {
            case R.id.menu_default_navigation:
                navigateTo(coords);
                return true;

            case R.id.menu_caches_around:
                cachesAround(coords);
                return true;

            case R.id.menu_clear_history:
                clearHistory();
                return true;

            case R.id.menu_navigate:
                NavigationAppFactory.showNavigationMenu(this, null, null, coords);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Geopoint getDestinationAndAddToHistory() {
        final Geopoint coords = getDestination();
        addToHistory(coords);
        return coords;
    }

    private void addToHistory(@Nullable final Geopoint coords) {
        if (coords == null) {
            return;
        }

        // Add locations to history
        final Destination loc = new Destination(coords);

        if (!getHistoryOfSearchedLocations().contains(loc)) {
            getHistoryOfSearchedLocations().add(0, loc);
            RxUtils.andThenOnUi(Schedulers.io(), new Action0() {
                @Override
                public void call() {
                    // Save location
                    DataStore.saveSearchedDestination(loc);
                }
            }, new Action0() {
                @Override
                public void call() {
                    // Ensure to remove the footer
                    historyListView.removeFooterView(getEmptyHistoryFooter());
                    destinationHistoryAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void removeFromHistory(final Destination destination) {
        if (getHistoryOfSearchedLocations().contains(destination)) {
            getHistoryOfSearchedLocations().remove(destination);

            // Save
            DataStore.removeSearchedDestination(destination);

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
            DataStore.clearSearchedDestinations();

            if (historyListView.getFooterViewsCount() == 0) {
                historyListView.addFooterView(getEmptyHistoryFooter());
            }

            getDestionationHistoryAdapter().notifyDataSetChanged();

            showToast(res.getString(R.string.search_history_cleared));
        }
    }

    private void navigateTo(final Geopoint coords) {
        if (coords == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(1, this, coords);
    }

    private void cachesAround(final Geopoint coords) {
        if (coords == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        CacheListActivity.startActivityCoordinates(this, coords);

        finish();
    }

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
            try {
                latButton.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                lonButton.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e);
            }
        }
    };

    private class CurrentListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final Geopoint coords = app.currentGeo().getCoords();
            latButton.setText(coords.format(GeopointFormatter.Format.LAT_DECMINUTE));
            lonButton.setText(coords.format(GeopointFormatter.Format.LON_DECMINUTE));
            changed = false;
        }
    }

    private Geopoint getDestination() {
        final String bearingText = bearingEditText.getText().toString();
        // combine distance from EditText and distanceUnit saved from Spinner
        final String distanceText = distanceEditText.getText().toString() + distanceUnit;
        final String latText = latButton.getText().toString();
        final String lonText = lonButton.getText().toString();

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
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
                return null;
            }
        } else {
            coords = app.currentGeo().getCoords();
        }

        // apply projection
        if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
            // bearing & distance
            final double bearing;
            try {
                bearing = Double.parseDouble(bearingText);
            } catch (final NumberFormatException ignored) {
                Dialogs.message(this, R.string.err_point_bear_and_dist_title, R.string.err_point_bear_and_dist);
                return null;
            }

            final double distance;
            try {
                distance = DistanceParser.parseDistance(distanceText,
                        !Settings.useImperialUnits());
            } catch (final NumberFormatException ignored) {
                showToast(res.getString(R.string.err_parse_dist));
                return null;
            }

            coords = coords.project(bearing, distance);
        }

        saveCoords(coords);

        return coords;
    }

    private void saveCoords(final Geopoint coords) {
        if (!changed) {
            return;
        }
        Settings.setAnyCoordinates(coords);
    }

    @Override
    public void startDefaultNavigation() {
        navigateTo(getDestinationAndAddToHistory());
    }

    @Override
    public void startDefaultNavigation2() {
        NavigationAppFactory.startDefaultNavigationApplication(2, this, getDestinationAndAddToHistory());
    }
}
