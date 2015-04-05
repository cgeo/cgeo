package cgeo.geocaching;

import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.AbstractTrackableLoggingManager;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class LogTrackableActivity extends AbstractLoggingActivity implements DateDialogParent, TimeDialogParent, CoordinateUpdate, LoaderManager.LoaderCallbacks<List<LogTypeTrackable>> {

    @InjectView(R.id.type) protected Button typeButton;
    @InjectView(R.id.date) protected Button dateButton;
    @InjectView(R.id.time) protected Button timeButton;
    @InjectView(R.id.geocode) protected EditText geocacheEditText;
    @InjectView(R.id.coordinates) protected Button coordinatesButton;
    @InjectView(R.id.tracking) protected EditText trackingEditText;
    @InjectView(R.id.log) protected EditText logEditText;
    @InjectView(R.id.tweet) protected CheckBox tweetCheck;
    @InjectView(R.id.tweet_box) protected LinearLayout tweetBox;

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

    TrackableConnector connector;
    private AbstractTrackableLoggingManager loggingManager;

    final public static int LOG_TRACKABLE = 1;

    @Override
    public Loader<List<LogTypeTrackable>> onCreateLoader(final int id, final Bundle bundle) {
        showProgress(true);
        loggingManager = connector.getTrackableLoggingManager(this);

        if (loggingManager == null) {
            showToast(res.getString(R.string.err_tb_not_loggable));
            finish();
            return null;
        }

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
        super.onCreate(savedInstanceState, R.layout.logtrackable_activity);
        ButterKnife.inject(this);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);

            // Load geocache if we can
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_GEOCACHE))) {
                final Geocache tmpGeocache = DataStore.loadCache(extras.getString(Intents.EXTRA_GEOCACHE), LoadFlags.LOAD_CACHE_OR_DB);
                if (tmpGeocache != null) {
                    geocache = tmpGeocache;
                }
            }
            // Display tracking code if we have, and move cursor next
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_TRACKING_CODE))) {
                trackingEditText.setText(extras.getString(Intents.EXTRA_TRACKING_CODE));
                Dialogs.moveCursorToEnd(trackingEditText);
            }
        }

        // Load Trackable from internal
        trackable = DataStore.loadTrackable(geocode);

        if (trackable == null) {
            Log.e("LogTrackableActivity.onCreate: cannot load trackable " + geocode);
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        if (StringUtils.isNotBlank(trackable.getName())) {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getName());
        } else {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getGeocode());
        }

        // We're in LogTrackableActivity, so trackable must be loggable ;)
        if (!trackable.isLoggable()) {
            showToast(res.getString(R.string.err_tb_not_loggable));
            finish();
            return;
        }

        // create trackable connector
        connector = ConnectorFactory.getTrackableConnector(geocode);

        // Start loading in background
        getSupportLoaderManager().initLoader(connector.getTrackableLoggingManagerLoaderId(), null, this).forceLoad();

        // Show retrieved infos
        displayTrackable();
        init();
        requestKeyboardForLogging();
    }

    private void displayTrackable() {

        if (StringUtils.isNotBlank(trackable.getName())) {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getName());
        } else {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getGeocode());
        }
    }

    @Override
    protected void requestKeyboardForLogging() {
        if (StringUtils.isBlank(trackingEditText.getText())) {
            new Keyboard(this).show(trackingEditText);
        }
        else {
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
            geocacheEditText.setOnFocusChangeListener(new LoadGeocacheListener());
            geocacheEditText.setText(geocache.getGeocode());
            updateCoordinates(geocache.getCoords());
            coordinatesButton.setOnClickListener(new CoordinatesListener());
        }

        initTwitter();

        if (CollectionUtils.isEmpty(possibleLogTypesTrackable)) {
            possibleLogTypesTrackable = Trackable.getPossibleLogTypes();
        }

        disableSuggestions(trackingEditText);
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

        // show/hide Coordinate fields as Trackable needs
        if (LogTypeTrackable.isCoordinatesNeeded(typeSelected) && loggingManager.canLogCoordinates()) {
            geocacheEditText.setVisibility(View.VISIBLE);
            coordinatesButton.setVisibility(View.VISIBLE);
        } else {
            geocacheEditText.setVisibility(View.GONE);
            coordinatesButton.setVisibility(View.GONE);
        }

        // show/hide Tracking Code Field for note type
        if (typeSelected != LogTypeTrackable.NOTE || typeSelected == LogTypeTrackable.NOTE && loggingManager.isTrackingCodeNeededToPostNote()) {
            trackingEditText.setVisibility(View.VISIBLE);
        } else {
            trackingEditText.setVisibility(View.GONE);
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
            timeDialog.show(getSupportFragmentManager(),"time_dialog");
        }
    }

    private class CoordinatesListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            final CoordinatesInputDialog coordinatesDialog = CoordinatesInputDialog.getInstance(geocache, geopoint, Sensors.getInstance().currentGeo());
            coordinatesDialog.setCancelable(true);
            coordinatesDialog.show(getSupportFragmentManager(),"coordinates_dialog");
        }
    }

    // React when changing geocode
    private class LoadGeocacheListener implements OnFocusChangeListener {
        @Override
        public void onFocusChange(final View view, final boolean hasFocus) {
            if (!hasFocus && !geocacheEditText.getText().toString().isEmpty()) {
                final Geocache tmpGeocache = DataStore.loadCache(geocacheEditText.getText().toString(), LoadFlags.LOAD_CACHE_OR_DB);
                if (tmpGeocache == null) {
                    geocache.setGeocode(geocacheEditText.getText().toString());
                } else {
                    geocache = tmpGeocache;
                    updateCoordinates(geocache.getCoords());
                }
            }
        }
    }

    private class Poster extends AsyncTaskWithProgress<String, StatusCode> {

        public Poster(final Activity activity, final String progressMessage) {
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
                        final LogEntry logNow = new LogEntry(date.getTimeInMillis(), typeSelected.oldLogtype, logMsg);
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
                // is this part of code really reachable ? Dind't see StatusCode.LOG_SAVED in postLog()
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
            final LogEntry logEntry = new LogEntry(date.getTimeInMillis(), typeSelected.oldLogtype, logText);
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
        final Intent logTouchIntent =  getIntent(context, trackable);
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
                    showToast(res.getString(R.string.err_trackable_log_not_anonymous));
                    if (connector.getPreferenceActivity() > 0) {
                        SettingsActivity.openForScreen(connector.getPreferenceActivity(), this);
                    }
                }
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

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
        if (loggingManager.canLogCoordinates() && LogTypeTrackable.isCoordinatesNeeded(typeSelected)) {
            // Check Coordinates
            if (geopoint == null) {
                showToast(res.getString(R.string.err_log_post_missing_coordinates));
                return;
            }
        }

        // Post Log in Background
        new Poster(this, res.getString(R.string.log_saving)).execute(logEditText.getText().toString());
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

}
