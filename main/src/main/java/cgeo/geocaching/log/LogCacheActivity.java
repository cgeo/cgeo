package cgeo.geocaching.log;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.databinding.LogcacheActivityBinding;
import cgeo.geocaching.databinding.LogcacheTrackableItemBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
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
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.lang3.StringUtils;

public class LogCacheActivity extends AbstractLoggingActivity implements LoaderManager.LoaderCallbacks<LogContextInfo> {

    private static final int LOADER_ID_LOGGING_INFO = 409809;

    private static final String SAVED_STATE_LOGEDITMODE = "cgeo.geocaching.saved_state_logeditmode";
    private static final String SAVED_STATE_OLDLOGENTRY = "cgeo.geocaching.saved_state_oldlogentry";
    private static final String SAVED_STATE_LOGENTRY = "cgeo.geocaching.saved_state_logentry";
    private static final String SAVED_STATE_AVAILABLE_FAV_POINTS  = "cgeo.geocaching.saved_state_available_fav_points";

    private enum LogEditMode {
        CREATE_NEW, // create/edit a new log entry (which may be stored offline)
        EDIT_EXISTING //edit an existing online log entry (as of now not storable offline)
    }

    private enum SaveMode { NORMAL, FORCE, SKIP }

    protected LogcacheActivityBinding binding;

    protected ImageListFragment imageListFragment;

    private final LogActivityHelper logActivityHelper = new LogActivityHelper(this)
        .setLogResultConsumer((type, result) -> onPostExecuteInternal(result));

    private InventoryLogAdapter inventoryAdapter;


    private Geocache cache = null;
    private String geocode = null;
    private ILoggingManager loggingManager;

    private OfflineLogEntry lastSavedState = null;

    private final CacheVotingBar cacheVotingBar = new CacheVotingBar();
    private final TextSpinner<LogType> logType = new TextSpinner<>();
    private final DateTimeEditor date = new DateTimeEditor();
    private int availableFavoritePoints = -1;

    private LogEditMode logEditMode = LogEditMode.CREATE_NEW;
    private LogEntry originalLogEntry = null;

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

    private static void startActivityInternal(final Context context, final String geocode, final LogEntry entryToEdit) {
        final Intent logVisitIntent = new Intent(context, LogCacheActivity.class);
        logVisitIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        if (entryToEdit != null) {
            logVisitIntent.putExtra(Intents.EXTRA_LOGENTRY, entryToEdit);
        }
        context.startActivity(logVisitIntent);
    }

    public static void startForCreate(@NonNull final Context fromActivity, final String geocode) {
        startActivityInternal(fromActivity, geocode, null);
    }

    public static void startForEdit(@NonNull final Context fromActivity, final String geocode, final LogEntry logEntry) {
        startActivityInternal(fromActivity, geocode, logEntry);
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

        if (logEditMode == LogEditMode.CREATE_NEW && possibleReportProblemTypes.size() > 1) {
            binding.reportProblemBox.setVisibility(View.VISIBLE);
        } else {
            binding.reportProblemBox.setVisibility(View.GONE);
        }
    }

    private void showErrorLoadingData(final String additionalData) {
        String message = LocalizationUtils.getString(R.string.warn_log_load_additional_data);
        if (additionalData != null) {
            message += ": " + additionalData;
        }
        showToast(message);
        showProgress(false);
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

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        logEditMode = LogEditMode.CREATE_NEW;
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            this.originalLogEntry = extras.getParcelable(Intents.EXTRA_LOGENTRY);
            if (this.originalLogEntry != null) {
                logEditMode = LogEditMode.EDIT_EXISTING;
            }
        }

        //handle inventory
        trackableActionsChangeAll.setTextView(binding.changebutton);
        inventoryAdapter = new InventoryLogAdapter(this, binding.inventory, trackableActionsChangeAll,
            binding.inventoryChangeall, binding.inventoryBox, logEditMode == LogEditMode.CREATE_NEW);
        binding.inventory.setAdapter(inventoryAdapter);

        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        invalidateOptionsMenuCompatible();
        setLogTypeValues(cache.getPossibleLogTypes());
        cacheVotingBar.initialize(cache, binding.getRoot(), null);

        setCacheTitleBar(cache);

        loggingManager = cache.getLoggingManager();
        LoaderManager.getInstance(this).initLoader(LOADER_ID_LOGGING_INFO, null, this);

        //convert log text if necessary
        if (this.originalLogEntry != null) {
            this.originalLogEntry = this.originalLogEntry.buildUpon().setLog(loggingManager.convertLogTextToEditableText(originalLogEntry.log)).build();
        }

        this.imageListFragment.init(geocode, loggingManager.getMaxImageUploadSize());

        // initialize with default values
        resetValues();
        logType.setChangeListener(lt -> refreshGui());

        if (this.originalLogEntry != null) {
            fillViewFromEntry(this.originalLogEntry);
        }

        // Restore previous state
        lastSavedState = restorePreviousLogEntry(savedInstanceState);
        if (lastSavedState == null) {
            //this means this is the initial creation of the activity
            lastSavedState = getEntryFromView();
            //set initial signature. Setting this AFTER setting lastSavedState and GUI leads to the signature being a change-to-save as requested in #8973
            if (StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature() && StringUtils.isBlank(currentLogText())) {
                insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, lastSavedState)), false);
            }
        } else {
            fillViewFromEntry(lastSavedState);
        }
        if (savedInstanceState != null) {
            this.availableFavoritePoints = savedInstanceState.getInt(SAVED_STATE_AVAILABLE_FAV_POINTS);
            this.logEditMode = LogEditMode.values()[savedInstanceState.getInt(SAVED_STATE_LOGEDITMODE)];
            this.originalLogEntry = savedInstanceState.getParcelable(SAVED_STATE_OLDLOGENTRY);
        }
        inventoryAdapter.putActions(lastSavedState.inventoryActions);

        refreshGui();

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
        if (entry == null && this.logEditMode == LogEditMode.CREATE_NEW) {
            entry = DataStore.loadLogOffline(geocode);
        }

        return entry;
    }

    private void fillViewFromEntry(final LogEntry logEntry) {
        logType.set(logEntry.logType);
        date.setDate(logEntry.getDate());
        setLogText(logEntry.log);
        reportProblem.set(logEntry.reportProblem);
        imageListFragment.setImages(logEntry.logImages);

        if (logEntry instanceof OfflineLogEntry) {
            final OfflineLogEntry offlineLogEntry = (OfflineLogEntry) logEntry;
            cacheVotingBar.setRating(offlineLogEntry.rating == 0 ? cache.getMyVote() : offlineLogEntry.rating);
            binding.favoriteCheck.setChecked(offlineLogEntry.favorite);
            //If fav check is set then ALWAYS make the checkbox visible. See https://github.com/cgeo/cgeo/issues/13309#issuecomment-1702026609
            if (offlineLogEntry.favorite) {
                binding.favoriteCheck.setVisibility(View.VISIBLE);
            }
            binding.logPassword.setText(offlineLogEntry.password);
            inventoryAdapter.putActions(offlineLogEntry.inventoryActions);
            //CollectionStream.of(inventory).forEach(t -> initializeTrackableAction(t, offlineLogEntry));
        } else {
            cacheVotingBar.setRating(0);
            binding.favoriteCheck.setChecked(false);
            binding.logPassword.setText("");
        }

        //updateTrackablesList();
    }

    private OfflineLogEntry getEntryFromView() {
        final OfflineLogEntry.Builder builder = new OfflineLogEntry.Builder()
                .setLogType(logType.get())
                .setDate(date.getDate().getTime())
                .setLog(currentLogText())
                .setReportProblem(reportProblem.get())
                .setRating(cacheVotingBar.getRating())
                .setFavorite(binding.favoriteCheck.isChecked())
                .setPassword(ViewUtils.getEditableText(binding.logPassword.getText()));
        CollectionStream.of(imageListFragment.getImages()).forEach(builder::addLogImage);
        builder.addInventoryActions(inventoryAdapter.getActionLogs());
        //CollectionStream.of(inventory).forEach(t -> builder.addTrackableAction(t.geocode, t.action));

        return builder.build();
    }

    /**
     * Checks whether there are favorite points available and sets the corresponding visibility of
     * "add to favorite" checkbox.
     */
    private void initializeFavoriteCheck() {
        final IConnector connector = ConnectorFactory.getConnector(cache);

        if ((connector instanceof IFavoriteCapability) && ((IFavoriteCapability) connector).supportsAddToFavorite(cache, logType.get()) && loggingManager.supportsLogWithFavorite()) {
            final int remainingPoints = availableFavoritePoints + (cache.isFavorite() ? 1 : 0);
            binding.favoriteCheck.setText(res.getQuantityString(loggingManager.getFavoriteCheckboxText(), remainingPoints, remainingPoints));
            if (availableFavoritePoints > 0 || (this.logEditMode == LogEditMode.EDIT_EXISTING && cache.isFavorite())) {
                binding.favoriteCheck.setVisibility(availableFavoritePoints > 0 ? View.VISIBLE : View.GONE);
                binding.favoriteCheck.setChecked(cache.isFavorite());
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

        inventoryAdapter.resetActions();
        //force copy due to #9101
        //CollectionStream.of(inventory, true).forEach(tl -> initializeTrackableAction(tl, null));
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
        logActivityHelper.finish();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        checkFinish(super::onBackPressed);
    }

    public void checkFinish(final Runnable runnable) {
        if (logEditMode == LogEditMode.EDIT_EXISTING && getEntryFromView().hasSaveRelevantChanges(originalLogEntry)) {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.confirm_unsent_changes_title))
                .setMessage(TextParam.id(R.string.confirm_unsent_changes))
                .confirm(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public void onStop() {
        saveLog();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_STATE_LOGEDITMODE, this.logEditMode.ordinal());
        outState.putParcelable(SAVED_STATE_OLDLOGENTRY, this.originalLogEntry);
        outState.putParcelable(SAVED_STATE_LOGENTRY, getEntryFromView());
        outState.putInt(SAVED_STATE_AVAILABLE_FAV_POINTS, availableFavoritePoints);
    }

    public void setType(final LogType type) {
        logType.set(type);
        refreshGui();
    }

    public void refreshGui() {
        updateLogPasswordBox(logType.get());
        cacheVotingBar.validateVisibility(cache, logType.get(), this.logEditMode == LogEditMode.CREATE_NEW);
        initializeFavoriteCheck();
        verifySelectedReportProblemType();
        //updateTrackablesList();
    }

    private void showProgress(final boolean loading) {
        readyToPost = !loading;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateLogPasswordBox(final LogType type) {
        if (logEditMode == LogEditMode.CREATE_NEW && type == LogType.FOUND_IT && cache.isLogPasswordRequired()) {
            binding.logPasswordBox.setVisibility(View.VISIBLE);
        } else {
            binding.logPasswordBox.setVisibility(View.GONE);
        }
    }

    private void saveLog() {
        saveLog(SaveMode.NORMAL);
    }

    private void saveLog(final SaveMode saveMode) {

        if (logEditMode != LogEditMode.CREATE_NEW) {
            return;
        }

        try (ContextLogger cLog = new ContextLogger("LogCacheActivity.saveLog(mode=%s)", saveMode)) {

            final OfflineLogEntry logEntry = getEntryFromView();

            final boolean logChanged = logEntry.hasSaveRelevantChanges(lastSavedState);
            final boolean doSave = SaveMode.FORCE.equals(saveMode) || (!SaveMode.SKIP.equals(saveMode) && logChanged);
            cLog.add("logChanged=%b, doSave=%b", logChanged, doSave);

            if (doSave) {
                lastSavedState = logEntry;
                AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    try (ContextLogger ccLog = new ContextLogger("LogCacheActivity.saveLog.doInBackground(gc=%s)", cache.getGeocode())) {
                        cache.storeLogOffline(LogCacheActivity.this, logEntry);
                        ccLog.add("log=%s", logEntry.log);
                        imageListFragment.adjustImagePersistentState();
                        inventoryAdapter.saveActions();
                    }
                });
            }
        }
    }

    private String currentLogText() {
        return ViewUtils.getEditableText(binding.log.getText());
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
            final int logLength = ViewUtils.getEditableText(binding.log.getText()).trim().length();
            if (logLength > 0) {
                sendLogAndConfirm();
            } else {
                ViewUtils.showToast(this, R.string.cache_empty_log);
            }
        } else if (itemId == R.id.save) {
            finish(SaveMode.FORCE);
        } else if (itemId == R.id.clear) {
            clearLog();
        } else if (itemId == R.id.menu_sort_trackables_name) {
            sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_NAME);
        } else if (itemId == R.id.menu_sort_trackables_code) {
            sortTrackables(TrackableComparator.TRACKABLE_COMPARATOR_TRACKCODE);
        } else if (itemId == android.R.id.home) {
            //back arrow
            checkFinish(() -> ActivityMixin.navigateUp(this));
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
        if (logEditMode == LogEditMode.EDIT_EXISTING) {
            logActivityHelper.editLog(cache, this.originalLogEntry,
                getEntryFromView().buildUponOfflineLogEntry().setServiceLogId(this.originalLogEntry.serviceLogId).build());
        } else {
            logActivityHelper.createLog(cache, getEntryFromView(), inventoryAdapter.getInventory());
        }
    }

    private void onPostExecuteInternal(final StatusResult statusResult) {
        GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
        if (statusResult.isOk()) {

            final String currentLogText = currentLogText();
            if (!StringUtils.isBlank(currentLogText)) {
                Settings.setLastCacheLog(currentLogText);
            }

            //reset Gui and all values
            resetValues();
            refreshGui();
            lastSavedState = getEntryFromView();

            imageListFragment.clearImages();
            imageListFragment.adjustImagePersistentState();

            showToast(res.getString(R.string.info_log_posted));
            // Prevent from saving log after it was sent successfully.
            finish(LogCacheActivity.SaveMode.SKIP);
        } else if (!LogCacheActivity.this.isFinishing()) {
            SimpleDialog.of(LogCacheActivity.this)
                    .setTitle(R.string.info_log_post_failed)
                    .setMessage(TextParam.id(R.string.info_log_post_failed_simple_reason))
                    .setButtons(R.string.info_log_post_retry, R.string.cancel, logEditMode == LogEditMode.CREATE_NEW ? R.string.info_log_post_save : 0)
                    .setNeutralAction(() -> finish(LogCacheActivity.SaveMode.FORCE))
                    .confirm(this::sendLogInternal);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.save).setVisible(logEditMode == LogEditMode.CREATE_NEW);
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
        //Settings.setTrackableComparator(comparator);
        inventoryAdapter.resortTrackables(comparator);
        //updateTrackablesList();
        invalidateOptionsMenuCompatible();
    }

    @NonNull
    @Override
    public Loader<LogContextInfo> onCreateLoader(final int id, @Nullable final Bundle args) {
        Log.i("LogCacheActivity.onLoadStarted()");
        showProgress(true);
        return new LogContextInfoLoader(this, loggingManager, null);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<LogContextInfo> loader, final LogContextInfo data) {
        Log.i("LogCacheActivity.onLoadFinished()");
        if (data.hasLoadError()) {
            showErrorLoadingData(data.getUserDisplayableErrorMessage());
        }
        if (!data.getAvailableLogTypes().isEmpty()) {
            setLogTypeValues(data.getAvailableLogTypes());
        }
        if (!data.getAvailableTrackables().isEmpty()) {
            inventoryAdapter.setTrackables(data.getAvailableTrackables());
        }
        if (!data.getAvailableReportProblemTypes().isEmpty()) {
            final List<ReportProblemType> list = data.getAvailableReportProblemTypes();
            reportProblem.setValues(list);

        }
        if (data.getAvailableFavoritePoints() >= 0) {
            this.availableFavoritePoints = data.getAvailableFavoritePoints();
        }


        refreshGui();
        showProgress(false);
    }

    private void setLogTypeValues(final Collection<LogType> logTypes) {
        if (this.originalLogEntry != null && !logTypes.contains(this.originalLogEntry.logType)) {
            //log type of original entry must ALWAYS be available for selection
            final List<LogType> rLogTypes = new ArrayList<>();
            rLogTypes.add(this.originalLogEntry.logType);
            rLogTypes.addAll(logTypes);
            this.logType.setValues(rLogTypes);
        } else {
            this.logType.setValues(logTypes);
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<LogContextInfo> loader) {
        //do nothing
    }

    private static class LogContextInfoLoader extends AsyncTaskLoader<LogContextInfo> {

        private final ILoggingManager loggingManager;
        private final String serviceLogId;

        LogContextInfoLoader(@NonNull final Context context, final ILoggingManager loggingManager, final String serviceLogId) {
            super(context);
            this.loggingManager = loggingManager;
            this.serviceLogId = serviceLogId;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Nullable
        @Override
        public LogContextInfo loadInBackground() {
            final LogContextInfo info = this.loggingManager.getLogContextInfo(serviceLogId);

            // Load Generic Trackables
            for (TrackableConnector genericTrackableConnector : ConnectorFactory.getGenericTrackablesConnectors()) {
                final List<Trackable> trackables = genericTrackableConnector.loadInventory();
                info.addAvailableTrackables(trackables);
            }
            return info;
        }
    }

    protected static class ViewHolder extends AbstractViewHolder {
        public LogcacheTrackableItemBinding binding;
        private final TextSpinner<LogTypeTrackable> trackableAction = new TextSpinner<>();

        public ViewHolder(final View rowView) {
            super(rowView);
            binding = LogcacheTrackableItemBinding.bind(rowView);
        }
    }

    private static final class InventoryLogAdapter extends ArrayAdapter<Trackable> {

        private final Map<String, LogTypeTrackable> actionLogs = new HashMap<>();
        private final TextSpinner<LogTypeTrackable> changeAllButton;
        private final ListView listView;
        private final View changeAllBox;
        private final View inventoryBox;

        private final boolean activated;

        private InventoryLogAdapter(final Context context, final ListView listView, final TextSpinner<LogTypeTrackable> changeAllButton, final View changeAllBox, final View inventoryBox, final boolean activated) {
            super(context, R.layout.logcache_trackable_item);
            this.setNotifyOnChange(false);
            this.listView = listView;
            this.changeAllButton = changeAllButton;
            this.changeAllBox = changeAllBox;
            this.inventoryBox = inventoryBox;
            this.activated = activated;

            //init trackable "change all" button
            changeAllButton.setTextDialogTitle(LocalizationUtils.getString(R.string.log_tb_changeall))
                .setTextDisplayMapperPure(lt -> LocalizationUtils.getString(R.string.log_tb_changeall))
                .setDisplayMapperPure(LogTypeTrackable::getLabel)
                .setValues(LogTypeTrackable.getLogTypesAllowedForInventory())
                .set(Settings.isTrackableAutoVisit() ? LogTypeTrackable.VISITED : LogTypeTrackable.DO_NOTHING)
                .setChangeListener(lt -> {
                    final Map<String, LogTypeTrackable> newMap = new HashMap<>();
                    for (int i = 0; i < getCount(); i++) {
                        newMap.put(getItem(i).getGeocode(), lt);
                    }
                    putActions(newMap);
                }, false);
        }

        public void setTrackables(final Collection<Trackable> inventory) {
            this.clear();
            this.addAll(inventory);
            resortTrackables(Settings.getTrackableComparator());
        }

        public void resortTrackables(final TrackableComparator comparator) {

            //sort trackables
            this.sort(comparator.getComparator());
            Settings.setTrackableComparator(comparator);

            handleChangedData();
        }

        public void resetActions() {
            this.actionLogs.clear();
            handleChangedData();
        }

        public void saveActions() {
            for (Map.Entry<String, LogTypeTrackable> entry : actionLogs.entrySet()) {
                LastTrackableAction.setAction(entry.getKey(), entry.getValue());
            }
        }

        @NonNull
        private LogTypeTrackable checkAndGetAction(final String key, @Nullable final LogTypeTrackable candidate) {
            if (candidate != null && candidate.allowedForInventory) {
                actionLogs.put(key, candidate);
                return candidate;
            }

            final LogTypeTrackable result = actionLogs.get(key);
            if (result != null) {
                return result;
            }

            LogTypeTrackable newAction = LastTrackableAction.getLastAction(key);
            if (newAction == null || !newAction.allowedForInventory) {
                newAction = Settings.isTrackableAutoVisit() ? LogTypeTrackable.VISITED : LogTypeTrackable.DO_NOTHING;
            }
            actionLogs.put(key, newAction);
            return newAction;

        }

        public void putActions(final Map<String, LogTypeTrackable> actions) {
            for (Map.Entry<String, LogTypeTrackable> entry : actions.entrySet()) {
                checkAndGetAction(entry.getKey(), entry.getValue());
            }
            handleChangedData();
        }

        public Map<String, LogTypeTrackable> getActionLogs() {
            return actionLogs;
        }

        public Map<String, Trackable> getInventory() {
            final Map<String, Trackable> inventory = new HashMap<>();
            for (int i = 0; i < getCount(); i++) {
                inventory.put(getItem(i).getGeocode(), getItem(i));
            }
            return inventory;
        }

        private void handleChangedData() {
            //re-render list
            notifyDataSetChanged();
            ViewUtils.setListViewHeightBasedOnItems(listView);

            //handle "change all" button
            inventoryBox.setVisibility(activated && getCount() > 0 ? View.VISIBLE : View.GONE);
            changeAllBox.setVisibility(activated && getCount() > 1 ? View.VISIBLE : View.GONE);
            changeAllButton.setTextDialogTitle(
                LocalizationUtils.getString(R.string.log_tb_changeall) + " (" + getCount() + ")");

        }

        @Override
        @NonNull
        public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = LayoutInflater.from(getContext()).inflate(R.layout.logcache_trackable_item, parent, false);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();
            if (holder == null) {
                holder = new ViewHolder(rowView);
            }

            final Trackable trackable = getItem(position);
            fillViewHolder(holder, trackable, holder.binding.action);
            return rowView;
        }

        private void fillViewHolder(final ViewHolder holder, final Trackable trackable, final TextView actionView) {
            holder.binding.trackableImageBrand.setImageResource(trackable.getBrand().getIconResource());
            holder.binding.trackcode.setText(trackable.getTrackingcode());
            holder.binding.name.setText(trackable.getName());

            final LogTypeTrackable action = checkAndGetAction(trackable.getGeocode(), null);

            holder.trackableAction.setTextView(actionView)
                    .setTextDialogTitle(trackable.getName())
                    .setTextDisplayMapperPure(lt -> lt.getLabel() + " ▼")
                    .setDisplayMapperPure(LogTypeTrackable::getLabel)
                    .setValues(LogTypeTrackable.getLogTypesAllowedForInventory())
                    .set(action)
                    .setChangeListener(lt -> actionLogs.put(trackable.getGeocode(), lt));


            holder.binding.info.setOnClickListener(view -> {
                final Intent trackablesIntent = new Intent(getContext(), TrackableActivity.class);
                final String tbCode = StringUtils.isNotEmpty(trackable.getGeocode()) ? trackable.getGeocode() : trackable.getTrackingcode();
                trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, tbCode);
                trackablesIntent.putExtra(Intents.EXTRA_BRAND, trackable.getBrand().getId());
                trackablesIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.getTrackingcode());
                getContext().startActivity(trackablesIntent);
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
            cache.storeLogOffline(getContext(), previousState);
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
