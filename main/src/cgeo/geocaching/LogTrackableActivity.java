package cgeo.geocaching;

import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.AbstractTrackableLoggingManager;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.LogEntry;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.TrackableLog;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.search.AutoCompleteAdapter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog.CoordinateUpdate;
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.DateDialog.DateDialogParent;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.TimeDialog;
import cgeo.geocaching.ui.dialog.TimeDialog.TimeDialogParent;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import android.R.layout;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class LogTrackableActivity extends AbstractLoggingActivity implements DateDialogParent, TimeDialogParent, CoordinateUpdate, LoaderManager.LoaderCallbacks<List<LogTypeTrackable>> {

    @BindView(R.id.type) protected Button typeButton;
    @BindView(R.id.date) protected Button dateButton;
    @BindView(R.id.time) protected Button timeButton;
    @BindView(R.id.geocode) protected AutoCompleteTextView geocodeEditText;
    @BindView(R.id.coordinates) protected Button coordinatesButton;
    @BindView(R.id.tracking) protected EditText trackingEditText;
    @BindView(R.id.log) protected EditText logEditText;
    @BindView(R.id.tweet) protected CheckBox tweetCheck;
    @BindView(R.id.tweet_box) protected LinearLayout tweetBox;

    private CompositeSubscription createSubscriptions;

    private List<LogTypeTrackable> possibleLogTypesTrackable = new ArrayList<>();
    private String geocode = null;
    private Geopoint geopoint;
    private Geocache geocache = new Geocache();
    /**
     * As long as we still fetch the current state of the trackable from the Internet, the user cannot yet send a log.
     */
    private boolean postReady = true;
    private Calendar date = Calendar.getInstance();
    private LogTypeTrackable typeSelected = LogTypeTrackable.getById(Settings.getTrackableAction());
    private Trackable trackable;
    private TrackableBrand brand;
    String trackingCode;

    TrackableConnector connector;
    private AbstractTrackableLoggingManager loggingManager;

    /**
     * How many times the warning popup for geocode not set should be displayed
     */
    public static final int MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE = 3;

    public static final int LOG_TRACKABLE = 1;

    @Override
    public Loader<List<LogTypeTrackable>> onCreateLoader(final int id, final Bundle bundle) {
        showProgress(true);

        if (id == Loaders.LOGGING_TRAVELBUG.getLoaderId()) {
            loggingManager.setGuid(trackable.getGuid());
        }

        return loggingManager;
    }

    @Override
    public void onLoadFinished(final Loader<List<LogTypeTrackable>> listLoader, final List<LogTypeTrackable> logTypesTrackable) {

        if (CollectionUtils.isNotEmpty(logTypesTrackable)) {
            possibleLogTypesTrackable.clear();
            possibleLogTypesTrackable.addAll(logTypesTrackable);
        }

        if (logTypesTrackable != null && !logTypesTrackable.contains(typeSelected) && !logTypesTrackable.isEmpty()) {
            setType(logTypesTrackable.get(0));
            showToast(res.getString(R.string.info_log_type_changed));
        }

        postReady = loggingManager.postReady(); // we're done, user can post log

        showProgress(false);
    }

    @Override
    public void onLoaderReset(final Loader<List<LogTypeTrackable>> listLoader) {
        // nothing
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.logtrackable_activity);
        ButterKnife.bind(this);
        createSubscriptions = new CompositeSubscription();

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = AndroidBeam.getUri(getIntent());

        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);

            // Load geocache if we can
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_GEOCACHE))) {
                final Geocache tmpGeocache = DataStore.loadCache(extras.getString(Intents.EXTRA_GEOCACHE), LoadFlags.LOAD_CACHE_OR_DB);
                if (tmpGeocache != null) {
                    geocache = tmpGeocache;
                }
            }
            // Load Tracking Code
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_TRACKING_CODE))) {
                trackingCode = extras.getString(Intents.EXTRA_TRACKING_CODE);
            }
        }

        // try to get data from URI
        if (geocode == null && uri != null) {
            geocode = ConnectorFactory.getTrackableFromURL(uri.toString());
        }

        // try to get data from URI from a potential tracking Code
        if (geocode == null && uri != null) {
            final TrackableTrackingCode tbTrackingCode = ConnectorFactory.getTrackableTrackingCodeFromURL(uri.toString());

            if (!tbTrackingCode.isEmpty()) {
                brand = tbTrackingCode.brand;
                geocode = tbTrackingCode.trackingCode;
            }
        }

        // no given data
        if (geocode == null) {
            showToast(res.getString(R.string.err_tb_display));
            finish();
            return;
        }

        refreshTrackable();
    }

    private void refreshTrackable() {
        showProgress(true);

        // create trackable connector
        connector = ConnectorFactory.getTrackableConnector(geocode, brand);
        loggingManager = connector.getTrackableLoggingManager(this);

        if (loggingManager == null) {
            showToast(res.getString(R.string.err_tb_not_loggable));
            finish();
        }

        // Initialize the UI
        init();

        createSubscriptions.add(AppObservable.bindActivity(this, ConnectorFactory.loadTrackable(geocode, null, null, brand)).singleOrDefault(null).subscribe(new Action1<Trackable>() {
            @Override
            public void call(final Trackable newTrackable) {
                if (newTrackable != null && trackingCode != null) {
                    newTrackable.setTrackingcode(trackingCode);
                }
                trackable = newTrackable;
                // Start loading in background
                getSupportLoaderManager().initLoader(connector.getTrackableLoggingManagerLoaderId(), null, LogTrackableActivity.this).forceLoad();
                displayTrackable();
            }
        }));
    }

    private void displayTrackable() {
        if (trackable == null) {
            Log.e("LogTrackableActivity.onCreate, cannot load trackable: " + geocode);
            showProgress(false);

            if (StringUtils.isNotBlank(geocode)) {
                showToast(res.getString(R.string.err_tb_find) + ' ' + geocode + '.');
            } else {
                showToast(res.getString(R.string.err_tb_find_that));
            }

            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // We're in LogTrackableActivity, so trackable must be loggable ;)
        if (!trackable.isLoggable()) {
            showProgress(false);
            showToast(res.getString(R.string.err_tb_not_loggable));
            finish();
            return;
        }

        setTitle(res.getString(R.string.trackable_touch) + ": " + StringUtils.defaultIfBlank(trackable.getGeocode(), trackable.getName()));

        // Display tracking code if we have, and move cursor next
        if (trackingCode != null) {
            trackingEditText.setText(trackingCode);
            Dialogs.moveCursorToEnd(trackingEditText);
        }
        init();

        showProgress(false);

        requestKeyboardForLogging();
    }

    @Override
    protected void requestKeyboardForLogging() {
        if (StringUtils.isBlank(trackingEditText.getText())) {
            new Keyboard(this).show(trackingEditText);
        } else {
            super.requestKeyboardForLogging();
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.type) {
            for (final LogTypeTrackable typeOne : possibleLogTypesTrackable) {
                menu.add(viewId, typeOne.id, 0, typeOne.getLabel());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final int group = item.getGroupId();
        final int id = item.getItemId();

        if (group == R.id.type) {
            setType(LogTypeTrackable.getById(id));

            return true;
        }

        return false;
    }

    private void init() {
        registerForContextMenu(typeButton);
        typeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                openContextMenu(view);
            }
        });

        setType(typeSelected);
        dateButton.setOnClickListener(new DateListener());
        setDate(date);

        // show/hide Time selector
        if (loggingManager.canLogTime()) {
            timeButton.setOnClickListener(new TimeListener());
            setTime(date);
            timeButton.setVisibility(View.VISIBLE);
        } else {
            timeButton.setVisibility(View.GONE);
        }

        // Register Coordinates Listener
        if (loggingManager.canLogCoordinates()) {
            geocodeEditText.setOnFocusChangeListener(new LoadGeocacheListener());
            geocodeEditText.setText(geocache.getGeocode());
            updateCoordinates(geocache.getCoords());
            coordinatesButton.setOnClickListener(new CoordinatesListener());
        }

        initTwitter();

        if (CollectionUtils.isEmpty(possibleLogTypesTrackable)) {
            possibleLogTypesTrackable = Trackable.getPossibleLogTypes();
        }

        disableSuggestions(trackingEditText);
        initGeocodeSuggestions();
    }

    /**
     * Link the geocodeEditText to the SuggestionsGeocode.
     */
    private void initGeocodeSuggestions() {
        geocodeEditText.setAdapter(new AutoCompleteAdapter(geocodeEditText.getContext(), layout.simple_dropdown_item_1line, new Func1<String, String[]>() {

            @Override
            public String[] call(final String input) {
                return DataStore.getSuggestionsGeocode(input);
            }
        }));
    }

    @Override
    public void setDate(final Calendar dateIn) {
        date = dateIn;
        dateButton.setText(Formatter.formatShortDateVerbally(date.getTime().getTime()));
    }

    @Override
    public void setTime(final Calendar dateIn) {
        date = dateIn;
        timeButton.setText(Formatter.formatTime(date.getTime().getTime()));
    }

    public void setType(final LogTypeTrackable type) {
        typeSelected = type;
        typeButton.setText(typeSelected.getLabel());

        // show/hide Tracking Code Field for note type
        if (typeSelected != LogTypeTrackable.NOTE || loggingManager.isTrackingCodeNeededToPostNote()) {
            trackingEditText.setVisibility(View.VISIBLE);
            // Request focus if field is empty
            if (StringUtils.isBlank(trackingEditText.getText())) {
                trackingEditText.requestFocus();
            }
        } else {
            trackingEditText.setVisibility(View.GONE);
        }

        // show/hide Coordinate fields as Trackable needs
        if (LogTypeTrackable.isCoordinatesNeeded(typeSelected) && loggingManager.canLogCoordinates()) {
            geocodeEditText.setVisibility(View.VISIBLE);
            coordinatesButton.setVisibility(View.VISIBLE);
            // Request focus if field is empty
            if (StringUtils.isBlank(geocodeEditText.getText())) {
                geocodeEditText.requestFocus();
            }
        } else {
            geocodeEditText.setVisibility(View.GONE);
            coordinatesButton.setVisibility(View.GONE);
        }
    }

    private void initTwitter() {
        tweetCheck.setChecked(true);
        if (Settings.isUseTwitter() && Settings.isTwitterLoginValid()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    @Override
    public void updateCoordinates(final Geopoint geopointIn) {
        if (geopointIn == null) {
            return;
        }
        geopoint = geopointIn;
        coordinatesButton.setText(geopoint.toString());
        geocache.setCoords(geopoint);
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final DateDialog dateDialog = DateDialog.getInstance(date);
            dateDialog.setCancelable(true);
            dateDialog.show(getSupportFragmentManager(), "date_dialog");
        }
    }

    private class TimeListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            final TimeDialog timeDialog = TimeDialog.getInstance(date);
            timeDialog.setCancelable(true);
            timeDialog.show(getSupportFragmentManager(), "time_dialog");
        }
    }

    private class CoordinatesListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            final CoordinatesInputDialog coordinatesDialog = CoordinatesInputDialog.getInstance(geocache, geopoint);
            coordinatesDialog.setCancelable(true);
            coordinatesDialog.show(getSupportFragmentManager(), "coordinates_dialog");
        }
    }

    // React when changing geocode
    private class LoadGeocacheListener implements OnFocusChangeListener {
        @Override
        public void onFocusChange(final View view, final boolean hasFocus) {
            if (!hasFocus && StringUtils.isNotBlank(geocodeEditText.getText())) {
                final Geocache tmpGeocache = DataStore.loadCache(geocodeEditText.getText().toString(), LoadFlags.LOAD_CACHE_OR_DB);
                if (tmpGeocache == null) {
                    geocache.setGeocode(geocodeEditText.getText().toString());
                } else {
                    geocache = tmpGeocache;
                    updateCoordinates(geocache.getCoords());
                }
            }
        }
    }

    private class Poster extends AsyncTaskWithProgress<String, StatusCode> {

        Poster(final Activity activity, final String progressMessage) {
            super(activity, null, progressMessage, true);
        }

        @Override
        protected StatusCode doInBackgroundInternal(final String[] params) {
            final String logMsg = params[0];
            try {
                // Set selected action
                final TrackableLog trackableLog = new TrackableLog(trackable.getGeocode(), trackable.getTrackingcode(), trackable.getName(), 0, 0, trackable.getBrand());
                trackableLog.setAction(typeSelected);
                // Real call to post log
                final LogResult logResult = loggingManager.postLog(geocache, trackableLog, date, logMsg);

                // Now posting tweet if log is OK
                if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                    addLocalTrackableLog(logMsg);
                    if (tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                        // TODO oldLogType as a temp workaround...
                        final LogEntry logNow = new LogEntry.Builder()
                                .setDate(date.getTimeInMillis())
                                .setLogType(typeSelected.oldLogtype)
                                .setLog(logMsg)
                                .build();
                        Twitter.postTweetTrackable(trackable.getGeocode(), logNow);
                    }
                }
                // Display errors to the user
                if (StringUtils.isNoneEmpty(logResult.getLogId())) {
                    showToast(logResult.getLogId());
                }

                // Return request status
                return logResult.getPostLogResult();
            } catch (final RuntimeException e) {
                Log.e("LogTrackableActivity.Poster.doInBackgroundInternal", e);
            }
            return StatusCode.LOG_POST_ERROR;
        }

        @Override
        protected void onPostExecuteInternal(final StatusCode status) {
            if (status == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                finish();
            } else if (status == StatusCode.LOG_SAVED) {
                // is this part of code really reachable? Didn't see StatusCode.LOG_SAVED in postLog()
                showToast(res.getString(R.string.info_log_saved));
                finish();
            } else {
                showToast(status.getErrorString(res));
            }
        }

        /**
         * Adds the new log to the list of log entries for this trackable to be able to show it in the trackable
         * activity.
         *
         *
         */
        private void addLocalTrackableLog(final String logText) {
            // TODO create a LogTrackableEntry. For now use "oldLogtype" as a temporary migration path
            final LogEntry logEntry = new LogEntry.Builder()
                    .setDate(date.getTimeInMillis())
                    .setLogType(typeSelected.oldLogtype)
                    .setLog(logText)
                    .build();
            final List<LogEntry> modifiedLogs = new ArrayList<>(trackable.getLogs());
            modifiedLogs.add(0, logEntry);
            trackable.setLogs(modifiedLogs);
            DataStore.saveTrackable(trackable);
        }

    }

    public static Intent getIntent(final Context context, final Trackable trackable) {
        final Intent logTouchIntent = new Intent(context, LogTrackableActivity.class);
        logTouchIntent.putExtra(Intents.EXTRA_GEOCODE, trackable.getGeocode());
        logTouchIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.getTrackingcode());
        return logTouchIntent;
    }

    public static Intent getIntent(final Context context, final Trackable trackable, final String geocache) {
        final Intent logTouchIntent = getIntent(context, trackable);
        logTouchIntent.putExtra(Intents.EXTRA_GEOCACHE, geocache);
        return logTouchIntent;
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(trackable, null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_send:
                if (connector.isRegistered()) {
                    sendLog();
                } else {
                    // Redirect user to concerned connector settings
                    Dialogs.confirmYesNo(this, res.getString(R.string.settings_title_open_settings), res.getString(R.string.err_trackable_log_not_anonymous, trackable.getBrand().getLabel(), connector.getServiceTitle()), new OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            if (connector.getPreferenceActivity() > 0) {
                                SettingsActivity.openForScreen(connector.getPreferenceActivity(), LogTrackableActivity.this);
                            } else {
                                showToast(res.getString(R.string.err_trackable_no_preference_activity, connector.getServiceTitle()));
                            }
                        }
                    });
                }
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Do form validation then post the Log
     */
    private void sendLog() {
        // Can logging?
        if (!postReady) {
            showToast(res.getString(R.string.log_post_not_possible));
            return;
        }

        // Check Tracking Code existence
        if (loggingManager.isTrackingCodeNeededToPostNote() && trackingEditText.getText().toString().isEmpty()) {
            showToast(res.getString(R.string.err_log_post_missing_tracking_code));
            return;
        }
        trackable.setTrackingcode(trackingEditText.getText().toString());

        // Check params for trackables needing coordinates
        if (loggingManager.canLogCoordinates() && LogTypeTrackable.isCoordinatesNeeded(typeSelected) && geopoint == null) {
            showToast(res.getString(R.string.err_log_post_missing_coordinates));
            return;
        }

        // Some Trackable connectors recommend logging with a Geocode.
        // Note: Currently, counter is shared between all connectors recommending Geocode.
        if (LogTypeTrackable.isCoordinatesNeeded(typeSelected) && loggingManager.canLogCoordinates() &&
                connector.recommendLogWithGeocode() && geocodeEditText.getText().toString().isEmpty() &&
                Settings.getLogTrackableWithoutGeocodeShowCount() < MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE) {
            new LogTrackableWithoutGeocodeBuilder().create(this).show();
        } else {
            postLog();
        }
    }

    /**
     * Post Log in Background
     */
    private void postLog() {
        new Poster(this, res.getString(R.string.log_saving)).execute(logEditText.getText().toString());
        Settings.setTrackableAction(typeSelected.id);
        Settings.setLastTrackableLog(logEditText.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        for (final LogTemplate template : LogTemplateProvider.getTemplatesWithoutSignature()) {
            if (template.getTemplateString().equals("NUMBER")) {
                menu.findItem(R.id.menu_templates).getSubMenu().removeItem(template.getItemId());
            }
        }
        return result;
    }

    @Override
    protected String getLastLog() {
        return Settings.getLastTrackableLog();
    }

    /**
     * This will display a popup for confirming if Trackable Log should be send without a geocode.
     * It will be displayed only MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE times. A "Do not ask me again"
     * checkbox is also added.
     */
    public class LogTrackableWithoutGeocodeBuilder {

        private CheckBox doNotAskAgain;

        public AlertDialog create(final Activity activity) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.trackable_title_log_without_geocode);

            final Context themedContext = Settings.isLightSkin() && VERSION.SDK_INT < VERSION_CODES.HONEYCOMB ? new ContextThemeWrapper(activity, R.style.dark) : activity;

            final View layout = View.inflate(themedContext, R.layout.logtrackable_without_geocode, null);
            builder.setView(layout);

            doNotAskAgain = (CheckBox) layout.findViewById(R.id.logtrackable_do_not_ask_me_again);

            final int showCount = Settings.getLogTrackableWithoutGeocodeShowCount();
            Settings.setLogTrackableWithoutGeocodeShowCount(showCount + 1);

            builder.setPositiveButton(string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    checkDoNotAskAgain();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    checkDoNotAskAgain();
                    dialog.dismiss();
                    // Post the log
                    postLog();
                }
            });
            return builder.create();
        }

        /**
         * Verify if doNotAskAgain is checked.
         * If true, set the counter to MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE
         */
        private void checkDoNotAskAgain() {
            if (doNotAskAgain.isChecked()) {
                Settings.setLogTrackableWithoutGeocodeShowCount(MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE);
            }
        }
    }

}
