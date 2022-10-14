package cgeo.geocaching.activity;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.TranslationUtils;
import cgeo.geocaching.utils.formulas.FormulaUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractActivity extends AppCompatActivity implements IAbstractActivity {

    protected CgeoApplication app = null;
    protected Resources res = null;
    private final CompositeDisposable resumeDisposable = new CompositeDisposable();

    private final String logToken = "[" + this.getClass().getName() + "]";

    private static final String ACTION_CLEAR_BACKSTACK = "cgeo.geocaching.ACTION_CLEAR_BACKSTACK";
    private final BroadcastReceiver finishBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            finish();
        }
    };

    protected final void setTheme() {
        ActivityMixin.setTheme(this);
    }

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
        onCreateCommon();
        this.getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, ACTION_CLEAR_BACKSTACK) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                finish();
            }
        });
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

    /**
     * Common actions for all onCreate functions.
     */
    private void onCreateCommon() {
        try {
            supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } catch (final AndroidRuntimeException ex) {
            Log.e("Error requesting indeterminate progress", ex);
        }
        AndroidBeam.disable(this);
        initializeCommonFields();
    }

    private void initializeCommonFields() {

        // initialize commonly used members
        res = this.getResources();
        app = (CgeoApplication) this.getApplication();

        ActivityMixin.onCreate(this, false);
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

    protected void buildDetailsContextMenu(final ActionMode actionMode, final Menu menu, final CharSequence fieldTitle, final boolean copyOnly) {
        actionMode.setTitle(fieldTitle);
        menu.findItem(R.id.menu_translate_to_sys_lang).setVisible(!copyOnly);
        if (!copyOnly) {
            menu.findItem(R.id.menu_translate_to_sys_lang).setTitle(res.getString(R.string.translate_to_sys_lang, Locale.getDefault().getDisplayLanguage()));
        }
        final boolean localeIsEnglish = StringUtils.equals(Locale.getDefault().getLanguage(), Locale.ENGLISH.getLanguage());
        menu.findItem(R.id.menu_translate_to_english).setVisible(!copyOnly && !localeIsEnglish);
    }

    protected boolean onClipboardItemSelected(@NonNull final ActionMode actionMode, final MenuItem item, final CharSequence clickedItemText, @Nullable final Geocache cache) {
        Log.v(logToken + ".onClipboardItemSelected: " + clickedItemText);
        if (clickedItemText == null) {
            return false;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_copy) {
            ClipboardUtils.copyToClipboard(clickedItemText);
            showToast(res.getString(R.string.clipboard_copy_ok));
            actionMode.finish();
        } else if (itemId == R.id.menu_translate_to_sys_lang) {
            TranslationUtils.startActivityTranslate(this, Locale.getDefault().getLanguage(), HtmlUtils.extractText(clickedItemText));
            actionMode.finish();
        } else if (itemId == R.id.menu_translate_to_english) {
            TranslationUtils.startActivityTranslate(this, Locale.ENGLISH.getLanguage(), HtmlUtils.extractText(clickedItemText));
            actionMode.finish();
        } else if (itemId == R.id.menu_extract_waypoints) {
            extractWaypoints(clickedItemText, cache);
            actionMode.finish();
        } else if (itemId == R.id.menu_cache_share_field) {
            ShareUtils.sharePlainText(this, clickedItemText.toString());
            actionMode.finish();
        } else {
            return false;
        }
        return true;
    }

    protected void extractWaypoints(@Nullable final CharSequence text, @Nullable final Geocache cache) {
        if (cache != null) {
            final int previousNumberOfWaypoints = cache.getWaypoints().size();
            final boolean success = cache.addWaypointsFromText(HtmlUtils.extractText(text), true, res.getString(R.string.cache_description), true);
            final int waypointsAdded = cache.getWaypoints().size() - previousNumberOfWaypoints;
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, waypointsAdded, waypointsAdded));
            if (success) {
                GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0));
        }
    }

    protected void scanForCalculatedWaypints(@Nullable final Geocache cache) {
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
                SimpleDialog.of(this).setTitle(TextParam.id(R.string.variables_scanlisting_choosepattern_title))
                        .selectMultiple(patterns, (s, i) -> TextParam.text("`" + s.first + " | " + s.second + "`").setMarkdown(true), null, set -> cache.addCalculatedWaypoints(set, LocalizationUtils.getString(R.string.calccoord_generate_waypointnameprefix)));
            }
        } else {
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, 0));
        }
    }

    protected void setCacheTitleBar(@Nullable final String geocode, @Nullable final CharSequence name, @Nullable final CacheType type) {
        final CharSequence title;
        if (StringUtils.isNotBlank(name)) {
            title = StringUtils.isNotBlank(geocode) ? name + " (" + geocode + ")" : name;
        } else {
            title = StringUtils.isNotBlank(geocode) ? geocode : res.getString(R.string.cache);
        }
        setCacheTitleBar(title, type);
    }

    private void setCacheTitleBar(@NonNull final CharSequence title, @Nullable final CacheType type) {
        setTitle(title);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (type != null) {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setIcon(ResourcesCompat.getDrawable(getResources(), type.markerId, null));
            } else {
                actionBar.setIcon(android.R.color.transparent);
            }
        }
    }

    /**
     * change the titlebar icon and text to show the current geocache
     */
    protected void setCacheTitleBar(@NonNull final Geocache cache) {
        setTitle(TextUtils.coloredCacheText(cache, cache.getName() + " (" + cache.getShortGeocode() + ")"));
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.OFFLINE).getDrawable());
        }
    }

    /**
     * change the titlebar icon and text to show the current geocache
     */
    protected void setCacheTitleBar(@Nullable final String geocode) {
        if (StringUtils.isEmpty(geocode)) {
            return;
        }
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache == null) {
            Log.e("AbstractActivity.setCacheTitleBar: cannot find the cache " + geocode);
            return;
        }
        setCacheTitleBar(cache);
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
