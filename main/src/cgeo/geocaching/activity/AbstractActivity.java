package cgeo.geocaching.activity;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.TranslationUtils;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractActivity extends AppCompatActivity implements IAbstractActivity {

    protected CgeoApplication app = null;
    protected Resources res = null;
    private boolean keepScreenOn = false;
    private final CompositeDisposable resumeDisposable = new CompositeDisposable();

    protected AbstractActivity() {
        this(false);
    }

    protected AbstractActivity(final boolean keepScreenOn) {
        this.keepScreenOn = keepScreenOn;
    }

    protected final void showProgress(final boolean show) {
        try {
            ActivityMixin.showProgress(this, show);
        } catch (final Exception ex) {
            Log.e(String.format("Error seeting progress: %b", show, Locale.US), ex);
        }
    }

    protected final void setTheme() {
        ActivityMixin.setTheme(this);
    }

    @Override
    public final void showToast(final String text) {
        ActivityMixin.showToast(this, text);
    }

    @Override
    public final void showShortToast(final String text) {
        ActivityMixin.showShortToast(this, text);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
        resumeDisposable.clear();
        super.onPause();
    }

    protected static void disableSuggestions(final EditText edit) {
        EditUtils.disableSuggestions(edit);
    }

    protected void restartActivity() {
        final Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    public void invalidateOptionsMenuCompatible() {
        ActivityMixin.invalidateOptionsMenu(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateCommon();
    }

    protected void onCreate(final Bundle savedInstanceState, @LayoutRes final int resourceLayoutID) {
        super.onCreate(savedInstanceState);
        onCreateCommon();

        // non declarative part of layout
        setTheme();

        setContentView(resourceLayoutID);

        // create view variables
        ButterKnife.bind(this);
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

        ActivityMixin.onCreate(this, keepScreenOn);
    }

    @Override
    public void setContentView(@LayoutRes final int layoutResID) {
        super.setContentView(layoutResID);

        // initialize the action bar title with the activity title for single source
        ActivityMixin.setTitle(this, getTitle());
    }

    protected void hideKeyboard() {
        new Keyboard(this).hide();
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
        if (clickedItemText == null) {
            return false;
        }
        switch (item.getItemId()) {
            // detail fields
            case R.id.menu_copy:
                ClipboardUtils.copyToClipboard(clickedItemText);
                showToast(res.getString(R.string.clipboard_copy_ok));
                actionMode.finish();
                return true;
            case R.id.menu_translate_to_sys_lang:
                TranslationUtils.startActivityTranslate(this, Locale.getDefault().getLanguage(), HtmlUtils.extractText(clickedItemText));
                actionMode.finish();
                return true;
            case R.id.menu_translate_to_english:
                TranslationUtils.startActivityTranslate(this, Locale.ENGLISH.getLanguage(), HtmlUtils.extractText(clickedItemText));
                actionMode.finish();
                return true;
            case R.id.menu_extract_waypoints:
                extractWaypoints(clickedItemText, cache);
                actionMode.finish();
                return true;
            case R.id.menu_cache_share_field:
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, clickedItemText.toString());
                startActivity(Intent.createChooser(intent, res.getText(R.string.cache_share_field)));
                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    protected void extractWaypoints(@Nullable final CharSequence text, @Nullable final Geocache cache) {
        if (cache != null) {
            final int previousNumberOfWaypoints = cache.getWaypoints().size();
            final boolean success = cache.addWaypointsFromText(HtmlUtils.extractText(text), true, res.getString(R.string.cache_description));
            final int waypointsAdded = cache.getWaypoints().size() - previousNumberOfWaypoints;
            showToast(res.getQuantityString(R.plurals.extract_waypoints_result, waypointsAdded, waypointsAdded));
            if (success) {
                final Intent intent = new Intent(Intents.INTENT_CACHE_CHANGED);
                intent.putExtra(Intents.EXTRA_WPT_PAGE_UPDATE, true);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
        assert title != null; // help Eclipse null analysis
        setCacheTitleBar(title, type);
    }

    private void setCacheTitleBar(@NonNull final CharSequence title, @Nullable final CacheType type) {
        setTitle(title);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (type != null) {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setIcon(Compatibility.getDrawable(getResources(), type.markerId));
            } else {
                actionBar.setIcon(android.R.color.transparent);
            }
        }
    }

    /**
     * change the titlebar icon and text to show the current geocache
     */
    protected void setCacheTitleBar(@NonNull final Geocache cache) {
        setTitle(TextUtils.coloredCacheText(cache, cache.getName() + " (" + cache.getGeocode() + ")"));
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
}
