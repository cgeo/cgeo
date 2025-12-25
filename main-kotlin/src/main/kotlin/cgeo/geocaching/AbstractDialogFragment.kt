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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.INavigationSource
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.log.LoggingUI
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.CacheDetailsCreator
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class AbstractDialogFragment : Fragment() : CacheMenuHandler.ActivityInterface, INavigationSource {
    public static val RESULT_CODE_SET_TARGET: Int = Activity.RESULT_FIRST_USER
    public static val REQUEST_CODE_TARGET_INFO: Int = 1
    protected static val GEOCODE_ARG: String = "GEOCODE"
    protected static val WAYPOINT_ARG: String = "WAYPOINT"
    private val resumeDisposables: CompositeDisposable = CompositeDisposable()
    protected var res: Resources = null
    protected String geocode
    protected CacheDetailsCreator details
    protected Geocache cache
    private var cacheDistance: TextView = null
    private val geoUpdate: GeoDirHandler = GeoDirHandler() {

        override         public Unit updateGeoData(final GeoData geo) {
            try {
                if (cacheDistance != null && cache != null && cache.getCoords() != null) {
                    cacheDistance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(cache.getCoords())))
                    cacheDistance.bringToFront()
                }
                onUpdateGeoData(geo)
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e)
            }
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        res = getResources()
        setHasOptionsMenu(true)
    }

    override     public Unit onStart() {
        super.onStart()
        geocode = getArguments().getString(GEOCODE_ARG)
    }

    protected Unit init() {
        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)

        if (cache == null) {
            ((AbstractActivity) requireActivity()).showToast(res.getString(R.string.err_detail_cache_find))

            ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment()
            return
        }

        geocode = cache.getGeocode()
    }

    override     public Unit onResume() {
        super.onResume()
        // resume location access
        resumeDisposables.add(geoUpdate.start(GeoDirHandler.UPDATE_GEODATA))
        init()
    }


    override     public Unit onPause() {
        resumeDisposables.clear()
        super.onPause()
    }

    protected final Unit addCacheDetails(final Boolean showGeocode) {
        assert cache != null

        // cache type
        val cacheType: String = cache.getType().getL10n()
        val cacheSize: String = cache.showSize() ? " (" + cache.getSize().getL10n() + ")" : ""
        details.add(R.string.cache_type, cacheType + cacheSize)

        if (showGeocode) {
            details.add(R.string.cache_geocode, cache.getShortGeocode())
        }
        details.addCacheState(cache)

        cacheDistance = details.addDistance(cache, cacheDistance)

        details.addDifficultyTerrain(cache)
        details.addEventDate(cache)

        // rating
        if (cache.getRating() > 0) {
            details.addRating(cache)
        }

        // favorite count
        val favCount: Int = cache.getFavoritePoints()
        if (favCount >= 0) {
            val findsCount: Int = cache.getFindsCount()
            if (findsCount > 0) {
                details.add(R.string.cache_favorite, res.getString(R.string.favorite_count_percent, favCount, (Float) (favCount * 100) / findsCount))
            } else if (!cache.isEventCache()) {
                details.add(R.string.cache_favorite, res.getString(R.string.favorite_count, favCount))
            }
        }

        details.addBetterCacher(cache)
        details.addCoordinates(cache.getCoords())

        // Latest logs
        details.addLatestLogs(cache)

        // more details
        val view: View = getView()
        assert view != null
        val buttonMore: Button = view.findViewById(R.id.more_details)

        buttonMore.setOnClickListener(arg0 -> {
            CacheDetailActivity.startActivity(getActivity(), geocode)
            ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment()
        })

        /* Only working combination as it seems */
        registerForContextMenu(buttonMore)
    }

    public final Unit showToast(final String text) {
        ActivityMixin.showToast(getActivity(), text)
    }

    /**
     * @param geo location
     */
    protected Unit onUpdateGeoData(final GeoData geo) {
        // do nothing by default
    }

    /**
     * Set the current popup coordinates as navigation target on map
     */
    private Unit setAsTarget() {
        val activity: TargetUpdateReceiver = (TargetUpdateReceiver) requireActivity()
        activity.onReceiveTargetUpdate(getTargetInfo())
        ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment()
    }

    public static Unit onCreatePopupOptionsMenu(final Toolbar toolbar, final INavigationSource navigationSource, final Geocache geocache) {
        val menu: Menu = toolbar.getMenu()
        menu.clear()
        toolbar.inflateMenu(R.menu.cache_options)
        CacheMenuHandler.onPrepareOptionsMenu(menu, geocache, true)
        CacheMenuHandler.initDefaultNavigationMenuItem(menu, navigationSource)
        ViewUtils.extendMenuActionBarDisplayItemCount(toolbar.getContext(), menu)
        menu.findItem(R.id.menu_target).setVisible(true)
        LoggingUI.onPrepareOptionsMenu(menu, geocache)
    }

    public Boolean onPopupOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_target) {
            setAsTarget()
            return true
        }

        if (CacheMenuHandler.onMenuItemSelected(item, this, cache, this::init, true)) {
            return true
        }

        return LoggingUI.onMenuItemSelected(item, getActivity(), cache, dialog -> init())
    }

    protected Unit setToolbarBackgroundColor(final Toolbar toolbar, final View swipView, final CacheType cacheType, final Boolean isEnabled) {
        if (!Settings.useColoredActionBar(toolbar.getContext())) {
            return
        }

        val actionbarColor: Int = CacheType.getActionBarColor(toolbar.getContext(), cacheType, isEnabled)
        swipView.getBackground().mutate().setTint(actionbarColor)
        toolbar.setBackgroundColor(actionbarColor)
    }

    protected abstract TargetInfo getTargetInfo()

    override     public Unit navigateTo() {
        startDefaultNavigation()
    }

    override     public Unit cachesAround() {
        val targetInfo: TargetInfo = getTargetInfo()
        if (targetInfo == null || targetInfo.coords == null) {
            showToast(res.getString(R.string.err_location_unknown))
            return
        }
        CacheListActivity.startActivityCoordinates((AbstractActivity) getActivity(), targetInfo.coords, cache != null ? cache.getName() : null)
    }

    interface TargetUpdateReceiver {
        Unit onReceiveTargetUpdate(TargetInfo targetInfo)
    }

    public static class TargetInfo : Parcelable {

        public static final Parcelable.Creator<TargetInfo> CREATOR = Parcelable.Creator<TargetInfo>() {
            override             public TargetInfo createFromParcel(final Parcel in) {
                return TargetInfo(in)
            }

            override             public TargetInfo[] newArray(final Int size) {
                return TargetInfo[size]
            }
        }
        public final Geopoint coords
        public final String geocode

        public TargetInfo(final Geopoint coords, final String geocode) {
            this.coords = coords
            this.geocode = geocode
        }

        public TargetInfo(final Parcel in) {
            this.coords = in.readParcelable(Geopoint.class.getClassLoader())
            this.geocode = in.readString()
        }

        override         public Unit writeToParcel(final Parcel dest, final Int flags) {
            dest.writeParcelable(coords, PARCELABLE_WRITE_RETURN_VALUE)
            dest.writeString(geocode)
        }

        override         public Int describeContents() {
            return 0
        }
    }
}
