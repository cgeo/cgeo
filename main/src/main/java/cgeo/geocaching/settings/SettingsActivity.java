package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.permission.PermissionAction;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.search.BaseSearchSuggestionCursor;
import cgeo.geocaching.search.BaseSuggestionsAdapter;
import cgeo.geocaching.search.SearchUtils;
import cgeo.geocaching.settings.fragments.BasePreferenceFragment;
import cgeo.geocaching.settings.fragments.PreferenceAppearanceFragment;
import cgeo.geocaching.settings.fragments.PreferenceBackupFragment;
import cgeo.geocaching.settings.fragments.PreferenceCachedetailsFragment;
import cgeo.geocaching.settings.fragments.PreferenceLoggingFragment;
import cgeo.geocaching.settings.fragments.PreferenceMapFragment;
import cgeo.geocaching.settings.fragments.PreferenceNavigationFragment;
import cgeo.geocaching.settings.fragments.PreferenceOfflinedataFragment;
import cgeo.geocaching.settings.fragments.PreferenceServiceGeocachingComFragment;
import cgeo.geocaching.settings.fragments.PreferenceServiceGeokretyOrgFragment;
import cgeo.geocaching.settings.fragments.PreferenceServiceSendToCgeoFragment;
import cgeo.geocaching.settings.fragments.PreferenceServicesFragment;
import cgeo.geocaching.settings.fragments.PreferenceSystemFragment;
import cgeo.geocaching.settings.fragments.PreferencesFragment;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;

import android.app.SearchManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * An {@link AppCompatActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design
 * guidelines and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for
 * more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "preferencesActivityTitle";
    private static final String INTENT_OPEN_SCREEN = "OPEN_SCREEN";
    public static final int NO_RESTART_NEEDED = 1;
    public static final int RESTART_NEEDED = 2;

    public static final String STATE_CSAH = "csah";
    public static final String STATE_BACKUPUTILS = "backuputils";

    private BackupUtils backupUtils = null;
    private ContentStorageActivityHelper contentStorageHelper = null;
    private CharSequence title;

    private static final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchIndex = new ArrayList<>();

    private final PermissionAction<Void> askShowWallpaperPermissionAction = PermissionAction.register(this, PermissionContext.SHOW_WALLPAPER, null);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this);
        super.onCreate(savedInstanceState);

        backupUtils = new BackupUtils(SettingsActivity.this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        this.contentStorageHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, folder -> {

                    final List<Fragment> fragments = getSupportFragmentManager().getFragments();
                    for (Fragment f : fragments) {
                        if (f instanceof PreferenceFragmentCompat) {
                            initPublicFolders((PreferenceFragmentCompat) f, contentStorageHelper);
                        }
                    }

                    if (PersistableFolder.OFFLINE_MAP_THEMES.equals(folder)) {
                        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();
                    }
                });

        setContentView(R.layout.layout_settings);
        buildSearchIndex();

        handleIntent(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.settings_titlebar);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AndroidBeam.disable(this);

        setResult(NO_RESTART_NEEDED);
    }

    public BackupUtils getBackupUtils() {
        return backupUtils;
    }

    public void askShowWallpaperPermission() {
        this.askShowWallpaperPermissionAction.launch();
    }

    private void handleIntent(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            final String action = intent.getAction();
            boolean found = false;
            if (Intents.ACTION_SETTINGS.equals(action)) {
                // user selected a search suggestion
                openRequestedFragment(intent.getStringExtra(SearchManager.QUERY));
                found = true;
            } else if (Intent.ACTION_SEARCH.equals(action)) {
                // user pressed enter in searchfield => search first pref matching this search string
                final String query = intent.getStringExtra(SearchManager.QUERY);
                synchronized (searchIndex) {
                    for (BasePreferenceFragment.PrefSearchDescriptor item : searchIndex) {
                        if (StringUtils.containsIgnoreCase(item.prefTitle, query) || StringUtils.containsIgnoreCase(item.prefSummary, query)) {
                            openRequestedFragment(item.baseKey, item.prefKey);
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                openRequestedFragment("");
            }
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG);
        }
    }

    /**
     * This method sets the fragment which is used upon opening the settings. This may be the user directly or a
     * requesting Intent.
     */
    private void openRequestedFragment(final String preference) {
        final Intent intent = getIntent();
        final int fragmentId = intent.getIntExtra(INTENT_OPEN_SCREEN, -1);
        String fragment = "";
        // try to get fragment key from preference name (if necessary)
        if (fragmentId < 0 && StringUtils.isNotBlank(preference)) {
            synchronized (searchIndex) {
                for (BasePreferenceFragment.PrefSearchDescriptor pref : searchIndex) {
                    if (StringUtils.equals(preference, pref.prefKey)) {
                        fragment = pref.baseKey;
                        break;
                    }
                }
            }
        } else {
            try {
                fragment = getString(fragmentId);
            } catch (Exception ignore) {
                fragment = "";
            }

        }
        openRequestedFragment(fragment, preference);
    }

    /**
     * This method sets the fragment given by its base key,
     * and optionally starts scrolling to a preference identified by its key
     */
    private void openRequestedFragment(@NonNull final String baseKey, @Nullable final String scrollToPrefKey) {
        Fragment preferenceFragment = new PreferencesFragment();
        if (StringUtils.equals(baseKey, getString(R.string.preference_screen_services))) {
            preferenceFragment = new PreferenceServicesFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_appearance))) {
            preferenceFragment = new PreferenceAppearanceFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_cachedetails))) {
            preferenceFragment = new PreferenceCachedetailsFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_map))) {
            preferenceFragment = new PreferenceMapFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_map))) {
            preferenceFragment = new PreferenceMapFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_logging))) {
            preferenceFragment = new PreferenceLoggingFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_offlinedata))) {
            preferenceFragment = new PreferenceOfflinedataFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_navigation))) {
            preferenceFragment = new PreferenceNavigationFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_system))) {
            preferenceFragment = new PreferenceSystemFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_backup))) {
            preferenceFragment = new PreferenceBackupFragment();

        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_sendtocgeo))) {
            preferenceFragment = new PreferenceServiceSendToCgeoFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_geokrety))) {
            preferenceFragment = new PreferenceServiceGeokretyOrgFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.preference_screen_gc))) {
            preferenceFragment = new PreferenceServiceGeocachingComFragment();
        }
        if (StringUtils.isNotBlank(scrollToPrefKey)) {
            ((BasePreferenceFragment) preferenceFragment).setScrollToPrefCallback(this::scrollToCallback, baseKey, scrollToPrefKey);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, preferenceFragment)
                .commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_CSAH, contentStorageHelper.getState());
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());

        // Save current activity title so we can set it again after a configuration change
        savedInstanceState.putCharSequence(TITLE_TAG, title);
    }

    @Override
    protected void onPause() {
        Log.i("Requesting settings backup with settings manager");
        BackupManager.dataChanged(getPackageName());
        super.onPause();
    }

    public static void openForScreen(final int preferenceScreenKey, final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey);
        fromActivity.startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }


    @Override
    public boolean onPreferenceStartFragment(final PreferenceFragmentCompat caller, final Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_root, fragment)
                .addToBackStack(null)
                .commit();
        title = pref.getTitle();
        return true;
    }

    public ContentStorageActivityHelper getCsah() {
        return contentStorageHelper;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (contentStorageHelper.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (backupUtils.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
    }

    // search related extensions

    private void buildSearchIndex() {
        synchronized (searchIndex) {
            if (searchIndex.size() > 0) {
                return;
            }
        }
        final ArrayList<BasePreferenceFragment> fragments = new ArrayList<>();
        fragments.add(new PreferenceServicesFragment().setIcon(R.drawable.settings_cloud));
        fragments.add(new PreferenceAppearanceFragment().setIcon(R.drawable.settings_eye));
        fragments.add(new PreferenceCachedetailsFragment().setIcon(R.drawable.settings_details));
        fragments.add(new PreferenceMapFragment().setIcon(R.drawable.settings_map));
        fragments.add(new PreferenceLoggingFragment().setIcon(R.drawable.settings_pen));
        fragments.add(new PreferenceOfflinedataFragment().setIcon(R.drawable.settings_sdcard));
        fragments.add(new PreferenceNavigationFragment().setIcon(R.drawable.settings_arrow));
        fragments.add(new PreferenceSystemFragment().setIcon(R.drawable.settings_nut));
        fragments.add(new PreferenceBackupFragment().setIcon(R.drawable.settings_backup));

        for (BasePreferenceFragment f : fragments) {
            f.setSearchdataCallback(this::collectSearchdataCallback);
        }
        final FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        for (Fragment f : fragments) {
            t.add(R.id.settings_fragment_root, f);
        }
        t.commit();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_root, new PreferencesFragment())
                .commit();
    }

    // callback for BasePreferenceFragments to register search data
    private void collectSearchdataCallback(final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> data) {
        synchronized (searchIndex) {
            searchIndex.addAll(data);
        }
    }

    // callback for BasePreferenceFragments for scrolling to a specific pref
    private void scrollToCallback(final String baseKey, final String prefKey) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            final PreferenceScreen prefScreen = ((PreferenceFragmentCompat) fragment).getPreferenceScreen();
            if (prefScreen != null && StringUtils.equalsIgnoreCase(prefScreen.getKey(), baseKey)) {
                final Preference pref = prefScreen.findPreference(prefKey);
                if (pref != null) {
                    ((PreferenceFragmentCompat) fragment).scrollToPreference(pref);
                    pref.setTitle(prepareDisplayString(this, (String) pref.getTitle(), (String) pref.getTitle()));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.settings_activity_options, menu);

        // prepare search in action bar
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem menuSearch = menu.findItem(R.id.menu_gosearch);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSuggestionsAdapter(new SettingsSuggestionsAdapter(this, searchIndex));

        SearchUtils.hideKeyboardOnSearchClick(searchView, menuSearch);
        SearchUtils.hideActionIconsWhenSearchIsActive(this, menu, menuSearch);
        SearchUtils.handleDropDownVisibility(this, searchView, menuSearch);

        return true;
    }

    static class SettingsSuggestionsAdapter extends BaseSuggestionsAdapter {
        private final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchdata;

        SettingsSuggestionsAdapter(final SettingsActivity context, final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchdata) {
            super(context, new SettingsSearchSuggestionCursor(), 0);
            this.searchdata = searchdata;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final TextView tv = view.findViewById(R.id.text);
            tv.setText(prepareDisplayString(context, cursor.getString(1), searchTerm));
            tv.setCompoundDrawablesWithIntrinsicBounds(cursor.getInt(5), 0, 0, 0);
            ((TextView) view.findViewById(R.id.info)).setText(prepareDisplayString(context, cursor.getString(2), searchTerm));
        }

        @Override
        protected Cursor query(@NonNull final String searchTerm) {
            final SettingsSearchSuggestionCursor resultCursor = new SettingsSearchSuggestionCursor();
            if (searchTerm.length() > 2) {
                synchronized (searchdata) {
                    for (BasePreferenceFragment.PrefSearchDescriptor item : searchdata) {
                        if (StringUtils.containsIgnoreCase(item.prefTitle, searchTerm) || StringUtils.containsIgnoreCase(item.prefSummary, searchTerm)) {
                            resultCursor.addItem(item.prefTitle, item.prefSummary, item.prefKey, item.prefCategoryIconRes);
                        }
                    }
                }
            }
            return resultCursor;
        }
    }

    static class SettingsSearchSuggestionCursor extends BaseSearchSuggestionCursor {
        public void addItem(@NonNull final CharSequence title, @NonNull final CharSequence summary, final String key, @DrawableRes final int iconRes) {
            addRow(new String[]{
                    String.valueOf(rowId),
                    (String) title,
                    (String) summary,
                    Intents.ACTION_SETTINGS,
                    key,
                    String.valueOf(iconRes)
            });
            rowId++;
        }
    }

    private static Spannable prepareDisplayString(final Context context, final String text, final String searchTerm) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        final int iPos = StringUtils.indexOfIgnoreCase(text, searchTerm);
        final Spannable s = new SpannableString(text);
        if (iPos >= 0) {
            s.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.colorAccent)), iPos, iPos + searchTerm.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.colorText)), iPos, iPos + searchTerm.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

}
