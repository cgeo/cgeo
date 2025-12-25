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

package cgeo.geocaching.settings

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractNavigationBarActivity
import cgeo.geocaching.activity.CustomMenuEntryActivity
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper
import cgeo.geocaching.search.BaseSearchSuggestionCursor
import cgeo.geocaching.search.BaseSuggestionsAdapter
import cgeo.geocaching.search.SearchUtils
import cgeo.geocaching.settings.fragments.BasePreferenceFragment
import cgeo.geocaching.settings.fragments.PreferenceAppearanceFragment
import cgeo.geocaching.settings.fragments.PreferenceBackupFragment
import cgeo.geocaching.settings.fragments.PreferenceCachedetailsFragment
import cgeo.geocaching.settings.fragments.PreferenceLoggingFragment
import cgeo.geocaching.settings.fragments.PreferenceMapContentBehaviorFragment
import cgeo.geocaching.settings.fragments.PreferenceMapSourcesFragment
import cgeo.geocaching.settings.fragments.PreferenceNavigationFragment
import cgeo.geocaching.settings.fragments.PreferenceOfflinedataFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceExtremcachingComFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceGeocachingComAdventureLabsFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceGeocachingComFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceGeocachingSuFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceGeokretyOrgFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencacheUkFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencachingDeFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencachingNlFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencachingPlFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencachingRoFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceOpencachingUsFragment
import cgeo.geocaching.settings.fragments.PreferenceServiceSendToCgeoFragment
import cgeo.geocaching.settings.fragments.PreferenceServicesFragment
import cgeo.geocaching.settings.fragments.PreferenceSystemFragment
import cgeo.geocaching.settings.fragments.PreferencesFragmentRoot
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.PersistableUri
import cgeo.geocaching.utils.BackupUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SettingsUtils.initPublicFolders

import android.app.SearchManager
import android.app.backup.BackupManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

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
class SettingsActivity : CustomMenuEntryActivity() : PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static val TITLE_TAG: String = "preferencesActivityTitle"
    private static val INTENT_OPEN_SCREEN: String = "OPEN_SCREEN"
    public static val NO_RESTART_NEEDED: Int = 1
    public static val RESTART_NEEDED: Int = 2

    public static val STATE_CSAH: String = "csah"
    public static val STATE_BACKUPUTILS: String = "backuputils"

    private var backupUtils: BackupUtils = null
    private var contentStorageHelper: ContentStorageActivityHelper = null

    private CharSequence title
    private var preferenceContentView: Int = R.id.settings_fragment_root
    private var delayedOpenPreference: String = null

    private static val searchIndex: ArrayList<BasePreferenceFragment.PrefSearchDescriptor> = ArrayList<>()

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        backupUtils = BackupUtils(SettingsActivity.this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS))

        this.contentStorageHelper = ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, folder -> {
                    val fragments: List<Fragment> = getSupportFragmentManager().getFragments()
                    for (Fragment f : fragments) {
                        if (f is PreferenceFragmentCompat) {
                            initPublicFolders((PreferenceFragmentCompat) f, contentStorageHelper)
                        }
                    }
                    if (PersistableFolder.OFFLINE_MAP_THEMES == (folder)) {
                        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder()
                    }
                })
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, file -> {
                    val fragments: List<Fragment> = getSupportFragmentManager().getFragments()
                    for (Fragment f : fragments) {
                        if (f is PreferenceMapContentBehaviorFragment) {
                            ((PreferenceMapContentBehaviorFragment) f).updateNotificationAudioInfo()
                        }
                    }
                })

        setContentView(R.layout.layout_settings)
        if (findViewById(R.id.settings_fragment_content_root) != null) {
            // landscape mode?
            preferenceContentView = R.id.settings_fragment_content_root
        }
        buildSearchIndex()

        handleIntent(savedInstanceState)
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.settings_titlebar)
            }
        })
        getSupportActionBar().setDisplayHomeAsUpEnabled(true)

        setResult(NO_RESTART_NEEDED)
    }

    private Boolean isInDualPaneMode() {
        return preferenceContentView == R.id.settings_fragment_content_root
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home && getSupportFragmentManager().popBackStackImmediate()) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override     public Unit onBackPressed() {
        if (!getSupportFragmentManager().popBackStackImmediate()) {
            super.onBackPressed()
        }
    }

    public BackupUtils getBackupUtils() {
        return backupUtils
    }

    private Unit handleIntent(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            val intent: Intent = getIntent()
            val action: String = intent.getAction()
            Boolean found = false
            if (Intents.ACTION_SETTINGS == (action)) {
                // user selected a search suggestion
                openRequestedFragment(intent.getStringExtra(SearchManager.QUERY))
                found = true
            } else if (Intent.ACTION_SEARCH == (action)) {
                // user pressed enter in searchfield => search first pref matching this search string
                val query: String = intent.getStringExtra(SearchManager.QUERY)
                synchronized (searchIndex) {
                    for (BasePreferenceFragment.PrefSearchDescriptor item : searchIndex) {
                        if (StringUtils.containsIgnoreCase(item.prefTitle, query) || StringUtils.containsIgnoreCase(item.prefSummary, query)) {
                            openRequestedFragment(item.baseKey, item.prefKey)
                            found = true
                            break
                        }
                    }
                }
            } else {
                // Uri starting with "cgeo-setting://" + prefKey value
                val data: String = intent.getDataString()
                val scheme: String = getString(R.string.settings_scheme) + "://"
                if (data != null && data.startsWith(scheme)) {
                    val prefKey: String = data.substring(scheme.length()).replaceAll("[^A-Za-z0-9_]+", " ").trim()
                    if (StringUtils.isNotBlank(prefKey)) {
                        Log.d("external link to: cgeo-settings://" + prefKey)
                        openRequestedFragment(prefKey)
                        found = true
                    }
                }
            }
            if (!found) {
                openRequestedFragment("")
            }
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
    }

    /**
     * This method sets the fragment which is used upon opening the settings. This may be the user directly or a
     * requesting Intent.
     */
    private Unit openRequestedFragment(final String preference) {
        val intent: Intent = getIntent()
        val fragmentId: Int = intent.getIntExtra(INTENT_OPEN_SCREEN, -1)
        String fragment = ""
        // try to get fragment key from preference name (if necessary)
        if (fragmentId < 0 && StringUtils.isNotBlank(preference)) {
            synchronized (searchIndex) {
                for (BasePreferenceFragment.PrefSearchDescriptor pref : searchIndex) {
                    if (StringUtils == (preference, pref.prefKey)) {
                        fragment = pref.baseKey
                        break
                    }
                }
                if (StringUtils.isBlank(fragment)) {
                    delayedOpenPreference = preference
                    return
                }
            }
        } else {
            try {
                fragment = getString(fragmentId)
            } catch (Exception ignore) {
                fragment = ""
            }

        }
        openRequestedFragment(fragment, preference)
    }

    /**
     * This method sets the fragment given by its base key,
     * and optionally starts scrolling to a preference identified by its key
     */
    private Unit openRequestedFragment(final String baseKey, final String scrollToPrefKey) {
        if (isInDualPaneMode() && StringUtils.isBlank(baseKey)) {
            // never open category view in content pane when in dual pane mode
            return
        }
        Fragment preferenceFragment = PreferencesFragmentRoot()

        // main configuration screens
        if (StringUtils == (baseKey, getString(R.string.preference_screen_services))) {
            preferenceFragment = PreferenceServicesFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_appearance))) {
            preferenceFragment = PreferenceAppearanceFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_cachedetails))) {
            preferenceFragment = PreferenceCachedetailsFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_map_sources))) {
            preferenceFragment = PreferenceMapSourcesFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_map_content_behavior))) {
            preferenceFragment = PreferenceMapContentBehaviorFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_logging))) {
            preferenceFragment = PreferenceLoggingFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_offlinedata))) {
            preferenceFragment = PreferenceOfflinedataFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_navigation))) {
            preferenceFragment = PreferenceNavigationFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_system))) {
            preferenceFragment = PreferenceSystemFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_backup))) {
            preferenceFragment = PreferenceBackupFragment()

        // service configuration screens
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_sendtocgeo))) {
            preferenceFragment = PreferenceServiceSendToCgeoFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_geokrety))) {
            preferenceFragment = PreferenceServiceGeokretyOrgFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_gc))) {
            preferenceFragment = PreferenceServiceGeocachingComFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocde))) {
            preferenceFragment = PreferenceServiceOpencachingDeFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocuk))) {
            preferenceFragment = PreferenceServiceOpencacheUkFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocnl))) {
            preferenceFragment = PreferenceServiceOpencachingNlFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocpl))) {
            preferenceFragment = PreferenceServiceOpencachingPlFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocus))) {
            preferenceFragment = PreferenceServiceOpencachingUsFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ocro))) {
            preferenceFragment = PreferenceServiceOpencachingRoFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_al))) {
            preferenceFragment = PreferenceServiceGeocachingComAdventureLabsFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_ec))) {
            preferenceFragment = PreferenceServiceExtremcachingComFragment()
        } else if (StringUtils == (baseKey, getString(R.string.preference_screen_su))) {
            preferenceFragment = PreferenceServiceGeocachingSuFragment()
        }

        if (StringUtils.isNotBlank(scrollToPrefKey)) {
            ((BasePreferenceFragment) preferenceFragment).setScrollToPrefCallback(this::scrollToCallback, baseKey, scrollToPrefKey)
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(preferenceContentView, preferenceFragment)
                .commit()
    }

    override     public Unit onSaveInstanceState(final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBundle(STATE_CSAH, contentStorageHelper.getState())
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState())

        // Save current activity title so we can set it again after a configuration change
        savedInstanceState.putCharSequence(TITLE_TAG, title)
    }

    override     public Unit onPause() {
        Log.i("Requesting settings backup with settings manager")
        BackupManager.dataChanged(getPackageName())
        super.onPause()
    }

    public static Unit openForScreen(final Int preferenceScreenKey, final Context fromActivity) {
        val intent: Intent = getOpenForScreenIntent(preferenceScreenKey, fromActivity)
        fromActivity.startActivity(intent)
    }

    public static Unit openForScreen(final Int preferenceScreenKey, final Context fromActivity, final Boolean hideBottomNavigation) {
        val intent: Intent = getOpenForScreenIntent(preferenceScreenKey, fromActivity)
        AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideBottomNavigation)
        fromActivity.startActivity(intent)
    }

    private static Intent getOpenForScreenIntent(final Int preferenceScreenKey, final Context fromActivity) {
        val intent: Intent = Intent(fromActivity, SettingsActivity.class)
        intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey)
        return intent
    }

    public static Unit openForSettingsLink(final Uri uri, final Context fromActivity) {
        val intent: Intent = Intent(Intent.ACTION_VIEW, uri, fromActivity, SettingsActivity.class)
        fromActivity.startActivity(intent)
    }

    override     public Boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }


    override     public Boolean onPreferenceStartFragment(final PreferenceFragmentCompat caller, final Preference pref) {
        // clear fragment backstack if base category opened and in dualmode
        if (isInDualPaneMode() && caller is PreferencesFragmentRoot) {
            while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate()
            }
        }
        // Instantiate the Fragment
        val args: Bundle = pref.getExtras()
        val fragment: Fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment())
        fragment.setArguments(args)
        fragment.setTargetFragment(caller, 0)
        // Replace the existing Fragment with the Fragment
        getSupportFragmentManager().beginTransaction()
            .replace(preferenceContentView, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.getTitle()
        return true
    }

    public ContentStorageActivityHelper getCsah() {
        return contentStorageHelper
    }

    // to be called by PreferenceMapContentBehaviorFragment
    public Unit startProximityNotificationSelector(final Boolean first) {
        contentStorageHelper.selectPersistableUri(first ? PersistableUri.PROXIMITY_NOTIFICATION_FAR : PersistableUri.PROXIMITY_NOTIFICATION_CLOSE)
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!contentStorageHelper.onActivityResult(requestCode, resultCode, data)) {
            backupUtils.onActivityResult(requestCode, resultCode, data)
        }
    }

    // search related extensions

    private Unit buildSearchIndex() {
        synchronized (searchIndex) {
            if (!searchIndex.isEmpty()) {
                return
            }
        }
        val fragments: ArrayList<BasePreferenceFragment> = ArrayList<>()
        fragments.add(PreferenceServicesFragment().setIcon(R.drawable.settings_cloud))
        fragments.add(PreferenceAppearanceFragment().setIcon(R.drawable.settings_eye))
        fragments.add(PreferenceCachedetailsFragment().setIcon(R.drawable.settings_details))
        fragments.add(PreferenceMapSourcesFragment().setIcon(R.drawable.settings_map))
        fragments.add(PreferenceMapContentBehaviorFragment().setIcon(R.drawable.settings_map_content))
        fragments.add(PreferenceLoggingFragment().setIcon(R.drawable.settings_pen))
        fragments.add(PreferenceOfflinedataFragment().setIcon(R.drawable.settings_sdcard))
        fragments.add(PreferenceNavigationFragment().setIcon(R.drawable.settings_arrow))
        fragments.add(PreferenceSystemFragment().setIcon(R.drawable.settings_nut))
        fragments.add(PreferenceBackupFragment().setIcon(R.drawable.settings_backup))

        for (BasePreferenceFragment f : fragments) {
            f.setSearchdataCallback(this::collectSearchdataCallback)
        }
        val t: FragmentTransaction = getSupportFragmentManager().beginTransaction()
        for (Fragment f : fragments) {
            t.add(preferenceContentView, f)
        }
        t.commit()

        // in dual column mode open services section on start
        if (isInDualPaneMode()) {
            openRequestedFragment(getString(R.string.preference_screen_services), null)
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(preferenceContentView, PreferencesFragmentRoot())
                    .commit()
        }
    }

    // callback for BasePreferenceFragments to register search data
    private Unit collectSearchdataCallback(final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> data) {
        synchronized (searchIndex) {
            searchIndex.addAll(data)
            if (StringUtils.isNotBlank(delayedOpenPreference)) {
                for (BasePreferenceFragment.PrefSearchDescriptor pref : data) {
                    if (StringUtils == (delayedOpenPreference, pref.prefKey)) {
                        openRequestedFragment(pref.baseKey, delayedOpenPreference)
                        delayedOpenPreference = null
                        break
                    }
                }
            }
        }
    }

    // callback for BasePreferenceFragments for scrolling to a specific pref
    private Unit scrollToCallback(final String baseKey, final String prefKey) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            val prefScreen: PreferenceScreen = ((PreferenceFragmentCompat) fragment).getPreferenceScreen()
            if (prefScreen != null && StringUtils.equalsIgnoreCase(prefScreen.getKey(), baseKey)) {
                val pref: Preference = prefScreen.findPreference(prefKey)
                if (pref != null) {
                    ((PreferenceFragmentCompat) fragment).scrollToPreference(pref)
                    pref.setTitle(prepareDisplayString(this, (String) pref.getTitle(), (String) pref.getTitle()))
                }
            }
        }
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.settings_activity_options, menu)

        // prepare search in action bar
        val searchManager: SearchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE)
        val menuSearch: MenuItem = menu.findItem(R.id.menu_gosearch)
        val searchView: SearchView = (SearchView) menuSearch.getActionView()
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()))
        searchView.setSuggestionsAdapter(SettingsSuggestionsAdapter(this, searchIndex))

        SearchUtils.hideKeyboardOnSearchClick(searchView, menuSearch)
        SearchUtils.hideActionIconsWhenSearchIsActive(this, menu, menuSearch)
        SearchUtils.handleDropDownVisibility(this, searchView, menuSearch)
        SearchUtils.setSearchViewColor(searchView)

        return true
    }

    static class SettingsSuggestionsAdapter : BaseSuggestionsAdapter() {
        private final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchdata

        SettingsSuggestionsAdapter(final SettingsActivity context, final ArrayList<BasePreferenceFragment.PrefSearchDescriptor> searchdata) {
            super(context, SettingsSearchSuggestionCursor(), 0)
            this.searchdata = searchdata
        }

        override         public Unit bindView(final View view, final Context context, final Cursor cursor) {
            val tv: TextView = view.findViewById(R.id.text)
            tv.setText(prepareDisplayString(context, cursor.getString(1), searchTerm))
            tv.setCompoundDrawablesWithIntrinsicBounds(cursor.getInt(5), 0, 0, 0)
            ((TextView) view.findViewById(R.id.info)).setText(prepareDisplayString(context, cursor.getString(2), searchTerm))
        }

        override         protected Cursor query(final String searchTerm) {
            val showExtended: Boolean = Settings.extendedSettingsAreEnabled()
            val resultCursor: SettingsSearchSuggestionCursor = SettingsSearchSuggestionCursor()
            if (searchTerm.length() > 2) {
                synchronized (searchdata) {
                    for (BasePreferenceFragment.PrefSearchDescriptor item : searchdata) {
                        if ((StringUtils.containsIgnoreCase(item.prefTitle, searchTerm) || StringUtils.containsIgnoreCase(item.prefSummary, searchTerm)) && (showExtended || item.isBasicSetting)) {
                            resultCursor.addItem(item.prefTitle, item.prefSummary, item.prefKey, item.prefCategoryIconRes)
                        }
                    }
                }
            }
            return resultCursor
        }
    }

    static class SettingsSearchSuggestionCursor : BaseSearchSuggestionCursor() {
        public Unit addItem(final CharSequence title, final CharSequence summary, final String key, @DrawableRes final Int iconRes) {
            addRow(String[]{
                    String.valueOf(rowId),
                    (String) title,
                    (String) summary,
                    Intents.ACTION_SETTINGS,
                    key,
                    String.valueOf(iconRes)
            })
            rowId++
        }
    }

    private static Spannable prepareDisplayString(final Context context, final String text, final String searchTerm) {
        if (StringUtils.isBlank(text)) {
            return null
        }
        val iPos: Int = StringUtils.indexOfIgnoreCase(text, searchTerm)
        val s: Spannable = SpannableString(text)
        if (iPos >= 0) {
            s.setSpan(BackgroundColorSpan(context.getResources().getColor(R.color.colorAccent)), iPos, iPos + searchTerm.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            s.setSpan(ForegroundColorSpan(context.getResources().getColor(R.color.colorText)), iPos, iPos + searchTerm.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return s
    }

    /** for theme settings, don't show second column in landscape mode */
    public static Unit hideRightColumnInLandscapeMode(final AppCompatActivity activity) {
        val rightColumn: Fragment = activity.getSupportFragmentManager().findFragmentById(R.id.settings_fragment_content_root)
        if (rightColumn != null) {
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .remove(rightColumn)
                    .commit()
            activity.findViewById(R.id.settings_fragment_divider).setVisibility(View.GONE)
            activity.findViewById(R.id.settings_fragment_content_root).setVisibility(View.GONE)
        }

    }

}
