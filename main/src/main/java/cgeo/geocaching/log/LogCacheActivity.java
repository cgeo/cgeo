package cgeo.geocaching.log;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.databinding.LogcacheActivityBinding;
import cgeo.geocaching.databinding.LogcacheTrackableItemBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.CacheVotingBar;
import cgeo.geocaching.ui.DateTimeEditor;
import cgeo.geocaching.ui.ImageListFragment;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.lang3.StringUtils;

public class LogCacheActivity extends AbstractLoggingActivity {

    private static final String SAVED_STATE_LOGENTRY = "cgeo.geocaching.saved_state_logentry";
    private static final int LOG_MAX_LENGTH = 4000;

    private enum SaveMode { NORMAL, FORCE, SKIP }

    private final Set<TrackableLog> trackables = new HashSet<>();
    protected LogcacheActivityBinding binding;

    protected ImageListFragment imageListFragment;

    private Geocache cache = null;
    private String geocode = null;
    private ILoggingManager loggingManager;

    private OfflineLogEntry lastSavedState = null;

    private final CacheVotingBar cacheVotingBar = new CacheVotingBar();
    private final TextSpinner<LogType> logType = new TextSpinner<>();
    private final DateTimeEditor date = new DateTimeEditor();

    private boolean readyToPost = false;
    private final TextSpinner<ReportProblemType> reportProblem = new TextSpinner<>();
    private final TextSpinner<LogTypeTrackable> trackableActionsChangeAll = new TextSpinner<>();

    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private final GeoDirHandler geoUpdate = new GeoDirHandler() {

        @Override
        public void updateGeoData(final GeoData geo) {
            // Do nothing explicit, listening to location updates is sufficient
        }
    };

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
        } else {
            logType.setValues(loggingManager.getPossibleLogTypes());
        }

        refreshGui();
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

        if (possibleReportProblemTypes.size() == 1) {
            binding.reportProblemBox.setVisibility(View.GONE);
        } else {
            binding.reportProblemBox.setVisibility(View.VISIBLE);
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
            tLog.setAction(LastTrackableAction.getNextAction(tLog.brand, tLog.trackCode));
        }
        return tLog;
    }

    private void updateTrackablesList() {
        final TrackableLog[] trackablesArray = getSortedTrackables().toArray(new TrackableLog[trackables.size()]);
        binding.inventory.setAdapter(new TrackableLogAdapter(this, R.layout.logcache_trackable_item, trackablesArray));
        ViewUtils.setListViewHeightBasedOnItems(binding.inventory);

        binding.inventoryBox.setVisibility(trackables.isEmpty() ? View.GONE : View.VISIBLE);
        binding.inventoryChangeall.setVisibility(trackables.size() > 1 ? View.VISIBLE : View.GONE);

        trackableActionsChangeAll.setTextDialogTitle(getString(R.string.log_tb_changeall) + " (" + trackables.size() + ")");
    }

    private ArrayList<TrackableLog> getSortedTrackables() {
        final TrackableComparator comparator = Settings.getTrackableComparator();
        final ArrayList<TrackableLog> sortedTrackables = new ArrayList<>(trackables);
        Collections.sort(sortedTrackables, comparator.getComparator());
        return sortedTrackables;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.logcache_activity);
        binding = LogcacheActivityBinding.bind(findViewById(R.id.logcache_viewroot));

        date.init(binding.date, null, null, getSupportFragmentManager());
        logType.setTextView(binding.type)
                .setDisplayMapper(lt -> TextParam.text(lt.getL10n()).setImage(ImageParam.id(lt.getLogOverlay())));
        reportProblem.setTextView(binding.reportProblem)
                .setTextDisplayMapperPure(rp -> rp.getL10n() + " ▼")
                .setDisplayMapperPure(ReportProblemType::getL10n);

        this.imageListFragment = (ImageListFragment) getSupportFragmentManager().findFragmentById(R.id.imagelist_fragment);

        //init trackable "change all" button
        trackableActionsChangeAll.setTextView(binding.changebutton)
                .setTextDialogTitle(getString(R.string.log_tb_changeall))
                .setTextDisplayMapperPure(lt -> getString(R.string.log_tb_changeall))
                .setDisplayMapperPure(LogTypeTrackable::getLabel)
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
        cacheVotingBar.initialize(cache, binding.getRoot(), null);

        setCacheTitleBar(cache);
        //initializeRatingBar();

        loggingManager = cache.getLoggingManager(this);
        loggingManager.init();

        this.imageListFragment.init(geocode, loggingManager.getMaxImageUploadSize(), loggingManager.isImageCaptionMandatory());

        // initialize with default values
        resetValues();
        logType.setChangeListener(lt -> refreshGui());

        // Restore previous state
        lastSavedState = restorePreviousLogEntry(savedInstanceState);
        if (lastSavedState == null) {
            //this means there is no previous entry
            lastSavedState = getEntryFromView();
            //set initial signature. Setting this AFTER setting lastSavedState and GUI leads to the signature being a change-to-save as requested in #8973
            if (StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature() && StringUtils.isBlank(currentLogText())) {
                insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, lastSavedState)), false);
            }
        } else {
            fillViewFromEntry(lastSavedState);
        }

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

    @Override
    public void onResume() {
        super.onResume();
        // Resume location access
        resumeDisposables.add(geoUpdate.start(GeoDirHandler.UPDATE_GEODATA));
    }

    @Override
    public void onPause() {
        resumeDisposables.clear();
        super.onPause();

    }

    private void setLogText(final String newText) {
        binding.log.setText(newText == null ? StringUtils.EMPTY : newText);
        Dialogs.moveCursorToEnd(binding.log);
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
        binding.favoriteCheck.setChecked(logEntry.favorite);
        //If fav check is set then ALWAYS make the checkbox visible. See https://github.com/cgeo/cgeo/issues/13309#issuecomment-1702026609
        if (logEntry.favorite) {
            binding.favoriteCheck.setVisibility(View.VISIBLE);
        }
        binding.logPassword.setText(logEntry.password);

        imageListFragment.setImages(logEntry.logImages);

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
                .setFavorite(binding.favoriteCheck.isChecked())
                .setPassword(binding.logPassword.getText().toString());
        CollectionStream.of(imageListFragment.getImages()).forEach(builder::addLogImage);
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
            binding.favoriteCheck.setText(res.getQuantityString(((ILoggingWithFavorites) loggingManager).getFavoriteCheckboxText(), favoritePoints, favoritePoints));
            if (favoritePoints > 0) {
                binding.favoriteCheck.setVisibility(View.VISIBLE);
            }
        } else {
            binding.favoriteCheck.setVisibility(View.GONE);
        }
    }

    private void resetValues() {
        setType(cache.getDefaultLogType());
        final Calendar defaultDate = Calendar.getInstance();
        // if this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && CalendarUtils.isPastEvent(cache) && logType.get() == LogType.ATTENDED) {
            defaultDate.setTime(cache.getHiddenDate());
        }
        date.setCalendar(defaultDate);
        setLogText(null);
        reportProblem.set(ReportProblemType.NO_PROBLEM);
        cacheVotingBar.setRating(cache.getMyVote());
        imageListFragment.clearImages();
        binding.favoriteCheck.setChecked(false);
        binding.logPassword.setText(StringUtils.EMPTY);

        /* still needed?
        final EditText logPasswordView = LogCacheActivity.this.findViewById(R.id.log_password);
        logPasswordView.setText(StringUtils.EMPTY);
        */

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
        if (lastSavedState != null && !StringUtils.isBlank(lastSavedState.log)) {
            Settings.setLastCacheLog(lastSavedState.log);
        }
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
        updateLogPasswordBox(logType.get());
        cacheVotingBar.validateVisibility(cache, logType.get());
        initializeFavoriteCheck();
        verifySelectedReportProblemType();
        updateTrackablesList();
    }

    private void showProgress(final boolean loading) {
        readyToPost = !loading;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateLogPasswordBox(final LogType type) {
        if (type == LogType.FOUND_IT && cache.isLogPasswordRequired()) {
            binding.logPasswordBox.setVisibility(View.VISIBLE);
        } else {
            binding.logPasswordBox.setVisibility(View.GONE);
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
                AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    try (ContextLogger ccLog = new ContextLogger("LogCacheActivity.saveLog.doInBackground(gc=%s)", cache.getGeocode())) {
                        cache.logOffline(LogCacheActivity.this, logEntry);
                        ccLog.add("log=%s", logEntry.log);
                        imageListFragment.adjustImagePersistentState();
                    }
                });
            }
        }
    }

    private String currentLogText() {
        return binding.log.getText().toString();
    }

    private String currentLogPassword() {
        return binding.logPassword.getText().toString();
    }

    @Override
    protected LogContext getLogContext() {
        return new LogContext(cache, getEntryFromView());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        imageListFragment.onParentActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.v("LogCacheActivity.onOptionsItemSelected(" + item.getItemId() + "/" + item.getTitle() + ")");
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_send) {
            final int logLength = TextUtils.getNormalizedStringLength(binding.log.getText().toString());
            if (logLength > 0) {
                if (logLength <= LOG_MAX_LENGTH) {
                    sendLogAndConfirm();
                } else {
                    Toast.makeText(this, R.string.cache_log_too_long, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, R.string.cache_empty_log, Toast.LENGTH_LONG).show();
            }
        } else if (itemId == R.id.save) {
            finish(SaveMode.FORCE);
        } else if (itemId == R.id.clear) {
            clearLog();
        } else if (itemId == R.id.menu_sort_trackables_name) {
            sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_NAME);
        } else if (itemId == R.id.menu_sort_trackables_code) {
            sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_TRACKCODE);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void sendLogAndConfirm() {
        if (!readyToPost) {
            SimpleDialog.of(this).setMessage(R.string.log_post_not_possible).show();
            return;
        }
        if (CalendarUtils.isFuture(date.getCalendar())) {
            SimpleDialog.of(this).setMessage(R.string.log_date_future_not_allowed).show();
            return;
        }
        if (logType.get().mustConfirmLog()) {
            SimpleDialog.of(this).setTitle(R.string.confirm_log_title).setMessage(R.string.confirm_log_message, logType.get().getL10n()).confirm(this::sendLogInternal);
        } else if (reportProblem.get() != ReportProblemType.NO_PROBLEM) {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.confirm_report_problem_title)).setMessage(TextParam.id(R.string.confirm_report_problem_message, reportProblem.get().getL10n())).confirm(this::sendLogInternal);
        } else {
            sendLogInternal();
        }
    }

    private void sendLogInternal() {
        lastSavedState = getEntryFromView();
        final LogCacheTaskInterface taskInterface = new LogCacheTaskInterface();
        taskInterface.loggingManager = loggingManager;
        taskInterface.geocache = cache;
        taskInterface.trackables = trackables;
        taskInterface.binding = binding;
        taskInterface.logType = logType.get();
        taskInterface.reportProblemType = reportProblem.get();
        taskInterface.imageListFragment = imageListFragment;
        taskInterface.cacheVotingBar = cacheVotingBar;
        taskInterface.date = date;
        new LogCacheTask(this, res, getString(R.string.log_saving), getString(imageListFragment.getImages().isEmpty() ? R.string.log_posting_log : R.string.log_saving_and_uploading), taskInterface, this::onPostExecuteInternal).execute(currentLogText(), currentLogPassword());
        Settings.setLastCacheLog(currentLogText());
        if (Settings.removeFromRouteOnLog()) {
            DataStore.removeFirstMatchingIdFromIndividualRoute(geocode);
        }
    }

    protected static class LogCacheTaskInterface {
        public ILoggingManager loggingManager;
        public Geocache geocache;
        public Set<TrackableLog> trackables;
        public LogcacheActivityBinding binding;
        public LogType logType;
        public ReportProblemType reportProblemType;
        public ImageListFragment imageListFragment;
        public CacheVotingBar cacheVotingBar;
        public DateTimeEditor date;
    }

    private void onPostExecuteInternal(final StatusResult statusResult) {
        GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
        if (statusResult.getStatusCode() == StatusCode.NO_ERROR) {
            //reset Gui and all values
            resetValues();
            refreshGui();
            lastSavedState = getEntryFromView();

            imageListFragment.clearImages();
            imageListFragment.adjustImagePersistentState();

            showToast(res.getString(R.string.info_log_posted));
            // Prevent from saving log after it was sent successfully.
            finish(LogCacheActivity.SaveMode.SKIP);
        } else if (statusResult.getStatusCode() == StatusCode.LOG_SAVED) {
            showToast(res.getString(R.string.info_log_saved));
            finish(LogCacheActivity.SaveMode.SKIP);
        } else if (!LogCacheActivity.this.isFinishing()) {
            SimpleDialog.of(LogCacheActivity.this)
                    .setTitle(R.string.info_log_post_failed)
                    .setMessage(TextParam.id(R.string.info_log_post_failed_reason, statusResult.getErrorString(res)).setMovement(true))
                    .setButtons(R.string.info_log_post_retry, 0, R.string.info_log_post_save)
                    .setNeutralAction(() -> finish(LogCacheActivity.SaveMode.FORCE))
                    .confirm(this::sendLogInternal);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.save).setVisible(true);
        menu.findItem(R.id.clear).setVisible(true);
        menu.findItem(R.id.menu_sort_trackables_by).setVisible(true);
        switch (Settings.getTrackableComparator()) {
            case TRACKABLE_COMPARATOR_TRACKCODE:
                menu.findItem(R.id.menu_sort_trackables_code).setChecked(true);
                break;
            case TRACKABLE_COMPARATOR_NAME:
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
        public LogcacheTrackableItemBinding binding;
        private final TextSpinner<LogTypeTrackable> trackableAction = new TextSpinner<>();

        public ViewHolder(final View rowView) {
            super(rowView);
            binding = LogcacheTrackableItemBinding.bind(rowView);
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
            fillViewHolder(holder, trackable, holder.binding.action);
            return rowView;
        }

        private void fillViewHolder(final ViewHolder holder, final TrackableLog trackable, final TextView action) {
            holder.binding.trackableImageBrand.setImageResource(trackable.brand.getIconResource());
            holder.binding.trackcode.setText(trackable.trackCode);
            holder.binding.name.setText(trackable.name);

            holder.trackableAction.setTextView(action)
                    .setTextDialogTitle(trackable.name)
                    .setTextDisplayMapperPure(lt -> lt.getLabel() + " ▼")
                    .setDisplayMapperPure(LogTypeTrackable::getLabel)
                    .setValues(LogTypeTrackable.getLogTypeTrackableForLogCache())
                    .set(trackable.action)
                    .setChangeListener(lt -> {
                        trackable.action = lt;
                        updateTrackablesList();
                    });


            holder.binding.info.setOnClickListener(view -> {
                final Intent trackablesIntent = new Intent(LogCacheActivity.this, TrackableActivity.class);
                final String tbCode = StringUtils.isNotEmpty(trackable.geocode) ? trackable.geocode : trackable.trackCode;
                trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
                trackablesIntent.putExtra(Intents.EXTRA_BRAND, trackable.brand.getId());
                trackablesIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.trackCode);
                startActivity(trackablesIntent);
            });
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
            cache.clearOfflineLog(this.getContext());
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

}
