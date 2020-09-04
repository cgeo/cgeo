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
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.VotingBarUtil;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.DateTimeEditor;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ViewUtils;

import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.lang3.StringUtils;

public class LogCacheActivity extends AbstractLoggingActivity {

    private static final String SAVED_STATE_RATING = "cgeo.geocaching.saved_state_rating";
    private static final String SAVED_STATE_TYPE = "cgeo.geocaching.saved_state_type";
    private static final String SAVED_STATE_DATE = "cgeo.geocaching.saved_state_date";
    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_FAVPOINTS = "cgeo.geocaching.saved_state_favpoints";
    private static final String SAVED_STATE_PROBLEM = "cgeo.geocaching.saved_state_problem";
    private static final String SAVED_STATE_TRACKABLES = "cgeo.geocaching.saved_state_trackables";

    private static final int SELECT_IMAGE = 101;
    private final Set<TrackableLog> trackables = new HashSet<>();
    @BindView(R.id.tweet)
    protected CheckBox tweetCheck;
    @BindView(R.id.log_password_box)
    protected LinearLayout logPasswordBox;
    @BindView(R.id.favorite_check)
    protected CheckBox favCheck;
    @BindView(R.id.gcvoteRating)
    protected RatingBar votingBar;
    @BindView(R.id.log)
    protected EditText logEditText;
    private Geocache cache = null;
    private String geocode = null;
    private String text = null;
    private ILoggingManager loggingManager;

    // Data to be saved while reconfiguring
    private float rating;
    private final TextSpinner<LogType> logType = new TextSpinner<>();
    private final DateTimeEditor date = new DateTimeEditor();
    private Image image;
    private boolean sendButtonEnabled;
    private final TextSpinner<ReportProblemType> reportProblem = new TextSpinner<>();
    private LogEntry oldLog;
    private Bundle trackableState;
    private final TextSpinner<LogTypeTrackable> trackableActionsChangeAll = new TextSpinner<>();

    private SaveMode saveMode = SaveMode.SMART;

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

        if (loggingManager.getPossibleLogTypes().isEmpty()) {
            showErrorLoadingData();
            return;
        }

       verifySelectedReportProblemType();

        initializeRatingBar();

        enablePostButton(true);

        initializeTrackablesAction();
        updateTrackablesList();
        initializeFavoriteCheck();

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
        logType.setTextView(findViewById(R.id.type)).setDisplayMapper(lt -> lt.getL10n());
        reportProblem.setTextView(findViewById(R.id.report_problem))
                .setTextDisplayMapper(rp -> rp.getL10n() + " ▼")
                .setDisplayMapper(rp -> rp.getL10n());

        //init trackable "change all" button
        trackableActionsChangeAll.setTextView(findViewById(R.id.changebutton))
                .setTextDialogTitle(getString(R.string.log_tb_changeall))
                .setTextDisplayMapper(lt -> getString(R.string.log_tb_changeall))
                .setDisplayMapper(lt -> lt.getLabel())
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

        setCacheTitleBar(cache);

        initializeRatingBar();

        loggingManager = cache.getLoggingManager(this);
        loggingManager.init();

        // initialize with default values
        setDefaultValues();
        logType.setChangeListener(lt -> adjustViewToLogType());

        // Restore previous state
        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(SAVED_STATE_RATING);
            logType.set(LogType.getById(savedInstanceState.getInt(SAVED_STATE_TYPE)));
            date.setDate(new Date(savedInstanceState.getLong(SAVED_STATE_DATE)));
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE);
            reportProblem.set(ReportProblemType.findByCode(savedInstanceState.getString(SAVED_STATE_PROBLEM)));
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

        if (StringUtils.isBlank(currentLogText()) && StringUtils.isNotBlank(text)) {
            setLogText();
        }

        tweetCheck.setChecked(true);
        updateTweetBox(logType.get());
        updateLogPasswordBox(logType.get());

        // Load Generic Trackables
        AndroidRxUtils.bindActivity(this,
                // Obtain the actives connectors
                Observable.fromIterable(ConnectorFactory.getLoggableGenericTrackablesConnectors())
                        .flatMap((Function<TrackableConnector, Observable<TrackableLog>>) trackableConnector -> Observable.defer(trackableConnector::trackableLogInventory).subscribeOn(AndroidRxUtils.networkScheduler)).toList()
        ).subscribe(trackableLogs -> {
            // Store trackables
            trackables.addAll(trackableLogs);
            // Update the UI
            initializeTrackablesAction();
            updateTrackablesList();
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
            logType.set(oldLog.getType());
            date.setDate(new Date(oldLog.date));
            text = oldLog.log;
            reportProblem.set(oldLog.reportProblem);
        } else if (StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature() && StringUtils.isBlank(currentLogText())) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(cache, null)), false);
        }
    }

    private void initializeRatingBar() {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (connector instanceof IVotingCapability && ((IVotingCapability) connector).canVote(cache, logType.get())) {
            VotingBarUtil.initializeRatingBar(cache, getWindow().getDecorView().getRootView(), stars -> rating = stars);
        } else {
            votingBar.setVisibility(View.GONE);
            getWindow().getDecorView().getRootView().findViewById(R.id.voteLabel).setVisibility(View.GONE);
        }
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

    private void setDefaultValues() {
        rating = GCVote.NO_RATING;
        setType(cache.getDefaultLogType());

        final Calendar defaultDate = Calendar.getInstance();
        // it this is an attended event log, use the event date by default instead of the current date
        if (cache.isEventCache() && CalendarUtils.isPastEvent(cache) && logType.get() == LogType.ATTENDED) {
            defaultDate.setTime(cache.getHiddenDate());
        }
        date.setCalendar(defaultDate);

        text = null;
        image = Image.NONE;

        logEditText.setText(StringUtils.EMPTY);
        reportProblem.set(ReportProblemType.NO_PROBLEM);
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
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(SAVED_STATE_RATING, rating);
        outState.putInt(SAVED_STATE_TYPE, logType.get().id);
        outState.putLong(SAVED_STATE_DATE, date.getDate().getTime());
        outState.putParcelable(SAVED_STATE_IMAGE, image);
        outState.putString(SAVED_STATE_PROBLEM, reportProblem.get().code);
        // save state of trackables
        final Bundle outTrackables = new Bundle();
        for (final TrackableLog trackable : trackables) {
            outTrackables.putInt(trackable.trackCode, trackable.action.id);
        }
        outState.putBundle(SAVED_STATE_TRACKABLES, outTrackables);
    }

    public void setType(final LogType type) {
        logType.set(type);
        adjustViewToLogType();
    }

    private void adjustViewToLogType() {
        updateTweetBox(logType.get());
        updateLogPasswordBox(logType.get());
        initializeRatingBar();
        initializeFavoriteCheck();
        verifySelectedReportProblemType();
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
                || (oldLog != null && (oldLog.getType() != logType.get() || oldLog.reportProblem != reportProblem.get() || oldLog.date != date.getDate().getTime()))
                || (oldLog == null && reportProblem.get() != ReportProblemType.NO_PROBLEM)
        ) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    cache.logOffline(LogCacheActivity.this, log, date.getCalendar(), logType.get(), reportProblem.get());
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
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
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

    private enum SaveMode {
        /**
         * save when relevant changes are detected
         */
        SMART,
        /**
         * explicitly save, via menu or saveLog button
         */
        FORCE,
        /**
         * when log was posted and offline log cleared
         */
        SKIP
    }

    protected static class ViewHolder extends AbstractViewHolder {
        @BindView(R.id.trackable_image_brand)
        protected ImageView brandView;
        @BindView(R.id.trackcode)
        protected TextView codeView;
        @BindView(R.id.name)
        protected TextView nameView;
        //@BindView(R.id.action)
        //protected TextView actionButton;
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
                    .setDisplayMapper(lt -> lt.getLabel())
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
            oldType = logType.get();
            oldDate = date.getCalendar();
            oldReportProblem = reportProblem.get();
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
            setType(oldType);
            date.setCalendar(oldDate);
            oldLog = oldOldLog;

            reportProblem.set(oldReportProblem);
            setLogText();
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
                        if (votingConnector.supportsVoting(cache) && votingConnector.isValidRating(rating)) {
                            publishProgress(res.getString(R.string.log_posting_vote));
                            if (votingConnector.postVote(cache, rating)) {
                                cache.setMyVote(rating);
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

}
