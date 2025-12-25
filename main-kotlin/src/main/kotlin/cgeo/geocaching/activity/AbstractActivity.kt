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

package cgeo.geocaching.activity

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.CalculatedCoordinate
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.ApplicationSettings
import cgeo.geocaching.utils.EditUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.html.HtmlUtils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.AndroidRuntimeException
import android.util.Pair
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.EditText

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.ViewBinding

import java.util.ArrayList
import java.util.List

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class AbstractActivity : AppCompatActivity() : IAbstractActivity {

    protected var app: CgeoApplication = null
    protected var res: Resources = null
    private val resumeDisposable: CompositeDisposable = CompositeDisposable()

    private val logToken: String = "[" + this.getClass().getName() + "]"

    private Insets currentWindowInsets

    private static val ACTION_CLEAR_BACKSTACK: String = "cgeo.geocaching.ACTION_CLEAR_BACKSTACK"

    protected final Unit setTheme() {
        ActivityMixin.setTheme(this)
    }

    // edge2edge parametrization, see configureEdge2Edge()
    private static val DEFAULT_INSETS: Int =
            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime()
    //private var addInsets: Int = 0
    //private var skipActionBarInsetCalculation: Boolean = false

    public Unit setUpNavigationEnabled(final Boolean enabled) {
        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled)
        }
    }

    override     public final Unit showToast(final String text) {
        ActivityMixin.showToast(this, text)
    }

    public final Unit showToast(final Int textId) {
        showToast(getString(textId))
    }

    override     public final Unit showShortToast(final String text) {
        ActivityMixin.showShortToast(this, text)
    }

    public final Unit showShortToast(final Int textId) {
        showShortToast(getString(textId))
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        Log.v(logToken + ".onOptionsItemSelected(" + item.getItemId() + "/" + item.getTitle() + ")")
        if (item.getItemId() == android.R.id.home) {
            return ActivityMixin.navigateUp(this)
        }
        return super.onOptionsItemSelected(item)
    }

    protected Unit resumeDisposables(final Disposable... resumeDisposable) {
        this.resumeDisposable.addAll(resumeDisposable)
    }

    override     public Unit onPause() {
        Log.v(logToken + ".onPause")
        resumeDisposable.clear()
        super.onPause()
    }

    protected static Unit disableSuggestions(final EditText edit) {
        EditUtils.disableSuggestions(edit)
    }

    override     public Unit invalidateOptionsMenuCompatible() {
        Log.v(logToken + ".invalidateOptionsMenuCompatible")
        ActivityMixin.invalidateOptionsMenu(this)
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        Log.v(logToken + ".onCreate(Bundle)")
        ApplicationSettings.setLocale(this)
        try {
            super.onCreate(savedInstanceState)
        } catch (Exception e) {
            Log.e(e.toString())
            throw e
        }

        this.getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, ACTION_CLEAR_BACKSTACK) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                finish()
            }
        })

        try {
            supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        } catch (final AndroidRuntimeException ex) {
            Log.e("Error requesting indeterminate progress", ex)
        }

        // initialize commonly used members
        res = this.getResources()
        app = (CgeoApplication) this.getApplication()
        ActivityMixin.onCreate(this, false)
        initEdgeToEdge()
    }

    private Unit initEdgeToEdge() {
        val currentWindow: Window = getWindow()
        //enable edge-to-edge downward-compatible
        WindowCompat.enableEdgeToEdge(currentWindow)
        //set window behaviour
        val windowInsetsController: WindowInsetsControllerCompat = WindowCompat.getInsetsController(currentWindow, currentWindow.getDecorView())

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        //apply edge2edge to activity content view
        ViewCompat.setOnApplyWindowInsetsListener(currentWindow.getDecorView(), (v, windowInsets) -> {
            val activityContent: View = v.findViewById(R.id.activity_content)
            if (activityContent == null) {
                Log.w("edge2edge: activityContent not found in " + this)
            } else {
                //calculate and set the activity_content's insets
                this.currentWindowInsets = windowInsets.getInsets(DEFAULT_INSETS)
                //trigger insets recalculation
                refreshActivityContentInsets()
            }
            return windowInsets
        })

        // adjust system bars appearance, depending on action bar color and visibility
        ActionBarUtils.setSystemBarAppearance(this, true)
    }

    /** Call if activityContent's edge-2-edge-padding needs to be reevaluated */
    protected Unit refreshActivityContentInsets() {
        if (this.currentWindowInsets == null) {
            //method was called before insets were set
            return
        }
        val activityContent: View = getWindow() == null || getWindow().getDecorView() == null ? null :
            getWindow().getDecorView().findViewById(R.id.activity_content)
        if (activityContent == null) {
            return
        }

        //let subclasses modify insets according to their needs
        val insets: Insets = calculateInsetsForActivityContent(this.currentWindowInsets)
        //apply final insets to activity content
        activityContent.setPadding(
                insets.left < 0 ? this.currentWindowInsets.left : insets.left,
                insets.top < 0 ? this.currentWindowInsets.top : insets.top,
                insets.right < 0 ? this.currentWindowInsets.right : insets.right,
                insets.bottom < 0 ? this.currentWindowInsets.bottom : insets.bottom)

    }

    /** Overwrite to manipulate edge-2-edge-insets for activitycontent in subclasses */
    protected Insets calculateInsetsForActivityContent(final Insets insets) {
        return insets
    }

    public Unit clearBackStack() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_CLEAR_BACKSTACK))
    }

    protected Unit setThemeAndContentView(@LayoutRes final Int resourceLayoutID) {
        setThemeAndContentView(resourceLayoutID, false)
    }

    protected Unit setThemeAndContentView(final ViewBinding binding) {
        ActivityMixin.setTheme(this, false)
        setContentView(binding.getRoot())
    }

    protected Unit setThemeAndContentView(@LayoutRes final Int resourceLayoutID, final Boolean isDialog) {
        Log.v(logToken + ".setThemeAndContentView(resourceLayoutId=" + resourceLayoutID + ", isDialog= " + isDialog + ")")
        ActivityMixin.setTheme(this, isDialog)
        setContentView(resourceLayoutID)
    }

    override     public Unit setContentView(@LayoutRes final Int layoutResID) {
        Log.v(logToken + ".setContentView(" + layoutResID + ")")
        super.setContentView(layoutResID)

        // initialize the action bar title with the activity title for single source
        ActivityMixin.setTitle(this, getTitle())
    }

    protected Unit hideKeyboard() {
        Keyboard.hide(this)
    }

    protected Unit extractWaypoints(final CharSequence text, final Geocache cache) {
        if (cache != null) {
            val previousNumberOfWaypoints: Int = cache.getWaypoints().size()
            val success: Boolean = cache.addCacheArtefactsFromText(HtmlUtils.extractText(text), true, res.getString(R.string.cache_description), true, null)
            val waypointsAdded: Int = cache.getWaypoints().size() - previousNumberOfWaypoints
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, waypointsAdded, waypointsAdded))
            if (success) {
                GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode())
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0))
        }
    }

    protected Unit scanForCalculatedWaypoints(final Geocache cache) {
        if (cache != null) {
            val toScan: List<String> = ArrayList<>()
            final List<Pair<String, String>> existingCoords = ArrayList<>()
            toScan.add(TextUtils.stripHtml(cache.getDescription()))
            toScan.add(cache.getHint())
            for (Waypoint w : cache.getWaypoints()) {
                toScan.add(w.getNote())
                if (w.isCalculated()) {
                    val cc: CalculatedCoordinate = CalculatedCoordinate.createFromConfig(w.getCalcStateConfig())
                    existingCoords.add(Pair<>(cc.getLatitudePattern(), cc.getLongitudePattern()))
                }
            }
            final List<Pair<String, String>> patterns = FormulaUtils.scanForCoordinates(toScan, existingCoords)
            if (patterns.isEmpty()) {
                ActivityMixin.showShortToast(this, R.string.variables_scanlisting_nopatternfound)
            } else {
                final SimpleDialog.ItemSelectModel<Pair<String, String>> model = SimpleDialog.ItemSelectModel<>()
                model.setItems(patterns).setDisplayMapper((s) -> TextParam.text("`" + s.first + " | " + s.second + "`").setMarkdown(true))

                SimpleDialog.of(this).setTitle(TextParam.id(R.string.variables_scanlisting_choosepattern_title))
                        .selectMultiple(model, set -> {
                            val added: Int = cache.addCalculatedWaypoints(set, LocalizationUtils.getString(R.string.calccoord_generate_waypointnameprefix))
                            if (added > 0) {
                                GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode())
                            }
                        })
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0))
        }
    }

    override     public Unit finish() {
        Log.v(logToken + ".finish()")
        super.finish()
    }

    override     protected Unit onStop() {
        Log.v(logToken + ".onStop()")
        super.onStop()
    }

    override     protected Unit onStart() {
        Log.v(logToken + ".onStart()")
        super.onStart()
    }

    override     protected Unit onResume() {
        Log.v(logToken + ".onResume()")
        super.onResume()
    }

    override     protected Unit onRestart() {
        Log.v(logToken + ".onRestart()")
        super.onRestart()
    }
}
