package cgeo.geocaching;

import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LogCacheActivity extends AbstractLoggingActivity implements DateDialog.DateDialogParent {
    static final String EXTRAS_GEOCODE = "geocode";
    static final String EXTRAS_ID = "id";

    private static final String SAVED_STATE_RATING = "cgeo.geocaching.saved_state_rating";
    private static final String SAVED_STATE_TYPE = "cgeo.geocaching.saved_state_type";
    private static final String SAVED_STATE_DATE = "cgeo.geocaching.saved_state_date";
    private static final String SAVED_STATE_IMAGE_CAPTION = "cgeo.geocaching.saved_state_image_caption";
    private static final String SAVED_STATE_IMAGE_DESCRIPTION = "cgeo.geocaching.saved_state_image_description";
    private static final String SAVED_STATE_IMAGE_URI = "cgeo.geocaching.saved_state_image_uri";

    private static final int SELECT_IMAGE = 101;

    private LayoutInflater inflater = null;
    private Geocache cache = null;
    private String geocode = null;
    private String text = null;
    private List<LogType> possibleLogTypes = new ArrayList<LogType>();
    private List<TrackableLog> trackables = null;
    private Button postButton = null;
    private CheckBox tweetCheck = null;
    private LinearLayout tweetBox = null;
    private LinearLayout logPasswordBox = null;
    private boolean tbChanged = false;
    private SparseArray<TrackableLog> actionButtons;

    private ILoggingManager loggingManager;

    // Data to be saved while reconfiguring
    private float rating;
    private LogType typeSelected;
    private Calendar date;
    private String imageCaption;
    private String imageDescription;
    private Uri imageUri;


    public void onLoadFinished() {

        if (loggingManager.hasLoaderError()) {
            showErrorLoadingData();
            return;
        }

        trackables = loggingManager.getTrackables();
        possibleLogTypes = loggingManager.getPossibleLogTypes();

        if (possibleLogTypes.isEmpty()) {
            showErrorLoadingData();
            return;
        }

        if (!possibleLogTypes.contains(typeSelected)) {
            typeSelected = possibleLogTypes.get(0);
            setType(typeSelected);

            showToast(res.getString(R.string.info_log_type_changed));
        }

        enablePostButton(true);

        initializeTrackablesAction();
        updateTrackablesList();

        showProgress(false);
    }

    private void showErrorLoadingData() {
        showToast(res.getString(R.string.err_log_load_data));
        showProgress(false);
    }

    private void initializeTrackablesAction() {
        if (Settings.isTrackableAutoVisit()) {
            for (TrackableLog trackable : trackables) {
                trackable.action = LogTypeTrackable.VISITED;
                tbChanged = true;
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
        actionButtons = new SparseArray<TrackableLog>();

        final LinearLayout inventoryView = (LinearLayout) findViewById(R.id.inventory);
        inventoryView.removeAllViews();

        for (TrackableLog tb : trackables) {
            LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.logcache_trackable_item, null);

            ((TextView) inventoryItem.findViewById(R.id.trackcode)).setText(tb.trackCode);
            ((TextView) inventoryItem.findViewById(R.id.name)).setText(tb.name);
            final TextView actionButton = (TextView) inventoryItem.findViewById(R.id.action);
            actionButton.setId(tb.id);
            actionButtons.put(actionButton.getId(), tb);
            actionButton.setText(tb.action.getLabel() + " ▼");
            actionButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    selectTrackableAction(view);
                }
            });

            final String tbCode = tb.trackCode;
            inventoryItem.setClickable(true);
            inventoryItem.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    final Intent trackablesIntent = new Intent(LogCacheActivity.this, TrackableActivity.class);
                    trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
                    startActivity(trackablesIntent);
                }
            });

            inventoryView.addView(inventoryItem);
        }

        if (inventoryView.getChildCount() > 0) {
            findViewById(R.id.inventory_box).setVisibility(View.VISIBLE);
        }
        if (inventoryView.getChildCount() > 1) {
            final LinearLayout inventoryChangeAllView = (LinearLayout) findViewById(R.id.inventory_changeall);

            final Button changeButton = (Button) inventoryChangeAllView.findViewById(R.id.changebutton);
            changeButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    selectAllTrackablesAction();
                }
            });

            inventoryChangeAllView.setVisibility(View.VISIBLE);
        }
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
        postButton.setText(getPostButtonText());
    }

    private String getPostButtonText() {
        if (!postButton.isEnabled()) {
            return res.getString(R.string.log_post_not_possible);
        }
        if (!GCVote.isVotingPossible(cache)) {
            return res.getString(R.string.log_post);
        }
        if (GCVote.isValidRating(rating)) {
            return res.getString(R.string.log_post_rate) + " " + GCVote.getRatingText(rating) + "*";
        }
        return res.getString(R.string.log_post_no_rate);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logcache_activity);

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(EXTRAS_GEOCODE);
            if (StringUtils.isBlank(geocode)) {
                final String cacheid = extras.getString(EXTRAS_ID);
                if (StringUtils.isNotBlank(cacheid)) {
                    geocode = DataStore.getGeocodeForGuid(cacheid);
                }
            }
        }

        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        possibleLogTypes = cache.getPossibleLogTypes();

        if (StringUtils.isNotBlank(cache.getName())) {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getName());
        } else {
            setTitle(res.getString(R.string.log_new_log) + ": " + cache.getGeocode());
        }

        // Get ids for later use
        postButton = (Button) findViewById(R.id.post);
        tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        tweetCheck = (CheckBox) findViewById(R.id.tweet);
        logPasswordBox = (LinearLayout) findViewById(R.id.log_password_box);

        final RatingBar ratingBar = (RatingBar) findViewById(R.id.gcvoteRating);
        initializeRatingBar(ratingBar);

        // initialize with default values
        setDefaultValues();

        // Restore previous state
        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(SAVED_STATE_RATING);
            typeSelected = LogType.getById(savedInstanceState.getInt(SAVED_STATE_TYPE));
            date.setTimeInMillis(savedInstanceState.getLong(SAVED_STATE_DATE));
            imageCaption = savedInstanceState.getString(SAVED_STATE_IMAGE_CAPTION);
            imageDescription = savedInstanceState.getString(SAVED_STATE_IMAGE_DESCRIPTION);
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_STATE_IMAGE_URI));
        } else {
            // If log had been previously saved, load it now, otherwise initialize signature as needed
            final LogEntry log = DataStore.loadLogOffline(geocode);
            if (log != null) {
                typeSelected = log.type;
                date.setTime(new Date(log.date));
                text = log.log;
            } else if (StringUtils.isNotBlank(Settings.getSignature())
                    && Settings.isAutoInsertSignature()
                    && StringUtils.isBlank(currentLogText())) {
                insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, null)), false);
            }
        }
        updatePostButtonText();
        updateImageButton();
        enablePostButton(false);

        final Button typeButton = (Button) findViewById(R.id.type);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectLogType();
            }
        });

        final Button dateButton = (Button) findViewById(R.id.date);
        setDate(date);
        dateButton.setOnClickListener(new DateListener());

        final EditText logView = (EditText) findViewById(R.id.log);
        if (StringUtils.isBlank(currentLogText()) && StringUtils.isNotBlank(text)) {
            logView.setText(text);
            Dialogs.moveCursorToEnd(logView);
        }

        tweetCheck.setChecked(true);
        updateTweetBox(typeSelected);
        updateLogPasswordBox(typeSelected);

        final Button imageButton = (Button) findViewById(R.id.image_btn);
        imageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        final Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveLog(true);
            }
        });

        final Button clearButton = (Button) findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                clearLog();
            }
        });

        loggingManager = cache.getLoggingManager(this);

        loggingManager.init();
    }

    private void initializeRatingBar(RatingBar ratingBar) {
        final TextView label = (TextView) findViewById(R.id.gcvoteLabel);
        if (GCVote.isVotingPossible(cache)) {
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
        }
        ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar ratingBar, float stars, boolean fromUser) {
                // 0.5 is not a valid rating, therefore we must limit
                rating = GCVote.isValidRating(stars) ? stars : 0;
                if (rating < stars) {
                    ratingBar.setRating(rating);
                }
                label.setText(GCVote.getDescription(rating));
                updatePostButtonText();
            }
        });
    }

    private void setDefaultValues() {
        date = Calendar.getInstance();
        rating = GCVote.NO_RATING;
        typeSelected = cache.getDefaultLogType();
        // it this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && DateUtils.isPastEvent(cache) && typeSelected == LogType.ATTENDED) {
            date.setTime(cache.getHiddenDate());
        }
        text = null;
        imageCaption = StringUtils.EMPTY;
        imageDescription = StringUtils.EMPTY;
        imageUri = Uri.EMPTY;
    }

    private void clearLog() {
        cache.clearOfflineLog();

        setDefaultValues();

        setType(typeSelected);
        setDate(date);

        final EditText logView = (EditText) findViewById(R.id.log);
        logView.setText(StringUtils.EMPTY);
        final EditText logPasswordView = (EditText) findViewById(R.id.log_password);
        logPasswordView.setText(StringUtils.EMPTY);

        updateImageButton();

        showToast(res.getString(R.string.info_log_cleared));
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
        outState.putString(SAVED_STATE_IMAGE_URI, imageUri.getPath());
        outState.putString(SAVED_STATE_IMAGE_CAPTION, imageCaption);
        outState.putString(SAVED_STATE_IMAGE_DESCRIPTION, imageDescription);
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

        updateTweetBox(type);
        updateLogPasswordBox(type);

        updatePostButtonText();
    }

    private void updateTweetBox(LogType type) {
        if (type == LogType.FOUND_IT && Settings.isUseTwitter() && Settings.isTwitterLoginValid()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    private void updateLogPasswordBox(LogType type) {
        if (type == LogType.FOUND_IT && cache.isLogPasswordRequired()) {
            logPasswordBox.setVisibility(View.VISIBLE);
        } else {
            logPasswordBox.setVisibility(View.GONE);
        }
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final DateDialog dateDialog = DateDialog.getInstance(date);
            dateDialog.setCancelable(true);
            dateDialog.show(getSupportFragmentManager(), "date_dialog");
        }
    }

    private class PostListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            final String message = res.getString(StringUtils.isBlank(imageUri.getPath()) ?
                    R.string.log_saving :
                    R.string.log_saving_and_uploading);
            new Poster(LogCacheActivity.this, message).execute(currentLogText(), currentLogPassword());
        }
    }

    private class Poster extends AsyncTaskWithProgress<String, StatusCode> {

        public Poster(final Activity activity, final String progressMessage) {
            super(activity, null, progressMessage, true);
        }

        @Override
        protected StatusCode doInBackgroundInternal(final String[] logTexts) {
            final String log = logTexts[0];
            final String logPwd = logTexts.length > 1 ? logTexts[1] : null;
            try {
                final LogResult logResult = loggingManager.postLog(cache, typeSelected, date, log, logPwd, trackables);

                if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                    // update geocache in DB
                    if (typeSelected == LogType.FOUND_IT || typeSelected == LogType.ATTENDED || typeSelected == LogType.WEBCAM_PHOTO_TAKEN) {
                        cache.setFound(true);
                        cache.setVisitedDate(new Date().getTime());
                    }
                    DataStore.saveChangedCache(cache);

                    // update logs in DB
                    ArrayList<LogEntry> newLogs = new ArrayList<LogEntry>(cache.getLogs());
                    final LogEntry logNow = new LogEntry(date.getTimeInMillis(), typeSelected, log);
                    logNow.friend = true;
                    newLogs.add(0, logNow);
                    DataStore.saveLogsWithoutTransaction(cache.getGeocode(), newLogs);

                    // update offline log in DB
                    cache.clearOfflineLog();

                    if (typeSelected == LogType.FOUND_IT) {
                        if (tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                            Twitter.postTweetCache(geocode, logNow);
                        }
                    }
                    if (GCVote.isValidRating(rating)) {
                        GCVote.setRating(cache, rating);
                    }

                    if (StringUtils.isNotBlank(imageUri.getPath())) {
                        ImageResult imageResult = loggingManager.postLogImage(logResult.getLogId(), imageCaption, imageDescription, imageUri);
                        final String uploadedImageUrl = imageResult.getImageUri();
                        if (StringUtils.isNotEmpty(uploadedImageUrl)) {
                            logNow.addLogImage(new Image(uploadedImageUrl, imageCaption, imageDescription));
                            DataStore.saveLogsWithoutTransaction(cache.getGeocode(), newLogs);
                        }
                        return imageResult.getPostResult();
                    }
                }

                return logResult.getPostLogResult();
            } catch (RuntimeException e) {
                Log.e("VisitCacheActivity.Poster.doInBackgroundInternal", e);
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
                showToast(status.getErrorString(res));
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
        }
        text = log;
    }

    private String currentLogText() {
        return ((EditText) findViewById(R.id.log)).getText().toString();
    }

    private String currentLogPassword() {
        return ((EditText) findViewById(R.id.log_password)).getText().toString();
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache, null);
    }

    private void selectAllTrackablesAction() {
        Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(res.getString(R.string.log_tb_changeall));
        String[] tbLogTypes = getTBLogTypes();
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int position) {
                final LogTypeTrackable logType = LogTypeTrackable.values()[position];
                for (TrackableLog tb : trackables) {
                    tb.action = logType;
                }
                tbChanged = true;
                updateTrackablesList();
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private static String[] getTBLogTypes() {
        final LogTypeTrackable[] logTypeValues = LogTypeTrackable.values();
        String[] logTypes = new String[logTypeValues.length];
        for (int i = 0; i < logTypes.length; i++) {
            logTypes[i] = logTypeValues[i].getLabel();
        }
        return logTypes;
    }

    private void selectLogType() {
        // use a local copy of the possible types, as that one might be modified in the background by the loader
        final ArrayList<LogType> possible = new ArrayList<LogType>(possibleLogTypes);

        Builder alert = new AlertDialog.Builder(this);
        String[] choices = new String[possible.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = possible.get(i).getL10n();
        }
        alert.setSingleChoiceItems(choices, possible.indexOf(typeSelected), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int position) {
                setType(possible.get(position));
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private void selectTrackableAction(View view) {
        final int realViewId = view.getId();
        Builder alert = new AlertDialog.Builder(this);
        final TrackableLog trackableLog = actionButtons.get(realViewId);
        alert.setTitle(trackableLog.name);
        String[] tbLogTypes = getTBLogTypes();
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int position) {
                final LogTypeTrackable logType = LogTypeTrackable.values()[position];
                tbChanged = true;
                trackableLog.action = logType;
                Log.i("Trackable " + trackableLog.trackCode + " (" + trackableLog.name + ") has new action: #" + logType);
                updateTrackablesList();
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private void selectImage() {
        Intent selectImageIntent = new Intent(this, ImageSelectActivity.class);
        selectImageIntent.putExtra(ImageSelectActivity.EXTRAS_CAPTION, imageCaption);
        selectImageIntent.putExtra(ImageSelectActivity.EXTRAS_DESCRIPTION, imageDescription);
        selectImageIntent.putExtra(ImageSelectActivity.EXTRAS_URI_AS_STRING, imageUri.toString());

        startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                imageCaption = data.getStringExtra(ImageSelectActivity.EXTRAS_CAPTION);
                imageDescription = data.getStringExtra(ImageSelectActivity.EXTRAS_DESCRIPTION);
                imageUri = Uri.parse(data.getStringExtra(ImageSelectActivity.EXTRAS_URI_AS_STRING));
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                showToast(getResources().getString(R.string.err_select_logimage_failed));
            }
            updateImageButton();

        }
    }

    private void updateImageButton() {
        final Button imageButton = (Button) findViewById(R.id.image_btn);
        if (cache.supportsLogImages()) {
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setText(StringUtils.isNotBlank(imageUri.getPath()) ?
                res.getString(R.string.log_image_edit) : res.getString(R.string.log_image_attach));
        } else {
            imageButton.setVisibility(View.GONE);
        }
    }
}
