// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.log

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.connector.trackable.TrackableConnector
import cgeo.geocaching.connector.trackable.TrackableLoggingManager
import cgeo.geocaching.databinding.LogtrackableActivityBinding
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogTemplateProvider.LogContext
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.search.GeocacheAutoCompleteAdapter
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.DateTimeEditor
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.dialog.CoordinateInputDialog
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.R.string
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.CheckBox

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader

import java.util.List

import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils

class LogTrackableActivity : AbstractLoggingActivity() : LoaderManager.LoaderCallbacks<List<LogTypeTrackable>> {

    private static val LOADER_ID_LOGGING_INFO: Int = 409842

    private static final LogTypeTrackable[] PREFERRED_DEFAULTS = LogTypeTrackable[] { LogTypeTrackable.DISCOVERED_IT, LogTypeTrackable.NOTE, LogTypeTrackable.RETRIEVED_IT }
    private LogtrackableActivityBinding binding

    private val createDisposables: CompositeDisposable = CompositeDisposable()

    private val logType: TextSpinner<LogTypeTrackable> = TextSpinner<>()
    private var geocode: String = null
    private Geopoint geopoint
    private var geocache: Geocache = Geocache()
    /**
     * As Long as we still fetch the current state of the trackable from the Internet, the user cannot yet send a log.
     */
    private var readyToPost: Boolean = true
    private val date: DateTimeEditor = DateTimeEditor()

    private Trackable trackable
    private TrackableBrand brand
    String trackingCode

    TrackableConnector connector
    private TrackableLoggingManager loggingManager

    private val logActivityHelper: LogActivityHelper = LogActivityHelper(this)
        .setLogResultConsumer((type, result) -> onPostExecuteInternal(result))

    /**
     * How many times the warning popup for geocode not set should be displayed
     */
    public static val MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE: Int = 3

    public static val LOG_TRACKABLE: Int = 1

    override     public Loader<List<LogTypeTrackable>> onCreateLoader(final Int id, final Bundle args) {
        Log.i("LogTrackableActivity.onLoadStarted()")
        showProgress(true)
        return LogTrackableActivity.LogContextInfoLoader(this, loggingManager)
    }

    override     public Unit onLoadFinished(final Loader<List<LogTypeTrackable>> loader, final List<LogTypeTrackable> logTypesTrackable) {
        Log.i("LogTrackableActivity.onLoadFinished()")

        if (CollectionUtils.isNotEmpty(logTypesTrackable)) {
            logType.setValues(logTypesTrackable)
            setLastUsedLogType()
        }

        showProgress(false)
    }

    private Unit setLastUsedLogType() {
        val lastUsedAction: LogTypeTrackable = LogTypeTrackable.getById(Settings.getTrackableAction())
        if (logType.getValues().contains(lastUsedAction)) {
            logType.set(lastUsedAction)
        } else {
            //last used action is not possible -> select the most preferred default
            for (LogTypeTrackable candidate : PREFERRED_DEFAULTS) {
                if (logType.getValues().contains(candidate)) {
                    logType.set(candidate)
                    updateForNewType()
                    break
                }
            }
            showToast(res.getString(R.string.info_log_type_changed))
        }
    }

    override     public Unit onLoaderReset(final Loader<List<LogTypeTrackable>> listLoader) {
        // nothing
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.logtrackable_activity)
        binding = LogtrackableActivityBinding.bind(findViewById(R.id.activity_content))

        date.init(findViewById(R.id.date), findViewById(R.id.time), null, getSupportFragmentManager())

        logType.setTextView(binding.type).setDisplayMapper(tt -> TextParam.text(tt.getLabel()))
        logType.setChangeListener(lt -> updateForNewType())
        logType.setValues(Trackable.getPossibleLogTypes())
        setLastUsedLogType()

        // get parameters
        val extras: Bundle = getIntent().getExtras()

        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE)

            // Load geocache if we can
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_GEOCACHE))) {
                val tmpGeocache: Geocache = DataStore.loadCache(extras.getString(Intents.EXTRA_GEOCACHE), LoadFlags.LOAD_CACHE_OR_DB)
                if (tmpGeocache != null) {
                    geocache = tmpGeocache
                }
            }
            // Load Tracking Code
            if (StringUtils.isNotBlank(extras.getString(Intents.EXTRA_TRACKING_CODE))) {
                trackingCode = extras.getString(Intents.EXTRA_TRACKING_CODE)
            }
            // load brand (if given)
            if (extras.getInt(Intents.EXTRA_BRAND, -1) != -1) {
                brand = TrackableBrand.getById(extras.getInt(Intents.EXTRA_BRAND, -1))
            }
        }

        // no given data
        if (geocode == null) {
            showToast(res.getString(R.string.err_tb_display))
            finish()
            return
        }

        refreshTrackable()
    }

    private Unit refreshTrackable() {
        showProgress(true)

        // create trackable connector
        connector = ConnectorFactory.getTrackableConnector(geocode, brand)
        loggingManager = connector.getTrackableLoggingManager(geocode)

        if (loggingManager == null) {
            showToast(res.getString(R.string.err_tb_not_loggable))
            finish()
        }

        // Initialize the UI
        init()

        createDisposables.add(AndroidRxUtils.bindActivity(this, ConnectorFactory.loadTrackable(geocode, null, null, brand))
                .toSingle()
                .subscribe(newTrackable -> {
                    if (trackingCode != null) {
                        newTrackable.setTrackingcode(trackingCode)
                    }
                    startLoader(newTrackable)
                }, throwable -> {
                    Log.e("cannot load trackable " + geocode, throwable)
                    showProgress(false)

                    if (StringUtils.isNotBlank(geocode)) {
                        showToast(res.getString(R.string.err_tb_not_found, geocode))
                    } else {
                        showToast(res.getString(R.string.err_tb_find_that))
                    }

                    setResult(RESULT_CANCELED)
                    finish()
                }))
    }

    private Unit startLoader(final Trackable newTrackable) {
        trackable = newTrackable
        // Start loading in background
        LoaderManager.getInstance(this).initLoader(LOADER_ID_LOGGING_INFO, null, LogTrackableActivity.this).forceLoad()
        displayTrackable()
    }

    private Unit displayTrackable() {
        // We're in LogTrackableActivity, so trackable must be loggable ;)
        if (!trackable.isLoggable()) {
            showProgress(false)
            showToast(res.getString(R.string.err_tb_not_loggable))
            finish()
            return
        }

        setTitle(res.getString(R.string.trackable_touch) + ": " + StringUtils.defaultIfBlank(trackable.getGeocode(), trackable.getName()))

        // Display tracking code if we have, and move cursor next
        if (trackingCode != null) {
            binding.tracking.setText(trackingCode)
            Dialogs.moveCursorToEnd(binding.tracking)
        }
        init()

        // only after loading we know which menu items for smileys need to be created
        invalidateOptionsMenuCompatible()

        showProgress(false)

        requestKeyboardForLogging()
    }

    override     protected Unit requestKeyboardForLogging() {
        if (StringUtils.isBlank(binding.tracking.getText())) {
            Keyboard.show(this, binding.tracking)

        } else {
            super.requestKeyboardForLogging()
        }
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)

        init()
    }

    private Unit init() {
        updateForNewType()

        // show/hide Time selector
        date.setTimeVisible(loggingManager.canLogTime())

        // Register Coordinates Listener
        if (loggingManager.canLogCoordinates()) {
            binding.geocode.setOnFocusChangeListener(LoadGeocacheListener())
            binding.geocode.setText(geocache.getGeocode())
            updateCoordinates(geocache.getCoords())
            binding.coordinates.setOnClickListener(CoordinatesListener())
        }

        disableSuggestions(binding.tracking)
        initGeocodeSuggestions()
    }

    /**
     * Link the geocodeEditText to the SuggestionsGeocode.
     */
    private Unit initGeocodeSuggestions() {
        binding.geocode.setAdapter(GeocacheAutoCompleteAdapter(binding.geocode.getContext(), DataStore::getSuggestionsGeocode, null))
    }

    public Unit updateForNewType() {

        // show/hide Tracking Code Field for note type
        if (logType.get() != LogTypeTrackable.NOTE || (loggingManager != null && loggingManager.isTrackingCodeNeededToPostNote())) {
            binding.trackingFrame.setVisibility(View.VISIBLE)
            // Request focus if field is empty
            if (StringUtils.isBlank(binding.tracking.getText())) {
                binding.tracking.requestFocus()
            }
        } else {
            binding.trackingFrame.setVisibility(View.GONE)
        }

        // show/hide Coordinate fields as Trackable needs
        if (LogTypeTrackable.isCoordinatesNeeded(logType.get()) && loggingManager != null && loggingManager.canLogCoordinates()) {
            binding.locationFrame.setVisibility(View.VISIBLE)
            // Request focus if field is empty
            if (StringUtils.isBlank(binding.geocode.getText())) {
                binding.geocode.requestFocus()
            }
        } else {
            binding.locationFrame.setVisibility(View.GONE)
        }
    }

    private Unit showProgress(final Boolean loading) {
        readyToPost = !loading
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE)
    }

    public Unit updateCoordinates(final Geopoint geopointIn) {
        if (geopointIn == null) {
            return
        }
        geopoint = geopointIn
        binding.coordinates.setText(geopoint.toString())
        geocache.setCoords(geopoint)
    }
    private class CoordinatesListener : View.OnClickListener {
        override         public Unit onClick(final View theView) {

            CoordinateInputDialog.showLocation(theView.getContext(), this::onCoordinatesUpdated, geopoint)
        }

        public Unit onCoordinatesUpdated(final Geopoint input) {

            updateCoordinates(input)
        }
    }

    private static class LogContextInfoLoader : AsyncTaskLoader()<List<LogTypeTrackable>> {

        private final TrackableLoggingManager loggingManager

        LogContextInfoLoader(final Context context, final TrackableLoggingManager loggingManager) {
            super(context)
            this.loggingManager = loggingManager
        }

        override         protected Unit onStartLoading() {
            forceLoad()
        }

        override         public List<LogTypeTrackable> loadInBackground() {
            return this.loggingManager.getPossibleLogTypesTrackableOnline()
        }
    }


    // React when changing geocode
    private class LoadGeocacheListener : OnFocusChangeListener {
        override         public Unit onFocusChange(final View view, final Boolean hasFocus) {
            if (!hasFocus && StringUtils.isNotBlank(binding.geocode.getText())) {
                val tmpGeocache: Geocache = DataStore.loadCache(binding.geocode.getText().toString(), LoadFlags.LOAD_CACHE_OR_DB)
                if (tmpGeocache == null) {
                    geocache.setGeocode(binding.geocode.getText().toString())
                } else {
                    geocache = tmpGeocache
                    updateCoordinates(geocache.getCoords())
                }
            }
        }
    }

    public static Intent getIntent(final Context context, final Trackable trackable) {
        val logTouchIntent: Intent = Intent(context, LogTrackableActivity.class)
        logTouchIntent.putExtra(Intents.EXTRA_GEOCODE, trackable.getGeocode())
        logTouchIntent.putExtra(Intents.EXTRA_TRACKING_CODE, trackable.getTrackingcode())
        logTouchIntent.putExtra(Intents.EXTRA_BRAND, trackable.getBrand().getId())
        return logTouchIntent
    }

    public static Intent getIntent(final Context context, final Trackable trackable, final String geocache) {
        val logTouchIntent: Intent = getIntent(context, trackable)
        logTouchIntent.putExtra(Intents.EXTRA_GEOCACHE, geocache)
        logTouchIntent.putExtra(Intents.EXTRA_BRAND, trackable.getBrand().getId())
        return logTouchIntent
    }

    override     protected LogContext getLogContext() {
        return LogContext(trackable, null)
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_send) {
            if (connector.isRegistered()) {
                sendLog()
            } else {
                // Redirect user to concerned connector settings
                //Dialogs.confirmYesNo(this, res.getString(R.string.settings_title_open_settings), res.getString(R.string.err_trackable_log_not_anonymous, trackable.getBrand().getLabel(), connector.getServiceTitle()), (dialog, which) -> {
                SimpleDialog.of(this).setTitle(R.string.settings_title_open_settings).setMessage(R.string.err_trackable_log_not_anonymous, trackable.getBrand().getLabel(), connector.getServiceTitle()).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> {
                    if (connector.getPreferenceActivity() > 0) {
                        SettingsActivity.openForScreen(connector.getPreferenceActivity(), LogTrackableActivity.this)
                    } else {
                        showToast(res.getString(R.string.err_trackable_no_preference_activity))
                    }
                })
            }
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Do form validation then post the Log
     */
    private Unit sendLog() {
        // Can logging?
        if (!readyToPost) {
            SimpleDialog.of(this).setMessage(R.string.log_post_not_possible).show()
            return
        }

        // Check Tracking Code existence
        if (loggingManager.isTrackingCodeNeededToPostNote() && binding.tracking.getText().toString().isEmpty()) {
            SimpleDialog.of(this).setMessage(R.string.err_log_post_missing_tracking_code).show()
            return
        }
        trackable.setTrackingcode(binding.tracking.getText().toString())

        // Check params for trackables needing coordinates
        if (loggingManager.canLogCoordinates() && LogTypeTrackable.isCoordinatesNeeded(logType.get()) && geopoint == null) {
            SimpleDialog.of(this).setMessage(R.string.err_log_post_missing_coordinates).show()
            return
        }

        // Some Trackable connectors recommend logging with a Geocode.
        // Note: Currently, counter is shared between all connectors recommending Geocode.
        if (LogTypeTrackable.isCoordinatesNeeded(logType.get()) && loggingManager.canLogCoordinates() &&
                connector.recommendLogWithGeocode() && binding.geocode.getText().toString().isEmpty() &&
                Settings.getLogTrackableWithoutGeocodeShowCount() < MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE) {
            LogTrackableWithoutGeocodeBuilder().create(this).show()
        } else {
            postLog()
        }
    }

    /**
     * Post Log in Background
     */
    private Unit postLog() {
    val logEntry: TrackableLogEntry = TrackableLogEntry.of(trackable)
            .setAction(logType.get())
            .setDate(date.getDate())
            .setLog(binding.log.getText().toString())

        logActivityHelper.createLogTrackable(geocache, logEntry, connector)

        Settings.setTrackableAction(logType.get().id)
        Settings.setLastTrackableLog(binding.log.getText().toString())
    }

    private Unit onPostExecuteInternal(final LogResult status) {
        if (status.isOk()) {
            showToast(res.getString(R.string.info_log_posted))
            finish()
        } else {
            showToast(status.getErrorString())
        }
    }

    override     public Unit finish() {
        super.finish()
        this.logActivityHelper.finish()
    }

    override     protected String getLastLog() {
        return Settings.getLastTrackableLog()
    }

    /**
     * This will display a popup for confirming if Trackable Log should be send without a geocode.
     * It will be displayed only MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE times. A "Do not ask me again"
     * checkbox is also added.
     */
    class LogTrackableWithoutGeocodeBuilder {

        private CheckBox doNotAskAgain

        public AlertDialog create(final Activity activity) {
            final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
            builder.setTitle(R.string.trackable_title_log_without_geocode)

            val layout: View = View.inflate(activity, R.layout.logtrackable_without_geocode, null)
            builder.setView(layout)

            doNotAskAgain = layout.findViewById(R.id.logtrackable_do_not_ask_me_again)

            val showCount: Int = Settings.getLogTrackableWithoutGeocodeShowCount()
            Settings.setLogTrackableWithoutGeocodeShowCount(showCount + 1)

            builder.setPositiveButton(string.yes, (dialog, which) -> {
                checkDoNotAskAgain()
                dialog.dismiss()
            })
            builder.setNegativeButton(string.no, (dialog, which) -> {
                checkDoNotAskAgain()
                dialog.dismiss()
                // Post the log
                postLog()
            })
            return builder.create()
        }

        /**
         * Verify if doNotAskAgain is checked.
         * If true, set the counter to MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE
         */
        private Unit checkDoNotAskAgain() {
            if (doNotAskAgain.isChecked()) {
                Settings.setLogTrackableWithoutGeocodeShowCount(MAX_SHOWN_POPUP_TRACKABLE_WITHOUT_GEOCODE)
            }
        }
    }

}
