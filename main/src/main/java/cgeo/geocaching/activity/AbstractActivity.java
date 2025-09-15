package cgeo.geocaching.activity;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ActionBarUtils;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.FormulaUtils;
import cgeo.geocaching.utils.html.HtmlUtils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class AbstractActivity extends AppCompatActivity implements IAbstractActivity {

    protected CgeoApplication app = null;
    protected Resources res = null;
    private final CompositeDisposable resumeDisposable = new CompositeDisposable();

    private final String logToken = "[" + this.getClass().getName() + "]";

    private Insets currentWindowInsets;

    private static final String ACTION_CLEAR_BACKSTACK = "cgeo.geocaching.ACTION_CLEAR_BACKSTACK";

    protected final void setTheme() {
        ActivityMixin.setTheme(this);
    }

    // edge2edge parametrization, see configureEdge2Edge()
    private static final int DEFAULT_INSETS =
            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime();
    //private int addInsets = 0;
    //private boolean skipActionBarInsetCalculation = false;

    public void setUpNavigationEnabled(final boolean enabled) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }

    @Override
    public final void showToast(final String text) {
        ActivityMixin.showToast(this, text);
    }

    public final void showToast(final int textId) {
        showToast(getString(textId));
    }

    @Override
    public final void showShortToast(final String text) {
        ActivityMixin.showShortToast(this, text);
    }

    public final void showShortToast(final int textId) {
        showShortToast(getString(textId));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.v(logToken + ".onOptionsItemSelected(" + item.getItemId() + "/" + item.getTitle() + ")");
        if (item.getItemId() == android.R.id.home) {
            return ActivityMixin.navigateUp(this);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void resumeDisposables(final Disposable... resumeDisposable) {
        this.resumeDisposable.addAll(resumeDisposable);
    }

    @Override
    public void onPause() {
        Log.v(logToken + ".onPause");
        resumeDisposable.clear();
        super.onPause();
    }

    protected static void disableSuggestions(final EditText edit) {
        EditUtils.disableSuggestions(edit);
    }

    @Override
    public void invalidateOptionsMenuCompatible() {
        Log.v(logToken + ".invalidateOptionsMenuCompatible");
        ActivityMixin.invalidateOptionsMenu(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.v(logToken + ".onCreate(Bundle)");
        ApplicationSettings.setLocale(this);
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {
            Log.e(e.toString());
            throw e;
        }

        this.getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, ACTION_CLEAR_BACKSTACK) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                finish();
            }
        });

        try {
            supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } catch (final AndroidRuntimeException ex) {
            Log.e("Error requesting indeterminate progress", ex);
        }

        // initialize commonly used members
        res = this.getResources();
        app = (CgeoApplication) this.getApplication();
        ActivityMixin.onCreate(this, false);
        initEdgeToEdge();
    }

    private void initEdgeToEdge() {
        final Window currentWindow = getWindow();
        //enable edge-to-edge downward-compatible
        WindowCompat.enableEdgeToEdge(currentWindow);
        //set window behaviour
        final WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(currentWindow, currentWindow.getDecorView());

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        //apply edge2edge to activity content view
        ViewCompat.setOnApplyWindowInsetsListener(currentWindow.getDecorView(), (v, windowInsets) -> {
            final View activityContent = v.findViewById(R.id.activity_content);
            if (activityContent == null) {
                Log.w("edge2edge: activityContent not found in " + this);
            } else {
                //calculate and set the activity_content's insets
                this.currentWindowInsets = windowInsets.getInsets(DEFAULT_INSETS);
                //trigger insets recalculation
                refreshActivityContentInsets();
            }
            return windowInsets;
        });

        // adjust system bars appearance, depending on action bar color and visibility
        ActionBarUtils.setSystemBarAppearance(this, true);
    }

    /** Call if activityContent's edge-2-edge-padding needs to be reevaluated */
    protected void refreshActivityContentInsets() {
        if (this.currentWindowInsets == null) {
            //method was called before insets were set
            return;
        }
        final View activityContent = getWindow() == null || getWindow().getDecorView() == null ? null :
            getWindow().getDecorView().findViewById(R.id.activity_content);
        if (activityContent == null) {
            return;
        }

        //let subclasses modify insets according to their needs
        final Insets insets = calculateInsetsForActivityContent(this.currentWindowInsets);
        //apply final insets to activity content
        activityContent.setPadding(
                insets.left < 0 ? this.currentWindowInsets.left : insets.left,
                insets.top < 0 ? this.currentWindowInsets.top : insets.top,
                insets.right < 0 ? this.currentWindowInsets.right : insets.right,
                insets.bottom < 0 ? this.currentWindowInsets.bottom : insets.bottom);

    }

    /** Overwrite to manipulate edge-2-edge-insets for activitycontent in subclasses */
    @NonNull
    protected Insets calculateInsetsForActivityContent(@NonNull final Insets insets) {
        return insets;
    }

    public void clearBackStack() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_CLEAR_BACKSTACK));
    }

    protected void setThemeAndContentView(@LayoutRes final int resourceLayoutID) {
        setThemeAndContentView(resourceLayoutID, false);
    }

    protected void setThemeAndContentView(final ViewBinding binding) {
        ActivityMixin.setTheme(this, false);
        setContentView(binding.getRoot());
    }

    protected void setThemeAndContentView(@LayoutRes final int resourceLayoutID, final boolean isDialog) {
        Log.v(logToken + ".setThemeAndContentView(resourceLayoutId=" + resourceLayoutID + ", isDialog= " + isDialog + ")");
        ActivityMixin.setTheme(this, isDialog);
        setContentView(resourceLayoutID);
    }

    @Override
    public void setContentView(@LayoutRes final int layoutResID) {
        Log.v(logToken + ".setContentView(" + layoutResID + ")");
        super.setContentView(layoutResID);

        // initialize the action bar title with the activity title for single source
        ActivityMixin.setTitle(this, getTitle());
    }

    protected void hideKeyboard() {
        Keyboard.hide(this);
    }

    protected void extractWaypoints(@Nullable final CharSequence text, @Nullable final Geocache cache) {
        if (cache != null) {
            final int previousNumberOfWaypoints = cache.getWaypoints().size();
            final boolean success = cache.addCacheArtefactsFromText(HtmlUtils.extractText(text), true, res.getString(R.string.cache_description), true, null);
            final int waypointsAdded = cache.getWaypoints().size() - previousNumberOfWaypoints;
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, waypointsAdded, waypointsAdded));
            if (success) {
                GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0));
        }
    }

    protected void scanForCalculatedWaypoints(@Nullable final Geocache cache) {
        if (cache != null) {
            final List<String> toScan = new ArrayList<>();
            final List<Pair<String, String>> existingCoords = new ArrayList<>();
            toScan.add(TextUtils.stripHtml(cache.getDescription()));
            toScan.add(cache.getHint());
            for (Waypoint w : cache.getWaypoints()) {
                toScan.add(w.getNote());
                if (w.isCalculated()) {
                    final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(w.getCalcStateConfig());
                    existingCoords.add(new Pair<>(cc.getLatitudePattern(), cc.getLongitudePattern()));
                }
            }
            final List<Pair<String, String>> patterns = FormulaUtils.scanForCoordinates(toScan, existingCoords);
            if (patterns.isEmpty()) {
                ActivityMixin.showShortToast(this, R.string.variables_scanlisting_nopatternfound);
            } else {
                final SimpleDialog.ItemSelectModel<Pair<String, String>> model = new SimpleDialog.ItemSelectModel<>();
                model.setItems(patterns).setDisplayMapper((s) -> TextParam.text("`" + s.first + " | " + s.second + "`").setMarkdown(true));

                SimpleDialog.of(this).setTitle(TextParam.id(R.string.variables_scanlisting_choosepattern_title))
                        .selectMultiple(model, set -> {
                            final int added = cache.addCalculatedWaypoints(set, LocalizationUtils.getString(R.string.calccoord_generate_waypointnameprefix));
                            if (added > 0) {
                                GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
                            }
                        });
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0));
        }
    }

    @Override
    public void finish() {
        Log.v(logToken + ".finish()");
        super.finish();
    }

    @Override
    protected void onStop() {
        Log.v(logToken + ".onStop()");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.v(logToken + ".onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.v(logToken + ".onResume()");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.v(logToken + ".onRestart()");
        super.onRestart();
    }
}
