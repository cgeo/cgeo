package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.network.AndroidBeam;
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

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.text.HtmlCompat;
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

    private SearchView searchView = null;
    private MenuItem menuSearch = null;
    private CharSequence lastTitle = null;
    private Preference lastResult = null;
    private final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchdata = new ArrayList<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this);
        setTheme(Settings.isLightSkin(this) ? R.style.settings_light : R.style.settings);
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

        if (savedInstanceState == null) {
            openRequestedFragment();
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG);
        }
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.settings_titlebar);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AndroidBeam.disable(this);

        setResult(NO_RESTART_NEEDED);

        initFragmentsForSearch();
    }

    /**
     * This method sets the fragment which is used upon opening the settings. This may be the user directly or a
     * requesting Intent.
     */
    private void openRequestedFragment() {
        final Intent intent = getIntent();
        final int fragmentId = intent.getIntExtra(INTENT_OPEN_SCREEN, -1);
        // openRequestedFragment(fragmentId);
        try {
            openRequestedFragment(getString(fragmentId), "");
        } catch (Exception ignore) {
            openRequestedFragment("", "");
        }
    }

    /**
     * This method sets the fragment given by its base key,
     * and optionally starts scrolling to a preference identified by its key
     */
    private void openRequestedFragment(@NonNull final String baseKey, @Nullable final String scrollToPrefKey) {
        Fragment preferenceFragment = new PreferencesFragment();
        if (StringUtils.equals(baseKey, getString(R.string.preference_screen_services))) {
            preferenceFragment = new PreferenceServicesFragment();
        } else if (StringUtils.equals(baseKey, getString(R.string.pref_appearance))) {
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
        /* @todo - still needed here?
        if (backupUtils.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        */

    }

    @Override
    public void onBackPressed() {
        // back may exit the app instead of closing the search action bar
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            menuSearch.collapseActionView();
        } else {
            super.onBackPressed();
        }
    }

    // search related extensions

    private void initFragmentsForSearch() {
        final ArrayList<BasePreferenceFragment> fragments = new ArrayList<>();
        fragments.add(new PreferenceServicesFragment());
        fragments.add(new PreferenceAppearanceFragment());
        fragments.add(new PreferenceCachedetailsFragment());
        fragments.add(new PreferenceMapFragment());
        fragments.add(new PreferenceOfflinedataFragment());
        fragments.add(new PreferenceNavigationFragment());
        fragments.add(new PreferenceSystemFragment());
        fragments.add(new PreferenceBackupFragment());

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
        synchronized (searchdata) {
            searchdata.addAll(data);
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
                    lastResult = pref;
                    lastTitle = pref.getTitle();
                    pref.setTitle(HtmlCompat.fromHtml("<font color=#00eeee>" + lastTitle + "</font>", 0));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.settings_activity_options, menu);

        // prepare search in action bar
        menuSearch = menu.findItem(R.id.menu_gosearch);
        searchView = (SearchView) menuSearch.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String s) {
                if (lastResult != null) {
                    lastResult.setTitle(lastTitle);
                }
                if (s.length() > 2) {
                    synchronized (searchdata) {
                        for (BasePreferenceFragment.PrefSearchDescriptor desc : searchdata) {
                            if (StringUtils.containsIgnoreCase(desc.prefTitle, s) || StringUtils.containsIgnoreCase(desc.prefSummary, s)) {
                                openRequestedFragment(desc.baseKey, desc.prefKey);
                                break;
                            }
                        }
                    }
                }
                return true;
            }
        });

        return true;
    }

}
