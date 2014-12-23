package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ShowcaseViewBuilder;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GpsStatusProvider;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DatabaseBackupUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.Version;

import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AbstractActionBarActivity {
    @InjectView(R.id.nav_satellites) protected TextView navSatellites;
    @InjectView(R.id.filter_button_title)protected TextView filterTitle;
    @InjectView(R.id.map) protected ImageView findOnMap;
    @InjectView(R.id.search_offline) protected ImageView findByOffline;
    @InjectView(R.id.advanced_button) protected ImageView advanced;
    @InjectView(R.id.any_button) protected ImageView any;
    @InjectView(R.id.filter_button) protected ImageView filter;
    @InjectView(R.id.nearest) protected ImageView nearestView;
    @InjectView(R.id.nav_type) protected TextView navType;
    @InjectView(R.id.nav_accuracy) protected TextView navAccuracy;
    @InjectView(R.id.nav_location) protected TextView navLocation;
    @InjectView(R.id.offline_count) protected TextView countBubble;
    @InjectView(R.id.info_area) protected LinearLayout infoArea;

    public static final int SEARCH_REQUEST_CODE = 2;

    private Geopoint addCoords = null;
    private boolean initialized = false;
    private ConnectivityChangeReceiver connectivityChangeReceiver;

    private final UpdateLocation locationUpdater = new UpdateLocation();

    private final Handler updateUserInfoHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {

            // Get active connectors with login status
            final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors();

            // Update UI
            infoArea.removeAllViews();
            final LayoutInflater inflater = getLayoutInflater();

            for (final ILogin conn : loginConns) {

                final TextView connectorInfo = (TextView) inflater.inflate(R.layout.main_activity_connectorstatus, null);
                infoArea.addView(connectorInfo);

                final StringBuilder userInfo = new StringBuilder(conn.getName()).append(Formatter.SEPARATOR);
                if (conn.isLoggedIn()) {
                    userInfo.append(conn.getUserName());
                    if (conn.getCachesFound() >= 0) {
                        userInfo.append(" (").append(conn.getCachesFound()).append(')');
                    }
                    userInfo.append(Formatter.SEPARATOR);
                }
                userInfo.append(conn.getLoginStatusString());

                connectorInfo.setText(userInfo);
            }
        }
    };

    private final class ConnectivityChangeReceiver extends BroadcastReceiver {
        private boolean isConnected = Network.isNetworkConnected();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean wasConnected = isConnected;
            isConnected = Network.isNetworkConnected();
            if (isConnected && !wasConnected) {
                startBackgroundLogin();
            }
        }
    }

    private static String formatAddress(final Address address) {
        final ArrayList<String> addressParts = new ArrayList<>();

        final String countryName = address.getCountryName();
        if (countryName != null) {
            addressParts.add(countryName);
        }
        final String locality = address.getLocality();
        if (locality != null) {
            addressParts.add(locality);
        } else {
            final String adminArea = address.getAdminArea();
            if (adminArea != null) {
                addressParts.add(adminArea);
            }
        }
        return StringUtils.join(addressParts, ", ");
    }

    private final Action1<GpsStatusProvider.Status> satellitesHandler = new Action1<Status>() {
        @Override
        public void call(final Status gpsStatus) {
            if (gpsStatus.gpsEnabled) {
                navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gpsStatus.satellitesFixed + '/' + gpsStatus.satellitesVisible);
            } else {
                navSatellites.setText(res.getString(R.string.loc_gps_disabled));
            }
        }
    };

    private final Handler firstLoginHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            try {
                final StatusCode reason = (StatusCode) msg.obj;

                if (reason != null && reason != StatusCode.NO_ERROR) { //LoginFailed
                    showToast(res.getString(reason == StatusCode.MAINTENANCE ? reason.getErrorString() : R.string.err_login_failed_toast));
                }
            } catch (final Exception e) {
                Log.w("MainActivity.firstLoginHander", e);
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        // Disable the up navigation for this activity
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        setContentView(R.layout.main_activity);
        ButterKnife.inject(this);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // If we had been open already, start from the last used activity.
            finish();
            return;
        }

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

        Log.i("Starting " + getPackageName() + ' ' + Version.getVersionCode(this) + " a.k.a " + Version.getVersionName(this));

        init();

        checkShowChangelog();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.LOW_POWER),
                app.gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(satellitesHandler));
        updateUserInfoHandler.sendEmptyMessage(-1);
        if (app.hasValidLocation()) {
            locationUpdater.updateGeoData(app.currentGeo());
        }
        startBackgroundLogin();
        init();

        connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void startBackgroundLogin() {
        assert(app != null);

        final boolean mustLogin = app.mustRelog();

        for (final ILogin conn : ConnectorFactory.getActiveLiveConnectors()) {
            if (mustLogin || !conn.isLoggedIn()) {
                RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        if (mustLogin) {
                            // Properly log out from geocaching.com
                            conn.logout();
                        }
                        conn.login(firstLoginHandler, MainActivity.this);
                        updateUserInfoHandler.sendEmptyMessage(-1);
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        initialized = false;
        app.showLoginToast = true;

        super.onDestroy();
    }

    @Override
    public void onStop() {
        initialized = false;
        super.onStop();
    }

    @Override
    public void onPause() {
        initialized = false;
        unregisterReceiver(connectivityChangeReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchItem = menu.findItem(R.id.menu_gosearch);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        presentShowcase();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_pocket_queries).setVisible(Settings.isGCPremiumMember());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // this activity must handle the home navigation different than all others
                showAbout(null);
                return true;
            case R.id.menu_about:
                showAbout(null);
                return true;
            case R.id.menu_helpers:
                startActivity(new Intent(this, UsefulAppsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_history:
                startActivity(CacheListActivity.getHistoryIntent(this));
                return true;
            case R.id.menu_scan:
                startScannerApplication();
                return true;
            case R.id.menu_pocket_queries:
                if (!Settings.isGCPremiumMember()) {
                    return true;
                }
                PocketQueryList.promptForListSelection(this, new Action1<PocketQueryList>() {

                    @Override
                    public void call(final PocketQueryList pql) {
                        CacheListActivity.startActivityPocket(MainActivity.this, pql);
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startScannerApplication() {
        final IntentIntegrator integrator = new IntentIntegrator(this);
        // integrator dialog is English only, therefore localize it
        integrator.setButtonYesByID(android.R.string.yes);
        integrator.setButtonNoByID(android.R.string.no);
        integrator.setTitleByID(R.string.menu_scan_geo);
        integrator.setMessageByID(R.string.menu_scan_description);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            final String scan = scanResult.getContents();
            if (StringUtils.isBlank(scan)) {
                return;
            }
            SearchActivity.startActivityScan(scan, this);
        } else if (requestCode == SEARCH_REQUEST_CODE) {
            // SearchActivity activity returned without making a search
            if (resultCode == RESULT_CANCELED) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                if (query == null) {
                    query = "";
                }
                Dialogs.message(this, res.getString(R.string.unknown_scan) + "\n\n" + query);
            }
        }
    }

    private void setFilterTitle() {
        filterTitle.setText(Settings.getCacheType().getL10n());
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoFindOnMap(v);
            }
        });

        findByOffline.setClickable(true);
        findByOffline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoFindByOffline(v);
            }
        });
        findByOffline.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                new StoredList.UserInterface(MainActivity.this).promptForListSelection(R.string.list_title, new Action1<Integer>() {

                    @Override
                    public void call(final Integer selectedListId) {
                        Settings.saveLastList(selectedListId);
                        CacheListActivity.startActivityOffline(MainActivity.this);
                    }
                }, false, PseudoList.HISTORY_LIST.id);
                return true;
            }
        });
        findByOffline.setLongClickable(true);

        advanced.setClickable(true);
        advanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoSearch(v);
            }
        });

        any.setClickable(true);
        any.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoPoint(v);
            }
        });

        filter.setClickable(true);
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selectGlobalTypeFilter();
            }
        });
        filter.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                Settings.setCacheType(CacheType.ALL);
                setFilterTitle();
                return true;
            }
        });

        updateCacheCounter();

        setFilterTitle();
        checkRestore();
        DataStore.cleanIfNeeded(this);
    }

    protected void selectGlobalTypeFilter() {
        final List<CacheType> cacheTypes = new ArrayList<>();

        //first add the most used types
        cacheTypes.add(CacheType.ALL);
        cacheTypes.add(CacheType.TRADITIONAL);
        cacheTypes.add(CacheType.MULTI);
        cacheTypes.add(CacheType.MYSTERY);

        // then add all other cache types sorted alphabetically
        final List<CacheType> sorted = new ArrayList<>();
        sorted.addAll(Arrays.asList(CacheType.values()));
        sorted.removeAll(cacheTypes);

        Collections.sort(sorted, new Comparator<CacheType>() {

            @Override
            public int compare(final CacheType left, final CacheType right) {
                return left.getL10n().compareToIgnoreCase(right.getL10n());
            }
        });

        cacheTypes.addAll(sorted);

        int checkedItem = cacheTypes.indexOf(Settings.getCacheType());
        if (checkedItem < 0) {
            checkedItem = 0;
        }

        final String[] items = new String[cacheTypes.size()];
        for (int i = 0; i < cacheTypes.size(); i++) {
            items[i] = cacheTypes.get(i).getL10n();
        }

        final Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_filter);
        builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                final CacheType cacheType = cacheTypes.get(position);
                Settings.setCacheType(cacheType);
                setFilterTitle();
                dialog.dismiss();
            }

        });
        builder.create().show();
    }

    public void updateCacheCounter() {
        AndroidObservable.bindActivity(this, DataStore.getAllCachesCountObservable()).subscribe(new Action1<Integer>() {
            @Override
            public void call(final Integer countBubbleCnt1) {
                if (countBubbleCnt1 == 0) {
                    countBubble.setVisibility(View.GONE);
                } else {
                    countBubble.setText(Integer.toString(countBubbleCnt1));
                    countBubble.bringToFront();
                    countBubble.setVisibility(View.VISIBLE);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable throwable) {
                Log.e("Unable to add bubble count", throwable);
            }
        });
    }

    private void checkRestore() {
        if (!DataStore.isNewlyCreatedDatebase() || null == DatabaseBackupUtils.getRestoreFile()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.init_backup_restore))
                .setMessage(res.getString(R.string.init_restore_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                        DataStore.resetNewlyCreatedDatabase();
                        DatabaseBackupUtils.restoreDatabase(MainActivity.this);
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        DataStore.resetNewlyCreatedDatabase();
                    }
                })
                .create()
                .show();
    }

    private class UpdateLocation extends GeoDirHandler {

        @Override
        public void updateGeoData(final GeoData geo) {
            if (!nearestView.isClickable()) {
                nearestView.setFocusable(true);
                nearestView.setClickable(true);
                nearestView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        cgeoFindNearest(v);
                    }
                });
                nearestView.setBackgroundResource(R.drawable.main_nearby);
            }

            navType.setText(res.getString(geo.getLocationProvider().resourceId));

            if (geo.getAccuracy() >= 0) {
                final int speed = Math.round(geo.getSpeed()) * 60 * 60 / 1000;
                navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()) + Formatter.SEPARATOR + Units.getSpeed(speed));
            } else {
                navAccuracy.setText(null);
            }

            if (Settings.isShowAddress()) {
                if (addCoords == null) {
                    navLocation.setText(R.string.loc_no_addr);
                }
                if (addCoords == null || (geo.getCoords().distanceTo(addCoords) > 0.5)) {
                    final Observable<String> address = Observable.create(new OnSubscribe<String>() {
                        @Override
                        public void call(final Subscriber<? super String> subscriber) {
                            try {
                                addCoords = geo.getCoords();
                                final Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                final Geopoint coords = app.currentGeo().getCoords();
                                final List<Address> addresses = geocoder.getFromLocation(coords.getLatitude(), coords.getLongitude(), 1);
                                if (!addresses.isEmpty()) {
                                    subscriber.onNext(formatAddress(addresses.get(0)));
                                }
                                subscriber.onCompleted();
                            } catch (final Exception e) {
                                subscriber.onError(e);
                            }
                        }
                    });
                    AndroidObservable.bindActivity(MainActivity.this, address.onErrorResumeNext(Observable.just(geo.getCoords().toString())))
                            .subscribeOn(RxUtils.networkScheduler)
                            .subscribe(new Action1<String>() {
                                @Override
                                public void call(final String address) {
                                    navLocation.setText(address);
                                }
                            });
                }
            } else {
                navLocation.setText(geo.getCoords().toString());
            }
        }
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindOnMap(final View v) {
        findOnMap.setPressed(true);
        startActivity(CGeoMap.getLiveMapIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindNearest(final View v) {
        nearestView.setPressed(true);
        startActivity(CacheListActivity.getNearestIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(final View v) {
        findByOffline.setPressed(true);
        CacheListActivity.startActivityOffline(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(final View v) {
        advanced.setPressed(true);
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(final View v) {
        any.setPressed(true);
        startActivity(new Intent(this, NavigateAnyPointActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(final View v) {
        filter.setPressed(true);
        filter.performClick();
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoNavSettings(final View v) {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void checkShowChangelog() {
        // temporary workaround for #4143
        //TODO: understand and avoid if possible
        try {
            final long lastChecksum = Settings.getLastChangelogChecksum();
            final long checksum = TextUtils.checksum(getString(R.string.changelog_master) + getString(R.string.changelog_release));
            Settings.setLastChangelogChecksum(checksum);
            // don't show change log after new install...
            if (lastChecksum > 0 && lastChecksum != checksum) {
                AboutActivity.showChangeLog(this);
            }
        } catch (final Exception ex) {
            Log.e("Error checking/showing changelog!", ex);
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void showAbout(final View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public ShowcaseViewBuilder getShowcase() {
        return new ShowcaseViewBuilder(this)
                .setTarget(new ActionViewTarget(this, ActionViewTarget.Type.OVERFLOW))
                .setContent(R.string.showcase_main_title, R.string.showcase_main_text);
    }
}
