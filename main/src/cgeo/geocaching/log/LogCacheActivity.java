package cgeo.geocaching.log;

import cgeo.geocaching.ImageSelectActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.CacheVotingBar;
import cgeo.geocaching.ui.DateTimeEditor;
import cgeo.geocaching.ui.ImageActivityHelper;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ViewUtils;

import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import butterknife.BindView;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class LogCacheActivity extends AbstractLoggingActivity {

    private static final String SAVED_STATE_LOGENTRY = "cgeo.geocaching.saved_state_logentry";

    private enum SaveMode { NORMAL, FORCE, SKIP }

    private static final int SELECT_IMAGE = 101;
    private final Set<TrackableLog> trackables = new HashSet<>();
    @BindView(R.id.tweet)
    protected CheckBox tweetCheck;
    @BindView(R.id.log_password_box)
    protected LinearLayout logPasswordBox;
    @BindView(R.id.log_password)
    protected EditText logPassword;
    @BindView(R.id.favorite_check)
    protected CheckBox favCheck;
    @BindView(R.id.log)
    protected EditText logEditText;
    @BindView(R.id.log_image_add_multi)
    protected View logImageAddMultiButton;
    @BindView(R.id.log_image_add_camera)
    protected View logImageAddCameraButton;

    private Geocache cache = null;
    private String geocode = null;
    private ILoggingManager loggingManager;
    private final ImageActivityHelper imageHelper = new ImageActivityHelper(this, 3000);

    private OfflineLogEntry lastSavedState = null;
    private final Set<String> lastSavedStateImagePaths = new HashSet<>();

    private CacheVotingBar cacheVotingBar = new CacheVotingBar();
    private final TextSpinner<LogType> logType = new TextSpinner<>();
    private final DateTimeEditor date = new DateTimeEditor();
    private final List<Image> images = new ArrayList<>();

    private boolean sendButtonEnabled;
    private final TextSpinner<ReportProblemType> reportProblem = new TextSpinner<>();
    private final TextSpinner<LogTypeTrackable> trackableActionsChangeAll = new TextSpinner<>();

    private LogImageAdapter imageListAdapter;

    public static Intent getLogCacheIntent(final Activity context, final String cacheId, final String geocode) {
        final Intent logVisitIntent = new Intent(context, LogCacheActivity.class);
        logVisitIntent.putExtra(Intents.EXTRA_ID, cacheId);
        logVisitIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        return logVisitIntent;
    }

    /**
     * Hook called at the beginning of onCreateLoader().
     */
    public void onLoadStarted() {
        Log.v("LogCacheActivity.onLoadStarted()");
        showProgress(true);
    }

    /**
     * Hook called at the beginning of onLoadFinished().
     */
    public void onLoadFinished() {
        Log.v("LogCacheActivity.onLoadFinished()");
        if (loggingManager.hasLoaderError()) {
            showErrorLoadingData();
            return;
        }

        if (!loggingManager.hasTrackableLoadError()) {
            trackables.addAll(initializeTrackableActions(loggingManager.getTrackables(), lastSavedState));
        } else {
            showErrorLoadingAdditionalData();
        }

        if (loggingManager.getPossibleLogTypes().isEmpty()) {
            showErrorLoadingData();
            return;
        }

        refreshGui();
        enablePostButton(true);
        showProgress(false);
    }

    private void verifySelectedReportProblemType() {
        final List<ReportProblemType> possibleReportProblemTypes = new ArrayList<>();
        possibleReportProblemTypes.add(ReportProblemType.NO_PROBLEM);
        for (final ReportProblemType reportProblem : loggingManager.getReportProblemTypes(cache)) {
            if (reportProblem.isVisible(logType.get(), cache.getType())) {
                possibleReportProblemTypes.add(reportProblem);
            }
        }
        reportProblem.setValues(possibleReportProblemTypes);

        final View reportProblemBox = findViewById(R.id.report_problem_box);
        if (possibleReportProblemTypes.size() == 1) {
            reportProblemBox.setVisibility(View.GONE);
        } else {
            reportProblemBox.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorLoadingData() {
        showToast(res.getString(R.string.err_log_load_data));
        showProgress(false);
    }

    private void showErrorLoadingAdditionalData() {
        showToast(res.getString(R.string.warn_log_load_additional_data));
    }

    private List<TrackableLog> initializeTrackableActions(final List<TrackableLog> tLogs, final OfflineLogEntry savedState) {
        return CollectionStream.of(tLogs).map(tLog -> initializeTrackableAction(tLog, savedState)).toList();
    }

    private TrackableLog initializeTrackableAction(final TrackableLog tLog, final OfflineLogEntry savedState) {
        if (savedState != null && savedState.trackableActions.containsKey(tLog.trackCode)) {
            tLog.setAction(savedState.trackableActions.get(tLog.trackCode));
        } else {
            tLog.setAction(Settings.isTrackableAutoVisit() ? LogTypeTrackable.VISITED : LogTypeTrackable.DO_NOTHING);
        }
        return tLog;
    }

    private void updateTrackablesList() {
        final TrackableLog[] trackablesArray = getSortedTrackables().toArray(new TrackableLog[trackables.size()]);
        final ListView inventoryList = findViewById(R.id.inventory);
        inventoryList.setAdapter(new TrackableLogAdapter(this, R.layout.logcache_trackable_item, trackablesArray));
        ViewUtils.setListViewHeightBasedOnItems(inventoryList);

        findViewById(R.id.inventory_box).setVisibility(trackables.isEmpty() ? View.GONE : View.VISIBLE);

        final LinearLayout inventoryChangeAllView = findViewById(R.id.inventory_changeall);
        inventoryChangeAllView.setVisibility(trackables.size() > 1 ? View.VISIBLE : View.GONE);

        trackableActionsChangeAll.setTextDialogTitle(getString(R.string.log_tb_changeall) + " (" + trackables.size() + ")");
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

        date.init(findViewById(R.id.date), null, getSupportFragmentManager());
        logType.setTextView(findViewById(R.id.type)).setDisplayMapper(LogType::getL10n);
        reportProblem.setTextView(findViewById(R.id.report_problem))
                .setTextDisplayMapper(rp -> rp.getL10n() + " ▼")
                .setDisplayMapper(ReportProblemType::getL10n);
        initializeImageList();
        this.logImageAddMultiButton.setOnClickListener(v ->
            imageHelper.getMultipleImagesFromStorage(getFastImageAutoScale(), true, false, imgs -> {
                    images.addAll(imgs);
                    updateImageList();
                }));
        this.logImageAddCameraButton.setOnClickListener(v ->
            imageHelper.getImageFromCamera(getFastImageAutoScale(), true, false, img -> {
                images.add(img);
                updateImageList();
            }));

        //init trackable "change all" button
        trackableActionsChangeAll.setTextView(findViewById(R.id.changebutton))
                .setTextDialogTitle(getString(R.string.log_tb_changeall))
                .setTextDisplayMapper(lt -> getString(R.string.log_tb_changeall))
                .setDisplayMapper(LogTypeTrackable::getLabel)
                .setValues(LogTypeTrackable.getLogTypeTrackableForLogCache())
                .set(Settings.isTrackableAutoVisit() ? LogTypeTrackable.VISITED : LogTypeTrackable.DO_NOTHING)
                .setChangeListener(lt -> {
                    CollectionStream.of(trackables).forEach(t -> t.action = lt);
                    updateTrackablesList();
                }, false);

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
        logType.setValues(cache.getPossibleLogTypes());
        cacheVotingBar.initialize(cache, getWindow().getDecorView().getRootView(), null);

        setCacheTitleBar(cache);
        //initializeRatingBar();

        loggingManager = cache.getLoggingManager(this);
        loggingManager.init();

        // initialize with default values
        resetValues();
        logType.setChangeListener(lt -> refreshGui());

        // Restore previous state
        lastSavedState = restorePreviousLogEntry(savedInstanceState);
        if (lastSavedState == null) {
            //this means there is no previous entry
            lastSavedState = getEntryFromView();
            lastSavedStateImagePaths.clear();
            //set initial signature. Setting this AFTER setting lastSavedState and GUI leads to the signature being a change-to-save as requested in #8973
            if (StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature() && StringUtils.isBlank(currentLogText())) {
                insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, lastSavedState)), false);
            }
        } else {
            fillViewFromEntry(lastSavedState);
        }
        adjustImageStateTo(lastSavedState, false);

        // TODO: Why is it disabled in onCreate?
        // Probably it should be disabled only when there is some explicit issue.
        // See https://github.com/cgeo/cgeo/issues/7188
        enablePostButton(false);

        refreshGui();

        // Load Generic Trackables
        AndroidRxUtils.bindActivity(this,
                // Obtain the actives connectors
                Observable.fromIterable(ConnectorFactory.getLoggableGenericTrackablesConnectors())
                        .flatMap((Function<TrackableConnector, Observable<TrackableLog>>) trackableConnector -> Observable.defer(trackableConnector::trackableLogInventory).subscribeOn(AndroidRxUtils.networkScheduler)).toList()
        ).subscribe(trackableLogs -> {
            // Store trackables
            trackables.addAll(initializeTrackableActions(trackableLogs, lastSavedState));
            // Update the UI
            updateTrackablesList();
        });

        requestKeyboardForLogging();
    }

    private void setLogText(final String newText) {
        logEditText.setText(newText == null ? StringUtils.EMPTY : newText);
        Dialogs.moveCursorToEnd(logEditText);
    }

    private int getFastImageAutoScale() {
        int scale = Settings.getLogImageScale();
        if (scale <= 0) {
            scale = 1024;
        }
        return scale;
    }

    private void adjustImageStateTo(final LogEntry entry, final boolean deleteOld) {
        if (deleteOld) {
            //delete all images on device which are in old state but not in new
            final Set<String> existingPaths = CollectionStream.of(entry.logImages).map(img -> img.getPath()).toSet();
            for (final String oldPath : lastSavedStateImagePaths) {
                if (!existingPaths.contains(oldPath)) {
                    final boolean result = new File(oldPath).delete();
                    Log.i("Deleting log image " + oldPath + " (result: " + result + ")");
                }
            }
        }
        //refill image state
        lastSavedStateImagePaths.clear();
        for (final Image img : entry.logImages) {
            if (!img.getPath().isEmpty()) {
                lastSavedStateImagePaths.add(img.getPath());
            }
        }
    }

    private OfflineLogEntry restorePreviousLogEntry(final Bundle savedInstanceState) {
        OfflineLogEntry entry = null;
        if (savedInstanceState != null) {
            entry = savedInstanceState.getParcelable(SAVED_STATE_LOGENTRY);
        }
        if (entry == null) {
            entry = DataStore.loadLogOffline(geocode);
        }

        return entry;
    }

    private void fillViewFromEntry(final OfflineLogEntry logEntry) {
        logType.set(logEntry.logType);
        date.setDate(new Date(logEntry.date));
        setLogText(logEntry.log);
        reportProblem.set(logEntry.reportProblem);
        cacheVotingBar.setRating(logEntry.rating == null ? cache.getMyVote() : logEntry.rating);
        favCheck.setChecked(logEntry.favorite);
        tweetCheck.setChecked(logEntry.tweet);
        logPassword.setText(logEntry.password);

        images.clear();
        images.addAll(logEntry.logImages);
        updateImageList();

        CollectionStream.of(trackables).forEach(t -> initializeTrackableAction(t, logEntry));
        updateTrackablesList();
    }

    private OfflineLogEntry getEntryFromView() {
        final OfflineLogEntry.Builder<?> builder = new OfflineLogEntry.Builder<>()
                .setLogType(logType.get())
                .setDate(date.getDate().getTime())
                .setLog(currentLogText())
                .setReportProblem(reportProblem.get())
                .setRating(cacheVotingBar.getRating())
                .setFavorite(favCheck.isChecked())
                .setTweet(tweetCheck.isChecked())
                .setPassword(logPassword.getText().toString());
        CollectionStream.of(images).forEach(builder::addLogImage);
        CollectionStream.of(trackables).forEach(t -> builder.addTrackableAction(t.trackCode, t.action));

        return builder.build();
    }
    /**
     * Checks whether there are favorite points available and sets the corresponding visibility of
     * "add to favorite" checkbox.
     */
    private void initializeFavoriteCheck() {
        final IConnector connector = ConnectorFactory.getConnector(cache);

        if ((connector instanceof IFavoriteCapability) && ((IFavoriteCapability) connector).supportsAddToFavorite(cache, logType.get()) && loggingManager instanceof ILoggingWithFavorites) {
            final int favoritePoints = ((ILoggingWithFavorites) loggingManager).getFavoritePoints();
            if (favoritePoints > 0) {
                favCheck.setVisibility(View.VISIBLE);
                favCheck.setText(res.getQuantityString(R.plurals.fav_points_remaining, favoritePoints, favoritePoints));
            }
        } else {
            favCheck.setVisibility(View.GONE);
        }
    }

    private void resetValues() {
        setType(cache.getDefaultLogType());
        final Calendar defaultDate = Calendar.getInstance();
        // it this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && CalendarUtils.isPastEvent(cache) && logType.get() == LogType.ATTENDED) {
            defaultDate.setTime(cache.getHiddenDate());
        }
        date.setCalendar(defaultDate);
        setLogText(null);
        reportProblem.set(ReportProblemType.NO_PROBLEM);
        cacheVotingBar.setRating(cache.getMyVote());
        images.clear();
        tweetCheck.setChecked(true);
        favCheck.setChecked(false);
        logPassword.setText(StringUtils.EMPTY);

        final EditText logPasswordView = LogCacheActivity.this.findViewById(R.id.log_password);
        logPasswordView.setText(StringUtils.EMPTY);

        //force copy due to #9101
        CollectionStream.of(trackables, true).forEach(tl -> initializeTrackableAction(tl, null));
    }

    private void clearLog() {
        new ClearLogCommand(this).execute();
    }

    @Override
    public void finish() {
        finish(SaveMode.NORMAL);
    }

    public void finish(final SaveMode saveMode) {
        saveLog(saveMode);
        super.finish();
    }

    @Override
    public void onStop() {
        saveLog();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_STATE_LOGENTRY, getEntryFromView());
    }

    public void setType(final LogType type) {
        logType.set(type);
        refreshGui();
    }

    public void refreshGui() {
        updateTweetBox(logType.get());
        updateLogPasswordBox(logType.get());
        cacheVotingBar.validateVisibility(cache, logType.get());
        initializeFavoriteCheck();
        verifySelectedReportProblemType();
        updateTrackablesList();
        updateImageList();
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

    private void saveLog() {
        saveLog(SaveMode.NORMAL);
    }

    private void saveLog(final SaveMode saveMode) {

        try (ContextLogger cLog = new ContextLogger("LogCacheActivity.saveLog(mode=%s)", saveMode)) {

            final OfflineLogEntry logEntry = getEntryFromView();

            final boolean logChanged = logEntry.hasSaveRelevantChanges(lastSavedState);
            final boolean doSave = SaveMode.FORCE.equals(saveMode) || (!SaveMode.SKIP.equals(saveMode) && logChanged);
            cLog.add("logChanged=%b, doSave=%b", logChanged, doSave);

            if (doSave) {
                lastSavedState = logEntry;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(final Void... params) {
                        try (ContextLogger ccLog = new ContextLogger("LogCacheActivity.saveLog.doInBackground(gc=%s)", cache.getGeocode())) {
                            cache.logOffline(LogCacheActivity.this, logEntry);
                            Settings.setLastCacheLog(logEntry.log);
                            ccLog.add("log=%s", logEntry.log);
                            adjustImageStateTo(lastSavedState, true);
                            return null;
                        }
                    }
                }.execute();
        }
        }
    }

    private String currentLogText() {
        return logEditText.getText().toString();
    }

    private String currentLogPassword() {
        return logPassword.getText().toString();
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache, getEntryFromView());
    }

    private void addOrEditImage(final int imageIndex) {
        final Intent selectImageIntent = new Intent(this, ImageSelectActivity.class);
        if (imageIndex >= 0 && imageIndex < images.size()) {
            selectImageIntent.putExtra(Intents.EXTRA_IMAGE, images.get(imageIndex));
        }
        selectImageIntent.putExtra(Intents.EXTRA_INDEX, imageIndex);
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
        selectImageIntent.putExtra(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE, loggingManager.getMaxImageUploadSize());
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE_CAPTION_MANDATORY, loggingManager.isImageCaptionMandatory());

        startActivityForResult(selectImageIntent, SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        if (imageHelper.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                final int imageIndex = data.getIntExtra(Intents.EXTRA_INDEX, -1);
                final boolean indexIsValid = imageIndex >= 0 && imageIndex < images.size();
                final boolean deleteFlag = data.getBooleanExtra(Intents.EXTRA_DELETE_FLAG, false);
                final Image image = data.getParcelableExtra(Intents.EXTRA_IMAGE);
                if (deleteFlag && indexIsValid) {
                    images.remove(imageIndex);
                } else if (image != null && indexIsValid) {
                    images.set(imageIndex, image);
                } else if (image != null) {
                    images.add(image);
                }
                updateImageList();
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed, advise user
                showToast(getString(R.string.err_select_logimage_failed));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.v("LogCacheActivity.onOptionsItemSelected(" + item.getItemId() + "/" + item.getTitle() + ")");
        switch (item.getItemId()) {
            case R.id.menu_send:
                sendLogAndConfirm();
                return true;
            case R.id.menu_image:
                addOrEditImage(-1);
                return true;
            case R.id.save:
                finish(SaveMode.FORCE);
                return true;
            case R.id.clear:
                clearLog();
                return true;
            case R.id.menu_sort_trackables_name:
                sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_NAME);
                return true;
            case R.id.menu_sort_trackables_code:
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
        if (CalendarUtils.isFuture(date.getCalendar())) {
            Dialogs.message(this, R.string.log_date_future_not_allowed);
            return;
        }
        if (logType.get().mustConfirmLog()) {
            Dialogs.confirm(this, R.string.confirm_log_title, res.getString(R.string.confirm_log_message, logType.get().getL10n()), (dialog, which) -> sendLogInternal());
        } else if (reportProblem.get() != ReportProblemType.NO_PROBLEM) {
            Dialogs.confirm(this, R.string.confirm_report_problem_title, res.getString(R.string.confirm_report_problem_message, reportProblem.get().getL10n()), (dialog, which) -> sendLogInternal());
        } else {
            sendLogInternal();
        }
    }

    private void sendLogInternal() {
        lastSavedState = getEntryFromView();
        new Poster(this, res.getString(R.string.log_saving)).execute(currentLogText(), currentLogPassword());
        Settings.setLastCacheLog(currentLogText());

        final IConnector cacheConnector = ConnectorFactory.getConnector(cache);
        if (cacheConnector instanceof ILogin && ((ILogin) cacheConnector).isLoggedIn() && ((ILogin) cacheConnector).getCachesFound() >= 0) {
            FoundNumCounter.updateFoundNum(((ILogin) cacheConnector).getName(), ((ILogin) cacheConnector).getCachesFound());
        }
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

    @Override
    protected String getLastLog() {
        return Settings.getLastCacheLog();
    }

    private void sortTrackables(final TrackableComparator comparator) {
        Settings.setTrackableComparator(comparator);
        updateTrackablesList();
        invalidateOptionsMenuCompatible();
    }

    protected static class ViewHolder extends AbstractViewHolder {
        @BindView(R.id.trackable_image_brand)
        protected ImageView brandView;
        @BindView(R.id.trackcode)
        protected TextView codeView;
        @BindView(R.id.name)
        protected TextView nameView;
        private final TextSpinner<LogTypeTrackable> trackableAction = new TextSpinner<>();
        @BindView(R.id.info)
        protected View infoView;

        public ViewHolder(final View rowView) {
            super(rowView);
        }
    }

    private final class TrackableLogAdapter extends ArrayAdapter<TrackableLog> {
        private TrackableLogAdapter(final Context context, final int resource, final TrackableLog[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = getLayoutInflater().inflate(R.layout.logcache_trackable_item, parent, false);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();
            if (holder == null) {
                holder = new ViewHolder(rowView);
            }

            final TrackableLog trackable = getItem(position);
            fillViewHolder(holder, trackable, rowView.findViewById(R.id.action));
            return rowView;
        }

        private void fillViewHolder(final ViewHolder holder, final TrackableLog trackable, final TextView action) {
            holder.brandView.setImageResource(trackable.brand.getIconResource());
            holder.codeView.setText(trackable.trackCode);
            holder.nameView.setText(trackable.name);

            holder.trackableAction.setTextView(action)
                    .setTextDialogTitle(trackable.name)
                    .setTextDisplayMapper(lt -> lt.getLabel() + " ▼")
                    .setDisplayMapper(LogTypeTrackable::getLabel)
                    .setValues(LogTypeTrackable.getLogTypeTrackableForLogCache())
                    .set(trackable.action)
                    .setChangeListener(lt -> {
                        trackable.action = lt;
                        updateTrackablesList();
                    });


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

    private void updateImageList() {
        //must create NEW (!) image list, otherwise update of list view will not work!
        final List<Image> newImages = new ArrayList<>();
        newImages.addAll(images);
        imageListAdapter.submitList(newImages,
            //following itemrangechanged event is necessary to update default image name counts (eg "Image 1", "Image 2", ...)
            () -> imageListAdapter.notifyItemRangeChanged(0, imageListAdapter.getItemCount()));
    }

    private void initializeImageList() {
        imageListAdapter = new LogImageAdapter();
        final RecyclerView imageList = findViewById(R.id.image_list);
        imageList.setAdapter(imageListAdapter);
        imageList.setLayoutManager(new LinearLayoutManager(this));
    }

    private String getImageTitle(final Image image, final int position) {
        if (!StringUtils.isBlank(image.getTitle())) {
            return image.getTitle();
        }
        return getString(R.string.log_image_titleprefix) + " " + (position + 1);
    }


    protected static class LogImageViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.log_image_thumbnail)
        protected ImageView imageThumbnail;
        @BindView(R.id.log_image_title)
        protected TextView imageTitle;
        @BindView(R.id.log_image_description)
        protected TextView imageDescription;
        @BindView(R.id.log_image_info)
        protected TextView imageInfo;
        @BindView(R.id.log_image_delete)
        protected View imageDeleteButton;

        public LogImageViewHolder(final View rowView) {
            super(rowView);
        }
    }


    private final class LogImageAdapter extends ListAdapter<Image, LogImageViewHolder> {

        private LogImageAdapter() {
            super(new DiffUtil.ItemCallback<Image>() {
                @Override
                public boolean areItemsTheSame(@NonNull final Image oldItem, @NonNull final Image newItem) {
                    return Objects.equals(oldItem.getPath(), newItem.getPath());
                }

                @Override
                public boolean areContentsTheSame(@NonNull final Image oldItem, @NonNull final Image newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            });
        }

        private void fillViewHolder(final LogImageViewHolder holder, final Image image, final int position) {
            if (image == null || holder.imageThumbnail == null) {
                return;
            }
            //holder.imageThumbnail.setImageURI(image.getUri());
            ImageActivityHelper.displayImageAsync(image, holder.imageThumbnail);
            holder.imageTitle.setText(getImageTitle(image, position));
            holder.imageDescription.setText(image.getDescription());
            holder.imageInfo.setText(getImageInfo(image));
        }

        private String getImageInfo(final Image image) {
            final File imgFile = image.getFile();
            if (imgFile == null) {
                return "---";
            }
            if (!imgFile.exists()) {
                return ImageUtils.getRelativePathToOutputImageDir(imgFile);
            }
            int width = 0;
            int height = 0;
            final ImmutablePair<Integer, Integer> widthHeight = ImageUtils.getImageSize(imgFile);
            if (widthHeight != null) {
                width = widthHeight.getLeft();
                height = widthHeight.getRight();
            }
            return getString(R.string.log_image_info, width, height, imgFile.length() / 1024, ImageUtils.getRelativePathToOutputImageDir(imgFile));
        }

        @NonNull
        @Override
        public LogImageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.logcache_image_item, parent, false);
            final LogImageViewHolder viewHolder = new LogImageViewHolder(view);
            viewHolder.itemView.setOnClickListener(view1 -> addOrEditImage(viewHolder.getAdapterPosition()));
            viewHolder.imageDeleteButton.setOnClickListener(v -> {
                images.remove(viewHolder.getAdapterPosition());
                updateImageList();
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final LogImageViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position), position);
        }

    }


    private final class ClearLogCommand extends AbstractCommand {

        private OfflineLogEntry previousState;

        ClearLogCommand(final Activity context) {
            super(context);
        }

        @Override
        protected void doCommand() {
            previousState = getEntryFromView();
            cache.clearOfflineLog();

        }

        @Override
        protected void undoCommand() {
            cache.logOffline(getContext(), previousState);
        }

        @Override
        protected String getResultMessage() {
            return getContext().getString(R.string.info_log_cleared);
        }

        @Override
        protected void onFinished() {
            resetValues();
            refreshGui();
            lastSavedState = getEntryFromView();
        }

        @Override
        protected void onFinishedUndo() {
            fillViewFromEntry(previousState);
            refreshGui();
        }
    }

    private class Poster extends AsyncTaskWithProgressText<String, StatusCode> {

        Poster(final Activity activity, final String progressMessage) {
            super(activity, res.getString(images.isEmpty() ?
                    R.string.log_posting_log :
                    R.string.log_saving_and_uploading), progressMessage);
        }

        @Override
        protected StatusCode doInBackgroundInternal(final String[] logTexts) {

            final String log = logTexts[0];
            final String logPwd = logTexts.length > 1 ? logTexts[1] : null;

            final ContextLogger cLog = new ContextLogger("LCA.Poster.doInBackgroundInternal(%s)", log);
            try {
                final LogResult logResult = loggingManager.postLog(logType.get(), date.getCalendar(),
                        log, logPwd, new ArrayList<>(trackables), reportProblem.get());
                ImageResult imageResult = null;

                if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                    // update geocache in DB
                    if (logType.get().isFoundLog()) {
                        cache.setFound(true);
                        cache.setVisitedDate(date.getDate().getTime());
                    }
                    DataStore.saveChangedCache(cache);

                    final LogEntry.Builder logBuilder = new LogEntry.Builder()
                            .setServiceLogId(logResult.getServiceLogId())
                            .setDate(date.getDate().getTime())
                            .setLogType(logType.get())
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
                    if (!images.isEmpty()) {
                        publishProgress(res.getString(R.string.log_posting_image));
                        int pos = 0;
                        for (Image img : images) {
                            final Image imgToSend = img.buildUpon().setTitle(getImageTitle(img, pos++)).build();
                            imageResult = loggingManager.postLogImage(logResult.getLogId(), imgToSend);
                            final String uploadedImageUrl = imageResult.getImageUri();
                            if (StringUtils.isNotEmpty(uploadedImageUrl)) {
                                logBuilder.addLogImage(imgToSend.buildUpon()
                                        .setUrl(uploadedImageUrl)
                                        .build());
                            }
                        }
                    }

                    // update logs in DB
                    final List<LogEntry> newLogs = new ArrayList<>(cache.getLogs());
                    final LogEntry logNow = logBuilder.build();
                    newLogs.add(0, logNow);
                    if (reportProblem.get() != ReportProblemType.NO_PROBLEM) {
                        final LogEntry logProblem = logBuilder.setLog(getString(reportProblem.get().textId)).setLogImages(Collections.emptyList()).setLogType(reportProblem.get().logType).build();
                        newLogs.add(0, logProblem);
                    }
                    DataStore.saveLogs(cache.getGeocode(), newLogs);

                    // update offline log in DB
                    cache.clearOfflineLog();

                    if (logType.get() == LogType.FOUND_IT && tweetCheck.isChecked() && tweetCheck.getVisibility() == View.VISIBLE) {
                        publishProgress(res.getString(R.string.log_posting_twitter));
                        Twitter.postTweetCache(geocode, logNow);
                    }

                    // Post cache rating
                    if (cacheConnector instanceof IVotingCapability) {
                        final IVotingCapability votingConnector = (IVotingCapability) cacheConnector;
                        if (votingConnector.supportsVoting(cache) && votingConnector.isValidRating(cacheVotingBar.getRating())) {
                            publishProgress(res.getString(R.string.log_posting_vote));
                            if (votingConnector.postVote(cache, cacheVotingBar.getRating())) {
                                cache.setMyVote(cacheVotingBar.getRating());
                                DataStore.saveChangedCache(cache);
                            } else {
                                showToast(res.getString(R.string.err_vote_send_rating));
                            }
                        }
                    }


                    // Posting Generic Trackables
                    for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
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
                                manager.postLog(cache, trackableLog, date.getCalendar(), log);
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
                cLog.setException(e);
                Log.e("LogCacheActivity.Poster.doInBackgroundInternal", e);
            } finally {
                cLog.endLog();
            }

            return StatusCode.LOG_POST_ERROR;
        }

        @Override
        protected void onPostExecuteInternal(final StatusCode status) {
            if (status == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                // Prevent from saving log after it was sent successfully.
                finish(SaveMode.SKIP);
            } else if (status == StatusCode.LOG_SAVED) {
                showToast(res.getString(R.string.info_log_saved));
                finish(SaveMode.SKIP);
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
                            finish(SaveMode.FORCE);
                        });
            }
        }
    }

}
