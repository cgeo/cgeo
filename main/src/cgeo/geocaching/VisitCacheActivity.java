package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.loaders.UrlLoader;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.DateDialog;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitCacheActivity extends AbstractLoggingActivity implements DateDialog.DateDialogParent, LoaderManager.LoaderCallbacks<String> {
    static final String EXTRAS_FOUND = "found";
    static final String EXTRAS_TEXT = "text";
    static final String EXTRAS_GEOCODE = "geocode";
    static final String EXTRAS_ID = "id";

    private static final int SUBMENU_VOTE = 3;
    private static final String SAVED_STATE_RATING = "cgeo.geocaching.saved_state_rating";
    private static final String SAVED_STATE_TYPE = "cgeo.geocaching.saved_state_type";
    private static final String SAVED_STATE_DATE = "cgeo.geocaching.saved_state_date";

    private LayoutInflater inflater = null;
    private cgCache cache = null;
    private ProgressDialog waitDialog = null;
    private String cacheid = null;
    private String geocode = null;
    private String text = null;
    private boolean alreadyFound = false;
    private List<LogType> possibleLogTypes = new ArrayList<LogType>();
    private String[] viewstates = null;
    private List<TrackableLog> trackables = null;
    private Button postButton = null;
    private Button clearButton = null;
    private CheckBox tweetCheck = null;
    private LinearLayout tweetBox = null;
    private boolean tbChanged = false;

    // Data to be saved while reconfiguring
    private double rating;
    private LogType typeSelected;
    private Calendar date;

    @Override
    public Loader<String> onCreateLoader(final int id, final Bundle args) {
        if (!Settings.isLogin()) { // allow offline logging
            showToast(res.getString(R.string.err_login));
            return null;
        }
        return new UrlLoader(getBaseContext(), "http://www.geocaching.com/seek/log.aspx", new Parameters("ID", cacheid));
    }

    @Override
    public void onLoaderReset(final Loader<String> loader) {
        // Nothing to do
    }

    @Override
    public void onLoadFinished(final Loader<String> loader, final String page) {
        if (page == null) {
            showToast(res.getString(R.string.err_log_load_data));
            showProgress(false);
            return;
        }

        viewstates = Login.getViewstates(page);
        trackables = GCParser.parseTrackableLog(page);
        possibleLogTypes = GCParser.parseTypes(page);
        possibleLogTypes.remove(LogType.UPDATE_COORDINATES);

        if (!possibleLogTypes.contains(typeSelected)) {
            typeSelected = possibleLogTypes.get(0);
            setType(typeSelected);

            showToast(res.getString(R.string.info_log_type_changed));
        }

        enablePostButton(true);

        // add trackables
        if (CollectionUtils.isNotEmpty(trackables)) {
            if (inflater == null) {
                inflater = getLayoutInflater();
            }

            final LinearLayout inventoryView = (LinearLayout) findViewById(R.id.inventory);
            inventoryView.removeAllViews();

            for (TrackableLog tb : trackables) {
                LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.visit_trackable, null);

                ((TextView) inventoryItem.findViewById(R.id.trackcode)).setText(tb.trackCode);
                ((TextView) inventoryItem.findViewById(R.id.name)).setText(tb.name);
                ((TextView) inventoryItem.findViewById(R.id.action))
                        .setText(res.getString(Settings.isTrackableAutoVisit()
                                ? LogTypeTrackable.VISITED.resourceId
                                : LogTypeTrackable.DO_NOTHING.resourceId)
                                + " ▼");

                inventoryItem.setId(tb.id);
                final String tbCode = tb.trackCode;
                inventoryItem.setClickable(true);
                registerForContextMenu(inventoryItem);
                inventoryItem.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        final Intent trackablesIntent = new Intent(VisitCacheActivity.this, TrackableActivity.class);
                        trackablesIntent.putExtra(EXTRAS_GEOCODE, tbCode);
                        startActivity(trackablesIntent);
                    }
                });
                inventoryItem.findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        openContextMenu(view);
                    }
                });

                inventoryView.addView(inventoryItem);

                if (Settings.isTrackableAutoVisit()) {
                    tb.action = LogTypeTrackable.VISITED;
                    tbChanged = true;
                }
            }

            if (inventoryView.getChildCount() > 0) {
                findViewById(R.id.inventory_box).setVisibility(View.VISIBLE);
            }
            if (inventoryView.getChildCount() > 1) {
                final LinearLayout inventoryChangeAllView = (LinearLayout) findViewById(R.id.inventory_changeall);

                final Button changeButton = (Button) inventoryChangeAllView.findViewById(R.id.changebutton);
                registerForContextMenu(changeButton);
                changeButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        openContextMenu(view);
                    }
                });

                inventoryChangeAllView.setVisibility(View.VISIBLE);
            }
        }

        showProgress(false);
    }

    private void enablePostButton(boolean enabled) {
        postButton.setEnabled(enabled);
        if (enabled) {
            postButton.setOnClickListener(new PostListener());
        }
        else {
            postButton.setOnTouchListener(null);
            postButton.setOnClickListener(null);
        }
        updatePostButtonText();
    }

    private void updatePostButtonText() {
        if (postButton.isEnabled()) {
            if (typeSelected == LogType.FOUND_IT && Settings.isGCvoteLogin()) {
                if (rating == 0) {
                    postButton.setText(res.getString(R.string.log_post_no_rate));
                } else {
                    postButton.setText(res.getString(R.string.log_post_rate) + " " + ratingTextValue(rating) + "*");
                }
            } else {
                postButton.setText(res.getString(R.string.log_post));
            }
        }
        else {
            postButton.setText(res.getString(R.string.log_post_not_possible));
        }
    }

    private final Handler postLogHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }

            final StatusCode error = (StatusCode) msg.obj;
            if (error == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                // No need to save the log when quitting if it has been posted.
                text = currentLogText();
                finish();
            } else if (error == StatusCode.LOG_SAVED) {
                showToast(res.getString(R.string.info_log_saved));

                if (waitDialog != null) {
                    waitDialog.dismiss();
                }

                finish();
            } else {
                showToast(error.getErrorString(res));
            }
        }
    };

    public VisitCacheActivity() {
        super("c:geo-log");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.visit);
        setTitle(res.getString(R.string.log_new_log));

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            cacheid = extras.getString(EXTRAS_ID);
            geocode = extras.getString(EXTRAS_GEOCODE);
            text = extras.getString(EXTRAS_TEXT);
            alreadyFound = extras.getBoolean(EXTRAS_FOUND);
        }

        if ((StringUtils.isBlank(cacheid)) && StringUtils.isNotBlank(geocode)) {
            cacheid = cgData.getCacheidForGeocode(geocode);
        }
        if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(cacheid)) {
            geocode = cgData.getGeocodeForGuid(cacheid);
        }
        cache = cgData.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (StringUtils.isNotBlank(cache.getName())) {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getName());
        } else {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getGeocode());
        }

        // Get ids for later use
        postButton = (Button) findViewById(R.id.post);
        tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        tweetCheck = (CheckBox) findViewById(R.id.tweet);
        clearButton = (Button) findViewById(R.id.clear);

        // Restore previous state or initialize with default values
        date = Calendar.getInstance();
        if (savedInstanceState != null) {
            rating = savedInstanceState.getDouble(SAVED_STATE_RATING);
            typeSelected = LogType.getById(savedInstanceState.getInt(SAVED_STATE_TYPE));
            date.setTimeInMillis(savedInstanceState.getLong(SAVED_STATE_DATE));
        } else {
            rating = 0.0;
            if (alreadyFound) {
                typeSelected = LogType.NOTE;
            } else {
                typeSelected = LogType.FOUND_IT;
            }

            // If log had been previously saved, load it now, otherwise initialize signature as needed
            final LogEntry log = cgData.loadLogOffline(geocode);
            if (log != null) {
                typeSelected = log.type;
                date.setTime(new Date(log.date));
                text = log.log;
            } else if (StringUtils.isNotBlank(Settings.getSignature())
                    && Settings.isAutoInsertSignature()
                    && StringUtils.isBlank(((EditText) findViewById(R.id.log)).getText())) {
                insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache)), false);
            }
        }
        updatePostButtonText();

        final Button saveButton = (Button) findViewById(R.id.save);

        possibleLogTypes = cache.getPossibleLogTypes();

        enablePostButton(false);

        final Button typeButton = (Button) findViewById(R.id.type);
        registerForContextMenu(typeButton);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        final Button dateButton = (Button) findViewById(R.id.date);
        setDate(date);
        dateButton.setOnClickListener(new DateListener());

        final EditText logView = (EditText) findViewById(R.id.log);
        if (StringUtils.isBlank(logView.getText()) && StringUtils.isNotBlank(text)) {
            logView.setText(text);
        }

        tweetCheck.setChecked(true);

        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveLog(true);
            }
        });

        clearButton.setOnClickListener(new ClearListener());

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        saveLog(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        final SubMenu menuStars = menu.addSubMenu(0, SUBMENU_VOTE, 0, res.getString(R.string.log_rating)).setIcon(R.drawable.ic_menu_sort_by_size);
        menuStars.add(0, 10, 0, res.getString(R.string.log_no_rating));
        menuStars.add(0, 19, 0, res.getString(R.string.log_stars_5) + " (" + res.getString(R.string.log_stars_5_description) + ")");
        menuStars.add(0, 18, 0, res.getString(R.string.log_stars_45) + " (" + res.getString(R.string.log_stars_45_description) + ")");
        menuStars.add(0, 17, 0, res.getString(R.string.log_stars_4) + " (" + res.getString(R.string.log_stars_4_description) + ")");
        menuStars.add(0, 16, 0, res.getString(R.string.log_stars_35) + " (" + res.getString(R.string.log_stars_35_description) + ")");
        menuStars.add(0, 15, 0, res.getString(R.string.log_stars_3) + " (" + res.getString(R.string.log_stars_3_description) + ")");
        menuStars.add(0, 14, 0, res.getString(R.string.log_stars_25) + " (" + res.getString(R.string.log_stars_25_description) + ")");
        menuStars.add(0, 13, 0, res.getString(R.string.log_stars_2) + " (" + res.getString(R.string.log_stars_2_description) + ")");
        menuStars.add(0, 12, 0, res.getString(R.string.log_stars_15) + " (" + res.getString(R.string.log_stars_15_description) + ")");
        menuStars.add(0, 11, 0, res.getString(R.string.log_stars_1) + " (" + res.getString(R.string.log_stars_1_description) + ")");

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean voteAvailable = Settings.isGCvoteLogin() && typeSelected == LogType.FOUND_IT && StringUtils.isNotBlank(cache.getGuid());
        menu.findItem(SUBMENU_VOTE).setVisible(voteAvailable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        final int id = item.getItemId();
        if (id >= 10 && id <= 19) {
            rating = (id - 9) / 2.0;
            if (rating < 1) {
                rating = 0;
            }
            updatePostButtonText();
            return true;
        }

        return false;
    }

    private static String ratingTextValue(final double rating) {
        return String.format(Locale.getDefault(), "%.1f", rating);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.type) {
            for (final LogType typeOne : possibleLogTypes) {
                menu.add(viewId, typeOne.id, 0, typeOne.getL10n());
            }
        } else if (viewId == R.id.changebutton) {
            final int textId = findViewById(viewId).getId();

            menu.setHeaderTitle(res.getString(R.string.log_tb_changeall));
            for (LogTypeTrackable logType : LogTypeTrackable.values()) {
                menu.add(textId, logType.id, 0, res.getString(logType.resourceId));
            }
        } else {
            final int realViewId = findViewById(viewId).getId();

            for (final TrackableLog tb : trackables) {
                if (tb.id == realViewId) {
                    menu.setHeaderTitle(tb.name);
                }
            }
            for (LogTypeTrackable logType : LogTypeTrackable.values()) {
                menu.add(realViewId, logType.id, 0, res.getString(logType.resourceId));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int group = item.getGroupId();
        final int id = item.getItemId();

        if (group == R.id.type) {
            setType(LogType.getById(id));
            return true;
        }

        if (group == R.id.changebutton) {
            try {
                final LogTypeTrackable logType = LogTypeTrackable.findById(id);
                if (logType != null) {
                    final LinearLayout inventView = (LinearLayout) findViewById(R.id.inventory);
                    for (int count = 0; count < inventView.getChildCount(); count++) {
                        final LinearLayout tbView = (LinearLayout) inventView.getChildAt(count);
                        if (tbView == null) {
                            return false;
                        }

                        final TextView tbText = (TextView) tbView.findViewById(R.id.action);
                        if (tbText == null) {
                            return false;
                        }
                        tbText.setText(res.getString(logType.resourceId) + " ▼");
                    }
                    for (TrackableLog tb : trackables) {
                        tb.action = logType;
                    }
                    tbChanged = true;
                    return true;
                }
            } catch (Exception e) {
                Log.e("cgeovisit.onContextItemSelected: " + e.toString());
            }
        } else {
            try {
                final LogTypeTrackable logType = LogTypeTrackable.findById(id);
                if (logType != null) {
                    final LinearLayout tbView = (LinearLayout) findViewById(group);
                    if (tbView == null) {
                        return false;
                    }

                    final TextView tbText = (TextView) tbView.findViewById(R.id.action);
                    if (tbText == null) {
                        return false;
                    }

                    for (TrackableLog tb : trackables) {
                        if (tb.id == group) {
                            tbChanged = true;

                            tb.action = logType;
                            tbText.setText(res.getString(logType.resourceId) + " ▼");

                            Log.i("Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + id);
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                Log.e("cgeovisit.onContextItemSelected: " + e.toString());
            }
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(SAVED_STATE_RATING, rating);
        outState.putInt(SAVED_STATE_TYPE, typeSelected.id);
        outState.putLong(SAVED_STATE_DATE, date.getTimeInMillis());
    }

    @Override
    public void setDate(Calendar dateIn) {
        date = dateIn;

        final Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(Formatter.formatShortDateVerbally(date.getTime().getTime()));
    }

    public void setType(LogType type) {
        final Button typeButton = (Button) findViewById(R.id.type);

        typeSelected = type;
        typeButton.setText(typeSelected.getL10n());

        if (LogType.FOUND_IT == type && !tbChanged) {
            // TODO: change action
        } else if (LogType.FOUND_IT != type && !tbChanged) {
            // TODO: change action
        }

        if (type == LogType.FOUND_IT && Settings.isUseTwitter()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
        updatePostButtonText();
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final Dialog dateDialog = new DateDialog(VisitCacheActivity.this, VisitCacheActivity.this, date);
            dateDialog.setCancelable(true);
            dateDialog.show();
        }
    }

    private class PostListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            waitDialog = ProgressDialog.show(VisitCacheActivity.this, null, res.getString(R.string.log_saving), true);
            waitDialog.setCancelable(true);

            final String log = ((EditText) findViewById(R.id.log)).getText().toString();
            final Thread thread = new PostLogThread(postLogHandler, log);
            thread.start();
        }
    }

    private class ClearListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            //TODO: unify this method and the code in init()
            cgData.clearLogOffline(geocode);

            if (alreadyFound) {
                typeSelected = LogType.NOTE;
            } else {
                typeSelected = possibleLogTypes.get(0);
            }
            date.setTime(new Date());
            text = null;

            setType(typeSelected);

            final Button dateButton = (Button) findViewById(R.id.date);
            dateButton.setOnClickListener(new DateListener());
            setDate(date);

            final EditText logView = (EditText) findViewById(R.id.log);
            logView.setText("");

            clearButton.setOnClickListener(new ClearListener());

            showToast(res.getString(R.string.info_log_cleared));
        }
    }

    private class PostLogThread extends Thread {

        private final Handler handler;
        private final String log;

        public PostLogThread(Handler handlerIn, String logIn) {
            super("Post log");
            handler = handlerIn;
            log = logIn;
        }

        @Override
        public void run() {
            final StatusCode status = postLogFn(log);
            handler.sendMessage(handler.obtainMessage(0, status));
        }
    }

    public StatusCode postLogFn(String log) {
        try {
            final StatusCode status = GCParser.postLog(geocode, cacheid, viewstates, typeSelected,
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE),
                    log, trackables);

            if (status == StatusCode.NO_ERROR) {
                final LogEntry logNow = new LogEntry(date, typeSelected, log);

                cache.getLogs().prepend(logNow);

                if (typeSelected == LogType.FOUND_IT) {
                    cache.setFound(true);
                }

                cgData.saveChangedCache(cache);
            }

            if (status == StatusCode.NO_ERROR) {
                cgData.clearLogOffline(geocode);
            }

            if (status == StatusCode.NO_ERROR && typeSelected == LogType.FOUND_IT && Settings.isUseTwitter()
                    && Settings.isTwitterLoginValid()
                    && tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                Twitter.postTweetCache(geocode);
            }

            if (status == StatusCode.NO_ERROR && typeSelected == LogType.FOUND_IT && Settings.isGCvoteLogin()) {
                GCVote.setRating(cache, rating);
            }

            return status;
        } catch (Exception e) {
            Log.e("cgeovisit.postLogFn: " + e.toString());
        }

        return StatusCode.LOG_POST_ERROR;
    }

    private void saveLog(final boolean force) {
        final String log = currentLogText();

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        if (force || (StringUtils.isNotEmpty(log) && !StringUtils.equals(log, text))) {
            cache.logOffline(this, log, date, typeSelected);
        }
        text = log;
    }

    private String currentLogText() {
        return ((EditText) findViewById(R.id.log)).getText().toString();
    }

    public static class ActivityState {
        private final String[] viewstates;
        private final List<TrackableLog> trackables;
        private final List<LogType> possibleLogTypes;

        public ActivityState(final String[] viewstates,
                             final List<TrackableLog> trackables,
                             final List<LogType> possibleLogTypes) {
            this.viewstates = viewstates;
            this.trackables = trackables;
            this.possibleLogTypes = possibleLogTypes;
        }

    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache);
    }
}
