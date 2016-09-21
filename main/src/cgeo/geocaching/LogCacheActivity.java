package cgeo.geocaching;

import cgeo.geocaching.activity.ShowcaseViewBuilder;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil;
import cgeo.geocaching.gcvote.GCVoteRatingBarUtil.OnRatingChangeListener;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.LogEntry;
import cgeo.geocaching.models.TrackableLog;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.TextUtils;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class LogCacheActivity extends AbstractLoggingActivity implements DateDialog.DateDialogParent {

    private static final String SAVED_STATE_RATING = "cgeo.geocaching.saved_state_rating";
    private static final String SAVED_STATE_TYPE = "cgeo.geocaching.saved_state_type";
    private static final String SAVED_STATE_DATE = "cgeo.geocaching.saved_state_date";
    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_FAVPOINTS = "cgeo.geocaching.saved_state_favpoints";

    private static final int SELECT_IMAGE = 101;

    private LayoutInflater inflater = null;
    private Geocache cache = null;
    private String geocode = null;
    private String text = null;
    private List<LogType> possibleLogTypes = new ArrayList<>();
    private final Set<TrackableLog> trackables = new HashSet<>();

    @BindView(R.id.tweet)
    protected CheckBox tweetCheck;
    @BindView(R.id.log_password_box)
    protected LinearLayout logPasswordBox;
    @BindView(R.id.favorite_check)
    protected CheckBox favCheck;
    @BindView(R.id.log) protected EditText logEditText;

    private SparseArray<TrackableLog> actionButtons;

    private ILoggingManager loggingManager;

    // Data to be saved while reconfiguring
    private float rating;
    private LogType typeSelected;
    private Calendar date;
    private Image image;
    private boolean sendButtonEnabled;
    private int premFavPoints;

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

        trackables.addAll(loggingManager.getTrackables());
        possibleLogTypes = loggingManager.getPossibleLogTypes();
        premFavPoints = loggingManager.getPremFavoritePoints();

        if (possibleLogTypes.isEmpty()) {
            showErrorLoadingData();
            return;
        }

        if (!possibleLogTypes.contains(typeSelected)) {
            typeSelected = possibleLogTypes.get(0);
            setType(typeSelected);

            showToast(res.getString(R.string.info_log_type_changed));
        }

        initializeRatingBar();

        enablePostButton(true);

        initializeTrackablesAction();
        updateTrackablesList();
        initializeFavoriteCheck();

        showProgress(false);
    }

    private void showErrorLoadingData() {
        showToast(res.getString(R.string.err_log_load_data));
        showProgress(false);
    }

    private void initializeTrackablesAction() {
        if (Settings.isTrackableAutoVisit()) {
            for (final TrackableLog trackable : trackables) {
                trackable.action = LogTypeTrackable.VISITED;
            }
        }
    }

    private void updateTrackablesList() {
        if (CollectionUtils.isEmpty(trackables)) {
            return;
        }
        if (inflater == null) {
            inflater = getLayoutInflater();
        }
        actionButtons = new SparseArray<>();

        final LinearLayout inventoryView = ButterKnife.findById(this, R.id.inventory);
        inventoryView.removeAllViews();

        for (final TrackableLog tb : getSortedTrackables()) {
            final LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.logcache_trackable_item, inventoryView, false);

            final ImageView brandView = ButterKnife.findById(inventoryItem, R.id.trackable_image_brand);
            brandView.setImageResource(tb.brand.getIconResource());
            final TextView codeView = ButterKnife.findById(inventoryItem, R.id.trackcode);
            codeView.setText(tb.trackCode);
            final TextView nameView = ButterKnife.findById(inventoryItem, R.id.name);
            nameView.setText(tb.name);
            final TextView actionButton = ButterKnife.findById(inventoryItem, R.id.action);
            actionButton.setId(tb.id);
            actionButtons.put(actionButton.getId(), tb);
            actionButton.setText(tb.action.getLabel() + " ▼");
            actionButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View view) {
                    selectTrackableAction(view);
                }
            });

            inventoryItem.setClickable(true);
            ButterKnife.findById(inventoryItem, R.id.info).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View view) {
                    final Intent trackablesIntent = new Intent(LogCacheActivity.this, TrackableActivity.class);
                    final String tbCode = StringUtils.isNotEmpty(tb.geocode) ? tb.geocode : tb.trackCode;
                    trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
                    trackablesIntent.putExtra(Intents.EXTRA_BRAND, tb.brand.getId());
                    trackablesIntent.putExtra(Intents.EXTRA_TRACKING_CODE, tb.trackCode);
                    startActivity(trackablesIntent);
                }
            });

            inventoryView.addView(inventoryItem);
        }

        if (inventoryView.getChildCount() > 0) {
            ButterKnife.findById(this, R.id.inventory_box).setVisibility(View.VISIBLE);
        }
        if (inventoryView.getChildCount() > 1) {
            final LinearLayout inventoryChangeAllView = ButterKnife.findById(this, R.id.inventory_changeall);

            final Button changeButton = ButterKnife.findById(inventoryChangeAllView, R.id.changebutton);
            changeButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View view) {
                    selectAllTrackablesAction();
                }
            });

            inventoryChangeAllView.setVisibility(View.VISIBLE);
        }
    }

    private ArrayList<TrackableLog> getSortedTrackables() {
        final ArrayList<TrackableLog> sortedTrackables = new ArrayList<>(trackables);
        Collections.sort(sortedTrackables, new Comparator<TrackableLog>() {

            @Override
            public int compare(final TrackableLog lhs, final TrackableLog rhs) {
                return TextUtils.COLLATOR.compare(lhs.name, rhs.name);
            }
        });
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

        if (StringUtils.isNotBlank(cache.getName())) {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getName());
        } else {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getGeocode());
        }

        initializeRatingBar();

        // initialize with default values
        setDefaultValues();

        // Restore previous state
        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(SAVED_STATE_RATING);
            typeSelected = LogType.getById(savedInstanceState.getInt(SAVED_STATE_TYPE));
            date.setTimeInMillis(savedInstanceState.getLong(SAVED_STATE_DATE));
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE);
            premFavPoints = savedInstanceState.getInt(SAVED_STATE_FAVPOINTS);
        } else {
            // If log had been previously saved, load it now, otherwise initialize signature as needed
            loadLogFromDatabase();
        }
        if (image == null) {
            image = Image.NONE;
        }
        enablePostButton(false);

        final Button typeButton = ButterKnife.findById(this, R.id.type);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                selectLogType();
            }
        });

        final Button dateButton = ButterKnife.findById(this, R.id.date);
        setDate(date);
        dateButton.setOnClickListener(new DateListener());

        if (StringUtils.isBlank(currentLogText()) && StringUtils.isNotBlank(text)) {
            setLogText();
        }

        tweetCheck.setChecked(true);
        updateTweetBox(typeSelected);
        updateLogPasswordBox(typeSelected);

        loggingManager = cache.getLoggingManager(this);
        loggingManager.init();

        // Load Generic Trackables
        AppObservable.bindActivity(this,
            // Obtain the actives connectors
            Observable.from(ConnectorFactory.getLoggableGenericTrackablesConnectors())
            .flatMap(new Func1<TrackableConnector, Observable<TrackableLog>>() {
                @Override
                public Observable<TrackableLog> call(final TrackableConnector trackableConnector) {
                    return Observable.defer(new Func0<Observable<TrackableLog>>() {
                        @Override
                        public Observable<TrackableLog> call() {
                            return trackableConnector.trackableLogInventory();
                        }
                    }).subscribeOn(AndroidRxUtils.networkScheduler);
                }
            }).toList()
        ).subscribe(new Action1<List<TrackableLog>>() {
            @Override
            public void call(final List<TrackableLog> trackableLogs) {
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
        final LogEntry log = DataStore.loadLogOffline(geocode);
        if (log != null) {
            typeSelected = log.getType();
            date.setTime(new Date(log.date));
            text = log.log;
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
                favCheck.setText(getString(R.string.fav_points_remaining, premFavPoints));
            }
        } else {
            favCheck.setVisibility(View.GONE);
        }
    }

    private void setDefaultValues() {
        date = Calendar.getInstance();
        rating = GCVote.NO_RATING;
        typeSelected = cache.getDefaultLogType();
        // it this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && CalendarUtils.isPastEvent(cache) && typeSelected == LogType.ATTENDED) {
            date.setTime(cache.getHiddenDate());
        }
        text = null;
        image = Image.NONE;
    }

    private void clearLog() {
        new ClearLogCommand(this).execute();
    }

    @Override
    public void finish() {
        saveLog(false);
        super.finish();
    }

    @Override
    public void onStop() {
        saveLog(false);
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
    }

    @Override
    public void setDate(final Calendar dateIn) {
        date = dateIn;

        final Button dateButton = ButterKnife.findById(this, R.id.date);
        dateButton.setText(Formatter.formatShortDateVerbally(date.getTime().getTime()));
    }

    public void setType(final LogType type) {
        final Button typeButton = ButterKnife.findById(this, R.id.type);

        typeSelected = type;
        typeButton.setText(typeSelected.getL10n());

        updateTweetBox(type);
        updateLogPasswordBox(type);
        initializeFavoriteCheck();
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

        ClearLogCommand(final Activity context) {
            super(context);
        }

        @Override
        protected void doCommand() {
            oldText = currentLogText();
            oldType = typeSelected;
            oldDate = date;
            cache.clearOfflineLog();
        }

        @Override
        protected void undoCommand() {
            cache.logOffline(getContext(), oldText, oldDate, oldType);
        }

        @Override
        protected String getResultMessage() {
            return getContext().getResources().getString(R.string.info_log_cleared);
        }

        @Override
        protected void onFinished() {
            setDefaultValues();

            setType(typeSelected);
            setDate(date);
            logEditText.setText(StringUtils.EMPTY);

            final EditText logPasswordView = ButterKnife.findById(LogCacheActivity.this, R.id.log_password);
            logPasswordView.setText(StringUtils.EMPTY);
        }

        @Override
        protected void onFinishedUndo() {
            text = oldText;
            typeSelected = oldType;
            date = oldDate;
            setType(typeSelected);
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
                final LogResult logResult = loggingManager.postLog(typeSelected, date, log, logPwd, new ArrayList<>(trackables));
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
                text = currentLogText();
                finish();
            } else if (status == StatusCode.LOG_SAVED) {
                showToast(res.getString(R.string.info_log_saved));
                finish();
            } else {
                Dialogs.confirmPositiveNegativeNeutral(activity, R.string.info_log_post_failed,
                    res.getString(R.string.info_log_post_failed_reason, status.getErrorString(res)),
                    R.string.info_log_post_retry, // Positive Button
                    string.cancel,                // Negative Button
                    R.string.info_log_post_save,  // Neutral Button
                    // Positive button: Retry
                    new OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            sendLogInternal();
                        }
                    },
                    // Negative button: dismiss popup
                    null,
                    // Neutral Button: SaveLog
                    new OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialogInterface, final int i) {
                            saveLog(true);
                            finish();
                        }
                });
            }
        }
    }

    private void saveLog(final boolean force) {
        final String log = currentLogText();

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        if (force || (StringUtils.isNotEmpty(log) && !StringUtils.equals(log, text))) {
            cache.logOffline(this, log, date, typeSelected);
            Settings.setLastCacheLog(log);
        }
        text = log;
    }

    private String currentLogText() {
        return logEditText.getText().toString();
    }

    private String currentLogPassword() {
        return ButterKnife.<EditText>findById(this, R.id.log_password).getText().toString();
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache, null);
    }

    private void selectAllTrackablesAction() {
        final Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(res.getString(R.string.log_tb_changeall));

        final List<LogTypeTrackable> tbLogTypeValues = LogTypeTrackable.getLogTypeTrackableForLogCache();
        final String[] tbLogTypes = getTBLogTypes(tbLogTypeValues);
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                final LogTypeTrackable logType = tbLogTypeValues.get(position);
                for (final TrackableLog tb : trackables) {
                    tb.action = logType;
                    Log.i("Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + logType);
                }
                updateTrackablesList();
                dialog.dismiss();
            }
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
        alert.setSingleChoiceItems(choices, possible.indexOf(typeSelected), new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                setType(possible.get(position));
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private void selectTrackableAction(final View view) {
        final int realViewId = view.getId();
        final Builder alert = new AlertDialog.Builder(this);
        final TrackableLog trackableLog = actionButtons.get(realViewId);
        alert.setTitle(trackableLog.name);
        final List<LogTypeTrackable> tbLogTypeValues = LogTypeTrackable.getLogTypeTrackableForLogCache();
        final String[] tbLogTypes = getTBLogTypes(tbLogTypeValues);
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                final LogTypeTrackable logType = tbLogTypeValues.get(position);
                trackableLog.action = logType;
                Log.i("Trackable " + trackableLog.trackCode + " (" + trackableLog.name + ") has new action: #" + logType);
                updateTrackablesList();
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private void selectImage() {
        final Intent selectImageIntent = new Intent(this, ImageSelectActivity.class);
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE, image);

        startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                image = data.getParcelableExtra(Intents.EXTRA_IMAGE);
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                showToast(getResources().getString(R.string.err_select_logimage_failed));
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
                saveLog(true);
                finish();
                return true;
            case R.id.clear:
                clearLog();
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
            Dialogs.confirm(this, R.string.confirm_log_title, res.getString(R.string.confirm_log_message, typeSelected.getL10n()), new OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    sendLogInternal();
                }
            });
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
        presentShowcase();
        return true;
    }

    @Override
    public ShowcaseViewBuilder getShowcase() {
        return new ShowcaseViewBuilder(this)
                .setTarget(new ActionItemTarget(this, R.id.menu_send))
                .setContent(R.string.showcase_logcache_title, R.string.showcase_logcache_text);
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
}
