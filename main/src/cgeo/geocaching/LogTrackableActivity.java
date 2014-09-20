package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.AbstractTrackableLoggingManager;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LogTrackableActivity extends AbstractLoggingActivity implements DateDialog.DateDialogParent, LoaderManager.LoaderCallbacks<List<LogTypeTrackable>> {

    @InjectView(R.id.type) protected Button typeButton;
    @InjectView(R.id.date) protected Button dateButton;
    @InjectView(R.id.tracking) protected EditText trackingEditText;
    @InjectView(R.id.tweet) protected CheckBox tweetCheck;
    @InjectView(R.id.tweet_box) protected LinearLayout tweetBox;

    private List<LogTypeTrackable> possibleLogTypesTrackable = new ArrayList<>();
    private ProgressDialog waitDialog = null;
    private String geocode = null;
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

    private final Handler postLogHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }

            final StatusCode error = (StatusCode) msg.obj;
            if (error == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                setResult(RESULT_OK);
                finish();
            } else {
                showToast(error.getErrorString(res));
            }
        }
    };

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

    public void init() {
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

    public void setType(final LogTypeTrackable type) {
        typeSelected = type;
        typeButton.setText(typeSelected.getLabel());
    }

    private void initTwitter() {
        tweetCheck.setChecked(true);
        if (Settings.isUseTwitter() && Settings.isTwitterLoginValid()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final DateDialog dateDialog = DateDialog.getInstance(date);
            dateDialog.setCancelable(true);
            dateDialog.show(getSupportFragmentManager(),"date_dialog");
        }
    }

    private class PostLogThread extends Thread {
        final private Handler handler;
        final private String tracking;
        final private String log;

        public PostLogThread(final Handler handlerIn, final String trackingIn, final String logIn) {
            super("Post trackable log");
            handler = handlerIn;
            tracking = trackingIn;
            log = logIn;
        }

        @Override
        public void run() {
            final StatusCode status = postLogFn(tracking, log);
            handler.sendMessage(handler.obtainMessage(0, status));
        }
    }

    public StatusCode postLogFn(final String tracking, final String log) {
        try {
            final TrackableLog trackableLog = new TrackableLog(tracking, trackable.getName(), 0, 0, trackable.getBrand());
            trackableLog.setAction(typeSelected);
            final LogResult logResult = loggingManager.postLog(null, trackableLog, date, log);

            if (logResult.getPostLogResult() == StatusCode.NO_ERROR && Settings.isUseTwitter() &&
                    Settings.isTwitterLoginValid() &&
                    tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                // TODO create a LogTrackableEntry. For now use "oldLogtype" as a temporary migration path
                Twitter.postTweetTrackable(geocode, new LogEntry(0, typeSelected.oldLogtype, log));
            }
            if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                addLocalTrackableLog(log);
            }

            return logResult.getPostLogResult();
        } catch (final Exception e) {
            Log.e("LogTrackableActivity.postLogFn", e);
        }

        return StatusCode.LOG_POST_ERROR;
    }

    /**
     * Adds the new log to the list of log entries for this trackable to be able to show it in the trackable activity.
     *
     *
     */
    private void addLocalTrackableLog(final String logText) {
        // TODO create a LogTrackableEntry. For now use "oldLogtype" as a temporary migration path
        final LogEntry logEntry = new LogEntry(date.getTimeInMillis(), typeSelected.oldLogtype, logText);
        final ArrayList<LogEntry> modifiedLogs = new ArrayList<>(trackable.getLogs());
        modifiedLogs.add(0, logEntry);
        trackable.setLogs(modifiedLogs);
        DataStore.saveTrackable(trackable);
    }

    public static Intent getIntent(final Context context, final Trackable trackable) {
        final Intent logTouchIntent = new Intent(context, LogTrackableActivity.class);
        logTouchIntent.putExtra(Intents.EXTRA_GEOCODE, trackable.getGeocode());
        logTouchIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.getTrackingcode());
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
                sendLog();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendLog() {
        if (postReady) {
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.log_saving), true);
            waitDialog.setCancelable(true);

            Settings.setTrackableAction(typeSelected.id);

            final EditText logEditText = (EditText) findViewById(R.id.log);
            final String tracking = trackingEditText.getText().toString();
            final String log = logEditText.getText().toString();
            new PostLogThread(postLogHandler, tracking, log).start();
            Settings.setLastTrackableLog(log);
        } else {
            showToast(res.getString(R.string.err_log_load_data_still));
        }
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
