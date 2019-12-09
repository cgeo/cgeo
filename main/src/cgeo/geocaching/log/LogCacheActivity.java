package cgeo.geocaching.log;

import cgeo.geocaching.ImageSelectActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil.OnRatingChangeListener;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ViewUtils;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import org.apache.commons.lang3.StringUtils;

public class LogCacheActivity extends AbstractLoggingActivity implements DateDialog.DateDialogParent {

    private static final String SAVED_STATE_RATING = "cgeo.geocaching.saved_state_rating";
    private static final String SAVED_STATE_TYPE = "cgeo.geocaching.saved_state_type";
    private static final String SAVED_STATE_DATE = "cgeo.geocaching.saved_state_date";
    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_FAVPOINTS = "cgeo.geocaching.saved_state_favpoints";
    private static final String SAVED_STATE_PROBLEM = "cgeo.geocaching.saved_state_problem";
    private static final String SAVED_STATE_TRACKABLES = "cgeo.geocaching.saved_state_trackables";

    private static final int SELECT_IMAGE = 101;

    private Geocache cache = null;
    private String geocode = null;
    private String text = null;
    private List<LogType> possibleLogTypes = new ArrayList<>();
    private final Set<TrackableLog> trackables = new HashSet<>();
    private final List<ReportProblemType> possibleReportProblemTypes = new ArrayList<>();

    @BindView(R.id.tweet)
    protected CheckBox tweetCheck;
    @BindView(R.id.log_password_box)
    protected LinearLayout logPasswordBox;
    @BindView(R.id.favorite_check)
    protected CheckBox favCheck;
    @BindView(R.id.log) protected EditText logEditText;

    private ILoggingManager loggingManager;

    // Data to be saved while reconfiguring
    private float rating;
    private LogType typeSelected;
    private Calendar date;
    private Image image;
    private boolean sendButtonEnabled;
    private int premFavPoints;
    private ReportProblemType reportProblemSelected = ReportProblemType.NO_PROBLEM;
    private LogEntry oldLog;
    private Bundle trackableState;

    private SaveMode saveMode = SaveMode.SMART;

    private enum SaveMode {
        /** save when relevant changes are detected */
        SMART,
        /** explicitly save, via menu or saveLog button */
        FORCE,
        /** when log was posted and offline log cleared*/
        SKIP
    }

    /**
     * Hook called at the beginning of onCreateLoader().
     */
    public void onLoadStarted() {
        showProgress(true);
    }

    /**
     * Hook called at the beginning of onLoadFinished().
     */
    public void onLoadFinished() {
        if (loggingManager.hasLoaderError()) {
            showErrorLoadingData();
            return;
        }

        if (!loggingManager.hasTrackableLoadError()) {
            trackables.addAll(loggingManager.getTrackables());
        } else {
            showErrorLoadingAdditionalData();
        }
        if (!loggingManager.hasFavPointLoadError()) {
            premFavPoints = loggingManager.getPremFavoritePoints();
        } else {
            showErrorLoadingAdditionalData();
        }

        possibleLogTypes = loggingManager.getPossibleLogTypes();

        if (possibleLogTypes.isEmpty()) {
            showErrorLoadingData();
            return;
        }

        verifySelectedLogType();
        verifySelectedReportProblemType();

        initializeRatingBar();

        enablePostButton(true);

        initializeTrackablesAction();
        updateTrackablesList();
        initializeFavoriteCheck();

        showProgress(false);
    }

    /**
     * Checks that the currently selected log type is still available as a possible log type. If it is not available,
     * the selected log type will be set to the first possible type.
     */
    private void verifySelectedLogType() {
        if (!possibleLogTypes.contains(typeSelected)) {
            typeSelected = possibleLogTypes.get(0);
            setType(typeSelected);

            showToast(res.getString(R.string.info_log_type_changed));
        }
    }

    private void verifySelectedReportProblemType() {
        possibleReportProblemTypes.clear();
        possibleReportProblemTypes.add(ReportProblemType.NO_PROBLEM);
        for (final ReportProblemType reportProblem : loggingManager.getReportProblemTypes(cache)) {
            if (reportProblem.isVisible(typeSelected, cache.getType())) {
                possibleReportProblemTypes.add(reportProblem);
            }
        }

        final View reportProblemBox = findViewById(R.id.report_problem_box);
        if (possibleReportProblemTypes.size() == 1) {
            reportProblemBox.setVisibility(View.GONE);
        } else {
            reportProblemBox.setVisibility(View.VISIBLE);
        }

        if (!possibleReportProblemTypes.contains(reportProblemSelected)) {
            reportProblemSelected = possibleReportProblemTypes.get(0);
            setReportProblem(reportProblemSelected);

            showToast(res.getString(R.string.info_log_report_problem_changed));
        }
    }

    private void showErrorLoadingData() {
        showToast(res.getString(R.string.err_log_load_data));
        showProgress(false);
    }

    private void showErrorLoadingAdditionalData() {
        showToast(res.getString(R.string.warn_log_load_additional_data));
    }

    private void initializeTrackablesAction() {
        for (final TrackableLog trackable : trackables) {
            if (trackableState != null) { // refresh view
                final int tbStateId = trackableState.getInt(trackable.trackCode);
                if (tbStateId > 0) { // found in saved list
                    trackable.action = LogTypeTrackable.getById(tbStateId);
                    continue;
                }
            }
            if (Settings.isTrackableAutoVisit()) { // initial or new grabbed
                trackable.action = LogTypeTrackable.VISITED;
            }
        }
    }

    private final class TrackableLogAdapter extends ArrayAdapter<TrackableLog> {
        private TrackableLogAdapter(final Context context, final int resource, final TrackableLog[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = getLayoutInflater().inflate(R.layout.logcache_trackable_item, parent, false);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();
            if (holder == null) {
                holder = new ViewHolder(rowView);
            }

            final TrackableLog trackable = getItem(position);
            fillViewHolder(holder, trackable);
            return rowView;
        }

        private void fillViewHolder(final ViewHolder holder, final TrackableLog trackable) {
            holder.brandView.setImageResource(trackable.brand.getIconResource());
            holder.codeView.setText(trackable.trackCode);
            holder.nameView.setText(trackable.name);
            holder.actionButton.setText(trackable.action.getLabel() + " ▼");
            holder.actionButton.setOnClickListener(view -> selectTrackableAction(trackable));

            holder.infoView.setOnClickListener(view -> {
                final Intent trackablesIntent = new Intent(LogCacheActivity.this, TrackableActivity.class);
                final String tbCode = StringUtils.isNotEmpty(trackable.geocode) ? trackable.geocode : trackable.trackCode;
                trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
                trackablesIntent.putExtra(Intents.EXTRA_BRAND, trackable.brand.getId());
                trackablesIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.trackCode);
                startActivity(trackablesIntent);
            });
        }
    }

    protected static class ViewHolder extends AbstractViewHolder {
        @BindView(R.id.trackable_image_brand) protected ImageView brandView;
        @BindView(R.id.trackcode) protected TextView codeView;
        @BindView(R.id.name) protected TextView nameView;
        @BindView(R.id.action) protected TextView actionButton;
        @BindView(R.id.info) protected View infoView;

        public ViewHolder(final View rowView) {
            super(rowView);
        }
    }

    private void updateTrackablesList() {
        final TrackableLog[] trackablesArray = getSortedTrackables().toArray(new TrackableLog[trackables.size()]);
        final ListView inventoryList = findViewById(R.id.inventory);
        inventoryList.setAdapter(new TrackableLogAdapter(this, R.layout.logcache_trackable_item, trackablesArray));
        ViewUtils.setListViewHeightBasedOnItems(inventoryList);

        findViewById(R.id.inventory_box).setVisibility(trackables.isEmpty() ? View.GONE : View.VISIBLE);

        final LinearLayout inventoryChangeAllView = findViewById(R.id.inventory_changeall);
        inventoryChangeAllView.setVisibility(trackables.size() > 1 ? View.VISIBLE : View.GONE);

        final Button changeButton = inventoryChangeAllView.findViewById(R.id.changebutton);
        changeButton.setOnClickListener(view -> selectAllTrackablesAction());
    }

    private ArrayList<TrackableLog> getSortedTrackables() {
        final TrackableComparator comparator = Settings.getTrackableComparator();
        final ArrayList<TrackableLog> sortedTrackables = new ArrayList<>(trackables);
        Collections.sort(sortedTrackables, comparator.getComparator());
        return sortedTrackables;
    }

    private void enablePostButton(final boolean enabled) {
        sendButtonEnabled = enabled;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.logcache_activity);

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            if (StringUtils.isBlank(geocode)) {
                final String cacheid = extras.getString(Intents.EXTRA_ID);
                if (StringUtils.isNotBlank(cacheid)) {
                    geocode = DataStore.getGeocodeForGuid(cacheid);
                }
            }
        }

        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        invalidateOptionsMenuCompatible();
        possibleLogTypes = cache.getPossibleLogTypes();

        setCacheTitleBar(cache);

        initializeRatingBar();

        loggingManager = cache.getLoggingManager(this);
        loggingManager.init();

        // initialize with default values
        setDefaultValues();

        // Restore previous state
        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(SAVED_STATE_RATING);
            typeSelected = LogType.getById(savedInstanceState.getInt(SAVED_STATE_TYPE));
            date.setTimeInMillis(savedInstanceState.getLong(SAVED_STATE_DATE));
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE);
            premFavPoints = savedInstanceState.getInt(SAVED_STATE_FAVPOINTS);
            reportProblemSelected = ReportProblemType.findByCode(savedInstanceState.getString(SAVED_STATE_PROBLEM));
            trackableState = savedInstanceState.getBundle(SAVED_STATE_TRACKABLES);
        } else {
            // If log had been previously saved, load it now, otherwise initialize signature as needed
            loadLogFromDatabase();
        }
        if (image == null) {
            image = Image.NONE;
        }
        // TODO: Why is it disabled in onCreate?
        // Probably it should be disabled only when there is some explicit issue.
        // See https://github.com/cgeo/cgeo/issues/7188
        enablePostButton(false);

        final TextView problemButton = findViewById(R.id.report_problem);
        problemButton.setText(getString(reportProblemSelected.labelId) + " ▼");
        problemButton.setOnClickListener(view -> selectProblemType());

        verifySelectedLogType();
        final Button typeButton = findViewById(R.id.type);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(view -> selectLogType());

        final Button dateButton = findViewById(R.id.date);
        setDate(date);
        dateButton.setOnClickListener(new DateListener());

        if (StringUtils.isBlank(currentLogText()) && StringUtils.isNotBlank(text)) {
            setLogText();
        }

        tweetCheck.setChecked(true);
        updateTweetBox(typeSelected);
        updateLogPasswordBox(typeSelected);

        // Load Generic Trackables
        AndroidRxUtils.bindActivity(this,
            // Obtain the actives connectors
            Observable.fromIterable(ConnectorFactory.getLoggableGenericTrackablesConnectors())
            .flatMap(new Function<TrackableConnector, Observable<TrackableLog>>() {
                @Override
                public Observable<TrackableLog> apply(final TrackableConnector trackableConnector) {
                    return Observable.defer(new Callable<Observable<TrackableLog>>() {
                        @Override
                        public Observable<TrackableLog> call() {
                            return trackableConnector.trackableLogInventory();
                        }
                    }).subscribeOn(AndroidRxUtils.networkScheduler);
                }
            }).toList()
        ).subscribe(new Consumer<List<TrackableLog>>() {
            @Override
            public void accept(final List<TrackableLog> trackableLogs) {
                // Store trackables
                trackables.addAll(trackableLogs);
                // Update the UI
                initializeTrackablesAction();
                updateTrackablesList();
            }
        });

        requestKeyboardForLogging();
    }

    private void setLogText() {
        logEditText.setText(text);
        Dialogs.moveCursorToEnd(logEditText);
    }

    private void loadLogFromDatabase() {
        oldLog = DataStore.loadLogOffline(geocode);
        if (oldLog != null) {
            typeSelected = oldLog.getType();
            date.setTime(new Date(oldLog.date));
            text = oldLog.log;
            reportProblemSelected = oldLog.reportProblem;
        } else if (StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature() && StringUtils.isBlank(currentLogText())) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, null)), false);
        }
    }

    private void initializeRatingBar() {
        if (GCVote.isVotingPossible(cache)) {
            GCVoteRatingBarUtil.initializeRatingBar(cache, getWindow().getDecorView().getRootView(), new OnRatingChangeListener() {

                @Override
                public void onRatingChanged(final float stars) {
                    rating = stars;
                }
            });
        }
    }

    private void initializeFavoriteCheck() {
        if (ConnectorFactory.getConnector(cache).supportsAddToFavorite(cache, typeSelected)) {
            if (premFavPoints > 0) {
                favCheck.setVisibility(View.VISIBLE);
                favCheck.setText(res.getQuantityString(R.plurals.fav_points_remaining, premFavPoints, premFavPoints));
            }
        } else {
            favCheck.setVisibility(View.GONE);
        }
    }

    private void setDefaultValues() {
        rating = GCVote.NO_RATING;
        setType(cache.getDefaultLogType());

        final Calendar defaultDate = Calendar.getInstance();
        // it this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && CalendarUtils.isPastEvent(cache) && typeSelected == LogType.ATTENDED) {
            defaultDate.setTime(cache.getHiddenDate());
        }
        setDate(defaultDate);

        text = null;
        image = Image.NONE;

        logEditText.setText(StringUtils.EMPTY);
        setReportProblem(ReportProblemType.NO_PROBLEM);
        oldLog = null;

        final EditText logPasswordView = LogCacheActivity.this.findViewById(R.id.log_password);
        logPasswordView.setText(StringUtils.EMPTY);

        saveMode = SaveMode.SMART;
    }

    private void clearLog() {
        new ClearLogCommand(this).execute();
    }

    @Override
    public void finish() {
        saveLog();
        super.finish();
    }

    @Override
    public void onStop() {
        saveLog();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(SAVED_STATE_RATING, rating);
        outState.putInt(SAVED_STATE_TYPE, typeSelected.id);
        outState.putLong(SAVED_STATE_DATE, date.getTimeInMillis());
        outState.putParcelable(SAVED_STATE_IMAGE, image);
        outState.putInt(SAVED_STATE_FAVPOINTS, premFavPoints);
        outState.putString(SAVED_STATE_PROBLEM, reportProblemSelected.code);
        // save state of trackables
        final Bundle outTrackables = new Bundle();
        for (final TrackableLog trackable : trackables) {
            outTrackables.putInt(trackable.trackCode, trackable.action.id);
        }
        outState.putBundle(SAVED_STATE_TRACKABLES, outTrackables);
    }

    @Override
    public void setDate(final Calendar dateIn) {
        date = dateIn;

        final Button dateButton = findViewById(R.id.date);
        dateButton.setText(Formatter.formatShortDateVerbally(date.getTime().getTime()));
    }

    public void setType(final LogType type) {
        final Button typeButton = findViewById(R.id.type);

        typeSelected = type;
        typeButton.setText(typeSelected.getL10n());

        updateTweetBox(type);
        updateLogPasswordBox(type);
        initializeFavoriteCheck();
        verifySelectedReportProblemType();
    }

    public void setReportProblem(final ReportProblemType reportProblem) {
        final TextView reportProblemButton = findViewById(R.id.report_problem);

        reportProblemSelected = reportProblem;
        reportProblemButton.setText(getString(reportProblemSelected.labelId) + " ▼");
    }

    private void updateTweetBox(final LogType type) {
        if (type == LogType.FOUND_IT && Settings.isUseTwitter() && Settings.isTwitterLoginValid()) {
            tweetCheck.setVisibility(View.VISIBLE);
        } else {
            tweetCheck.setVisibility(View.GONE);
        }
    }

    private void updateLogPasswordBox(final LogType type) {
        if (type == LogType.FOUND_IT && cache.isLogPasswordRequired()) {
            logPasswordBox.setVisibility(View.VISIBLE);
        } else {
            logPasswordBox.setVisibility(View.GONE);
        }
    }

    private final class ClearLogCommand extends AbstractCommand {

        private String oldText;
        private LogType oldType;
        private Calendar oldDate;
        private ReportProblemType oldReportProblem;
        private LogEntry oldOldLog;

        ClearLogCommand(final Activity context) {
            super(context);
        }

        @Override
        protected void doCommand() {
            oldText = currentLogText();
            oldType = typeSelected;
            oldDate = date;
            oldReportProblem = reportProblemSelected;
            oldOldLog = oldLog;
            cache.clearOfflineLog();
        }

        @Override
        protected void undoCommand() {
            cache.logOffline(getContext(), oldText, oldDate, oldType, oldReportProblem);
        }

        @Override
        protected String getResultMessage() {
            return getContext().getString(R.string.info_log_cleared);
        }

        @Override
        protected void onFinished() {
            setDefaultValues();
        }

        @Override
        protected void onFinishedUndo() {
            text = oldText;
            typeSelected = oldType;
            date = oldDate;
            oldLog = oldOldLog;
            setType(typeSelected);
            setReportProblem(oldReportProblem);
            setDate(date);
            setLogText();
        }
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final DateDialog dateDialog = DateDialog.getInstance(date);
            dateDialog.setCancelable(true);
            dateDialog.show(getSupportFragmentManager(), "date_dialog");
        }
    }

    private class Poster extends AsyncTaskWithProgressText<String, StatusCode> {

        Poster(final Activity activity, final String progressMessage) {
            super(activity, res.getString(image.isEmpty() ?
                    R.string.log_posting_log :
                    R.string.log_saving_and_uploading), progressMessage);
        }

        @Override
        protected StatusCode doInBackgroundInternal(final String[] logTexts) {
            final String log = logTexts[0];
            final String logPwd = logTexts.length > 1 ? logTexts[1] : null;

            try {
                final LogResult logResult = loggingManager.postLog(typeSelected, date, log, logPwd, new ArrayList<>(trackables), reportProblemSelected);
                ImageResult imageResult = null;

                if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                    // update geocache in DB
                    if (typeSelected.isFoundLog()) {
                        cache.setFound(true);
                        cache.setVisitedDate(date.getTimeInMillis());
                    }
                    DataStore.saveChangedCache(cache);

                    final LogEntry.Builder logBuilder = new LogEntry.Builder()
                            .setDate(date.getTimeInMillis())
                            .setLogType(typeSelected)
                            .setLog(log)
                            .setFriend(true);

                    // login credentials may vary from actual username
                    // Get correct author name from connector (if applicable)
                    final IConnector cacheConnector = ConnectorFactory.getConnector(cache);
                    if (cacheConnector instanceof ILogin) {
                        final String username = ((ILogin) cacheConnector).getUserName();
                        if (!"".equals(username)) {
                            logBuilder.setAuthor(username);
                        }
                    }

                    // Posting image
                    if (!image.isEmpty()) {
                        publishProgress(res.getString(R.string.log_posting_image));
                        imageResult = loggingManager.postLogImage(logResult.getLogId(), image);
                        final String uploadedImageUrl = imageResult.getImageUri();
                        if (StringUtils.isNotEmpty(uploadedImageUrl)) {
                            logBuilder.addLogImage(image.buildUpon()
                                    .setUrl(uploadedImageUrl)
                                    .build());
                        }
                    }

                    // update logs in DB
                    final List<LogEntry> newLogs = new ArrayList<>(cache.getLogs());
                    final LogEntry logNow = logBuilder.build();
                    newLogs.add(0, logNow);
                    if (reportProblemSelected != ReportProblemType.NO_PROBLEM) {
                        final LogEntry logProblem = logBuilder.setLog(getString(reportProblemSelected.textId)).setLogImages(Collections.<Image>emptyList()).setLogType(reportProblemSelected.logType).build();
                        newLogs.add(0, logProblem);
                    }
                    DataStore.saveLogs(cache.getGeocode(), newLogs);

                    // update offline log in DB
                    cache.clearOfflineLog();

                    if (typeSelected == LogType.FOUND_IT && tweetCheck.isChecked() && tweetCheck.getVisibility() == View.VISIBLE) {
                        publishProgress(res.getString(R.string.log_posting_twitter));
                        Twitter.postTweetCache(geocode, logNow);
                    }
                    if (GCVote.isValidRating(rating) && GCVote.isVotingPossible(cache)) {
                        publishProgress(res.getString(R.string.log_posting_gcvote));
                        if (GCVote.setRating(cache, rating)) {
                            cache.setMyVote(rating);
                            DataStore.saveChangedCache(cache);
                        } else {
                            showToast(res.getString(R.string.err_gcvote_send_rating));
                        }
                    }

                    // Posting Generic Trackables
                    for (final TrackableConnector connector: ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
                        final TrackableLoggingManager manager = connector.getTrackableLoggingManager((AbstractLoggingActivity) activity);
                        if (manager != null) {
                            // Filter trackables logs by action and brand
                            final Set<TrackableLog> trackablesMoved = new HashSet<>();
                            for (final TrackableLog trackableLog : trackables) {
                                if (trackableLog.action != LogTypeTrackable.DO_NOTHING && trackableLog.brand == connector.getBrand()) {
                                    trackablesMoved.add(trackableLog);
                                }
                            }

                            // Posting trackables logs
                            int trackableLogcounter = 1;
                            for (final TrackableLog trackableLog : trackablesMoved) {
                                publishProgress(res.getString(R.string.log_posting_generic_trackable, trackableLog.brand.getLabel(), trackableLogcounter, trackablesMoved.size()));
                                manager.postLog(cache, trackableLog, date, log);
                                trackableLogcounter++;
                            }
                        }
                    }
                }

                // Todo error handling should be better than that
                if (imageResult != null && imageResult.getPostResult() != StatusCode.NO_ERROR && imageResult.getPostResult() != StatusCode.LOG_SAVED) {
                    return imageResult.getPostResult();
                }
                return logResult.getPostLogResult();
            } catch (final RuntimeException e) {
                Log.e("LogCacheActivity.Poster.doInBackgroundInternal", e);
            }

            return StatusCode.LOG_POST_ERROR;
        }

        @Override
        protected void onPostExecuteInternal(final StatusCode status) {
            if (status == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                // No need to save the log when quitting if it has been posted.
                saveMode = SaveMode.SKIP;
                finish();
            } else if (status == StatusCode.LOG_SAVED) {
                showToast(res.getString(R.string.info_log_saved));
                saveMode = SaveMode.SKIP;
                finish();
            } else {
                Dialogs.confirmPositiveNegativeNeutral(activity, R.string.info_log_post_failed,
                    res.getString(R.string.info_log_post_failed_reason, status.getErrorString(res)),
                    R.string.info_log_post_retry, // Positive Button
                    string.cancel,                // Negative Button
                    R.string.info_log_post_save,  // Neutral Button
                    // Positive button: Retry
                        (dialog, which) -> sendLogInternal(),
                    // Negative button: dismiss popup
                    null,
                    // Neutral Button: SaveLog
                        (dialogInterface, i) -> {
                            saveMode = SaveMode.FORCE;
                            finish();
                        });
            }
        }
    }

    private void saveLog() {
        if (saveMode == SaveMode.SKIP) {
            return;
        }

        final String log = currentLogText();

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        // But if date or the reportProblemType has changed, then save anyway.
        if (saveMode == SaveMode.FORCE ||
                (StringUtils.isNotEmpty(log) && !StringUtils.equals(log, text) && !StringUtils.equals(log, Settings.getSignature()))
                || (oldLog != null && (oldLog.reportProblem != reportProblemSelected || oldLog.date != date.getTime().getTime()))
                || (oldLog == null && reportProblemSelected != ReportProblemType.NO_PROBLEM)
                ) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    cache.logOffline(LogCacheActivity.this, log, date, typeSelected, reportProblemSelected);
                    Settings.setLastCacheLog(log);
                    return null;
                }
            }.execute();
        }
        text = log;
    }

    private String currentLogText() {
        return logEditText.getText().toString();
    }

    private String currentLogPassword() {
        final EditText passwdEditText = findViewById(R.id.log_password);
        return passwdEditText.getText().toString();
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache, null);
    }

    private void selectAllTrackablesAction() {
        final Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(res.getString(R.string.log_tb_changeall) + " (" + trackables.size() + ')');

        final List<LogTypeTrackable> tbLogTypeValues = LogTypeTrackable.getLogTypeTrackableForLogCache();
        final String[] tbLogTypes = getTBLogTypes(tbLogTypeValues);
        alert.setItems(tbLogTypes, (dialog, position) -> {
            final LogTypeTrackable logType = tbLogTypeValues.get(position);
            for (final TrackableLog tb : trackables) {
                tb.action = logType;
                Log.d("Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + logType);
            }
            updateTrackablesList();
            dialog.dismiss();
        });
        alert.create().show();
    }

    private static String[] getTBLogTypes(final List<LogTypeTrackable> tbLogTypeValues) {
        final String[] tbLogTypes = new String[tbLogTypeValues.size()];
        for (int i = 0; i < tbLogTypes.length; i++) {
            tbLogTypes[i] = tbLogTypeValues.get(i).getLabel();
        }
        return tbLogTypes;
    }

    private void selectLogType() {
        // use a local copy of the possible types, as that one might be modified in the background by the loader
        final List<LogType> possible = new ArrayList<>(possibleLogTypes);

        final Builder alert = new AlertDialog.Builder(this);
        final String[] choices = new String[possible.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = possible.get(i).getL10n();
        }
        alert.setSingleChoiceItems(choices, possible.indexOf(typeSelected), (dialog, position) -> {
            setType(possible.get(position));
            dialog.dismiss();
        });
        alert.create().show();
    }

    private void selectProblemType() {
        // use a local copy of the possible problem types, as that one might be modified in the background by the loader
        final List<ReportProblemType> possible = new ArrayList<>(possibleReportProblemTypes);

        final Builder alert = new AlertDialog.Builder(this);
        final String[] choices = new String[possible.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = getString(possible.get(i).labelId);
        }
        alert.setSingleChoiceItems(choices, possible.indexOf(reportProblemSelected), (dialog, position) -> {
            setReportProblem(possible.get(position));
            dialog.dismiss();
        });
        alert.create().show();
    }

    private void selectTrackableAction(final TrackableLog trackable) {
        final Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(trackable.name);
        final List<LogTypeTrackable> tbLogTypeValues = LogTypeTrackable.getLogTypeTrackableForLogCache();
        final String[] tbLogTypes = getTBLogTypes(tbLogTypeValues);
        alert.setSingleChoiceItems(tbLogTypes, tbLogTypeValues.indexOf(trackable.action), (dialog, position) -> {
            final LogTypeTrackable logType = tbLogTypeValues.get(position);
            trackable.action = logType;
            Log.d("Trackable " + trackable.trackCode + " (" + trackable.name + ") has new action: #" + logType);
            updateTrackablesList();
            dialog.dismiss();
        });
        alert.create().show();
    }

    private void selectImage() {
        final Intent selectImageIntent = new Intent(this, ImageSelectActivity.class);
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE, image);
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
        selectImageIntent.putExtra(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE, loggingManager.getMaxImageUploadSize());
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE_CAPTION_MANDATORY, loggingManager.isImageCaptionMandatory());

        startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                image = data.getParcelableExtra(Intents.EXTRA_IMAGE);
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                showToast(getString(R.string.err_select_logimage_failed));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_send:
                sendLogAndConfirm();
                return true;
            case R.id.menu_image:
                selectImage();
                return true;
            case R.id.save:
                saveMode = SaveMode.FORCE;
                finish();
                return true;
            case R.id.clear:
                clearLog();
                return true;
            case R.id.menu_sort_trackables_name:
//                item.setChecked(true);
                sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_NAME);
                return true;
            case R.id.menu_sort_trackables_code:
//                item.setChecked(true);
                sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_TRACKCODE);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendLogAndConfirm() {
        if (!sendButtonEnabled) {
            Dialogs.message(this, R.string.log_post_not_possible);
            return;
        }
        if (CalendarUtils.isFuture(date)) {
            Dialogs.message(this, R.string.log_date_future_not_allowed);
            return;
        }
        if (typeSelected.mustConfirmLog()) {
            Dialogs.confirm(this, R.string.confirm_log_title, res.getString(R.string.confirm_log_message, typeSelected.getL10n()), (dialog, which) -> sendLogInternal());
        } else if (reportProblemSelected != ReportProblemType.NO_PROBLEM) {
            Dialogs.confirm(this, R.string.confirm_report_problem_title, res.getString(R.string.confirm_report_problem_message, reportProblemSelected.getL10n()), (dialog, which) -> sendLogInternal());
        } else {
            sendLogInternal();
        }
    }

    private void sendLogInternal() {
        new Poster(this, res.getString(R.string.log_saving)).execute(currentLogText(), currentLogPassword());
        Settings.setLastCacheLog(currentLogText());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_image).setVisible(cache.supportsLogImages());
        menu.findItem(R.id.save).setVisible(true);
        menu.findItem(R.id.clear).setVisible(true);
        menu.findItem(R.id.menu_sort_trackables_by).setVisible(true);
        switch (Settings.getTrackableComparator()) {
            case TRACKABLE_COMPARATOR_NAME:
                menu.findItem(R.id.menu_sort_trackables_name).setChecked(true);
                break;
            case TRACKABLE_COMPARATOR_TRACKCODE:
                menu.findItem(R.id.menu_sort_trackables_code).setChecked(true);
                break;
            default:
                menu.findItem(R.id.menu_sort_trackables_name).setChecked(true);
        }
        return true;
    }

    public static Intent getLogCacheIntent(final Activity context, final String cacheId, final String geocode) {
        final Intent logVisitIntent = new Intent(context, LogCacheActivity.class);
        logVisitIntent.putExtra(Intents.EXTRA_ID, cacheId);
        logVisitIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        return logVisitIntent;
    }

    @Override
    protected String getLastLog() {
        return Settings.getLastCacheLog();
    }

    private void sortTrackables(final TrackableComparator comparator) {
        Settings.setTrackableComparator(comparator);
        updateTrackablesList();
        invalidateOptionsMenuCompatible();
    }

}
