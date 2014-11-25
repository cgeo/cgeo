package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.ShowcaseViewBuilder;
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
import cgeo.geocaching.ui.dialog.DateDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import com.github.amlcurran.showcaseview.targets.ActionItemTarget;

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
import android.view.Menu;
import android.view.MenuItem;
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
    private List<LogType> possibleLogTypes = new ArrayList<>();
    private List<TrackableLog> trackables = null;
    protected @InjectView(R.id.tweet) CheckBox tweetCheck;
    protected @InjectView(R.id.tweet_box) LinearLayout tweetBox;
    protected @InjectView(R.id.log_password_box) LinearLayout logPasswordBox;
    private SparseArray<TrackableLog> actionButtons;

    private ILoggingManager loggingManager;

    // Data to be saved while reconfiguring
    private float rating;
    private LogType typeSelected;
    private Calendar date;
    private String imageCaption;
    private String imageDescription;
    private Uri imageUri;
    private boolean sendButtonEnabled;
    private boolean isRatingBarShown = false;

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

        initializeRatingBar();

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

        for (final TrackableLog tb : trackables) {
            final LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.logcache_trackable_item, inventoryView, false);

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

            final String tbCode = tb.trackCode;
            inventoryItem.setClickable(true);
            ButterKnife.findById(inventoryItem, R.id.info).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View view) {
                    final Intent trackablesIntent = new Intent(LogCacheActivity.this, TrackableActivity.class);
                    trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
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

    private void enablePostButton(final boolean enabled) {
        sendButtonEnabled = enabled;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logcache_activity);

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

        final EditText logView = ButterKnife.findById(this, R.id.log);
        if (StringUtils.isBlank(currentLogText()) && StringUtils.isNotBlank(text)) {
            logView.setText(text);
            Dialogs.moveCursorToEnd(logView);
        }

        tweetCheck.setChecked(true);
        updateTweetBox(typeSelected);
        updateLogPasswordBox(typeSelected);

        loggingManager = cache.getLoggingManager(this);

        loggingManager.init();
        requestKeyboardForLogging();
    }

    private void initializeRatingBar() {
        if (GCVote.isVotingPossible(cache) && !isRatingBarShown) {
            final RatingBar ratingBar = ButterKnife.findById(this, R.id.gcvoteRating);
            final TextView label = ButterKnife.findById(this, R.id.gcvoteLabel);
            isRatingBarShown = true;
            ratingBar.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

                @Override
                public void onRatingChanged(final RatingBar ratingBar, final float stars, final boolean fromUser) {
                    // 0.5 is not a valid rating, therefore we must limit
                    rating = GCVote.isValidRating(stars) ? stars : 0;
                    if (rating < stars) {
                        ratingBar.setRating(rating);
                    }
                    label.setText(GCVote.getDescription(rating));
                }
            });
            ratingBar.setRating(cache.getMyVote());
        }
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

        final EditText logView = ButterKnife.findById(this, R.id.log);
        logView.setText(StringUtils.EMPTY);
        final EditText logPasswordView = ButterKnife.findById(this, R.id.log_password);
        logPasswordView.setText(StringUtils.EMPTY);

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
    }

    private void updateTweetBox(final LogType type) {
        if (type == LogType.FOUND_IT && Settings.isUseTwitter() && Settings.isTwitterLoginValid()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    private void updateLogPasswordBox(final LogType type) {
        if (type == LogType.FOUND_IT && cache.isLogPasswordRequired()) {
            logPasswordBox.setVisibility(View.VISIBLE);
        } else {
            logPasswordBox.setVisibility(View.GONE);
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

    private class Poster extends AsyncTaskWithProgress<String, StatusCode> {

        public Poster(final Activity activity, final String progressMessage) {
            super(activity, null, progressMessage, true);
        }

        @Override
        protected StatusCode doInBackgroundInternal(final String[] logTexts) {
            final String log = logTexts[0];
            final String logPwd = logTexts.length > 1 ? logTexts[1] : null;
            try {
                final LogResult logResult = loggingManager.postLog(typeSelected, date, log, logPwd, trackables);

                if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                    // update geocache in DB
                    if (typeSelected.isFoundLog()) {
                        cache.setFound(true);
                        cache.setVisitedDate(date.getTimeInMillis());
                    }
                    DataStore.saveChangedCache(cache);

                    // update logs in DB
                    final ArrayList<LogEntry> newLogs = new ArrayList<>(cache.getLogs());
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
                    if (GCVote.isValidRating(rating) && GCVote.isVotingPossible(cache)) {
                        if (!GCVote.setRating(cache, rating)) {
                            showToast(res.getString(R.string.err_gcvote_send_rating));
                        }
                    }

                    if (StringUtils.isNotBlank(imageUri.getPath())) {
                        final ImageResult imageResult = loggingManager.postLogImage(logResult.getLogId(), imageCaption, imageDescription, imageUri);
                        final String uploadedImageUrl = imageResult.getImageUri();
                        if (StringUtils.isNotEmpty(uploadedImageUrl)) {
                            logNow.addLogImage(new Image(uploadedImageUrl, imageCaption, imageDescription));
                            DataStore.saveLogsWithoutTransaction(cache.getGeocode(), newLogs);
                        }
                        return imageResult.getPostResult();
                    }
                }

                return logResult.getPostLogResult();
            } catch (final RuntimeException e) {
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
            Settings.setLastCacheLog(log);
        }
        text = log;
    }

    private String currentLogText() {
        return ButterKnife.<EditText>findById(this, R.id.log).getText().toString();
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
        final String[] tbLogTypes = getTBLogTypes();
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                final LogTypeTrackable logType = LogTypeTrackable.values()[position];
                for (final TrackableLog tb : trackables) {
                    tb.action = logType;
                }
                updateTrackablesList();
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private static String[] getTBLogTypes() {
        final LogTypeTrackable[] logTypeValues = LogTypeTrackable.values();
        final String[] logTypes = new String[logTypeValues.length];
        for (int i = 0; i < logTypes.length; i++) {
            logTypes[i] = logTypeValues[i].getLabel();
        }
        return logTypes;
    }

    private void selectLogType() {
        // use a local copy of the possible types, as that one might be modified in the background by the loader
        final ArrayList<LogType> possible = new ArrayList<>(possibleLogTypes);

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
        final String[] tbLogTypes = getTBLogTypes();
        alert.setItems(tbLogTypes, new OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                final LogTypeTrackable logType = LogTypeTrackable.values()[position];
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
        selectImageIntent.putExtra(Intents.EXTRA_CAPTION, imageCaption);
        selectImageIntent.putExtra(Intents.EXTRA_DESCRIPTION, imageDescription);
        selectImageIntent.putExtra(Intents.EXTRA_URI_AS_STRING, imageUri.toString());

        startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                imageCaption = data.getStringExtra(Intents.EXTRA_CAPTION);
                imageDescription = data.getStringExtra(Intents.EXTRA_DESCRIPTION);
                imageUri = Uri.parse(data.getStringExtra(Intents.EXTRA_URI_AS_STRING));
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
        if (typeSelected.mustConfirmLog()) {
            Dialogs.confirm(this, R.string.confirm_log_title, res.getString(R.string.confirm_log_message, typeSelected.getL10n()), new OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    sendLogInternal();
                }
            });
        }
        else {
            sendLogInternal();
        }
    }

    private void sendLogInternal() {
        final String message = res.getString(StringUtils.isBlank(imageUri.getPath()) ?
                R.string.log_saving :
                R.string.log_saving_and_uploading);
        new Poster(this, message).execute(currentLogText(), currentLogPassword());
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
