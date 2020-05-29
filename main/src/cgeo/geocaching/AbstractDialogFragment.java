package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class AbstractDialogFragment extends DialogFragment implements CacheMenuHandler.ActivityInterface, PopupMenu.OnMenuItemClickListener, MenuItem.OnMenuItemClickListener {
    public static final int RESULT_CODE_SET_TARGET = Activity.RESULT_FIRST_USER;
    public static final int REQUEST_CODE_TARGET_INFO = 1;
    protected static final String GEOCODE_ARG = "GEOCODE";
    protected static final String WAYPOINT_ARG = "WAYPOINT";
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    protected Resources res = null;
    protected String geocode;
    protected CacheDetailsCreator details;
    protected Geocache cache;
    private TextView cacheDistance = null;
    private final GeoDirHandler geoUpdate = new GeoDirHandler() {

        @Override
        public void updateGeoData(final GeoData geo) {
            try {
                if (cache != null && cache.getCoords() != null) {
                    cacheDistance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(cache.getCoords())));
                    cacheDistance.bringToFront();
                }
                onUpdateGeoData(geo);
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e);
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        setHasOptionsMenu(true);
    }

    protected void initCustomActionBar(final View v) {
        final ImageView defaultNavigationImageView = v.findViewById(R.id.defaultNavigation);
        defaultNavigationImageView.setOnLongClickListener(v12 -> {
            startDefaultNavigation2();
            return true;
        });
        defaultNavigationImageView.setOnClickListener(v1 -> navigateTo());

        final View setAsTargetView = v.findViewById(R.id.setAsTarget);
        final View setAsTargetSep = v.findViewById(R.id.setAsTargetSep);
        if (getActivity().getCallingActivity() != null) {
            setAsTargetView.setVisibility(View.VISIBLE);
            setAsTargetSep.setVisibility(View.VISIBLE);
            setAsTargetView.setOnClickListener(v13 -> setAsTarget());
        } else {
            setAsTargetView.setVisibility(View.GONE);
            setAsTargetSep.setVisibility(View.GONE);
        }

        final View overflowActionBar = v.findViewById(R.id.overflowActionBar);
        overflowActionBar.setOnClickListener(v14 -> showPopup(v14));
    }

    public final void setTitle(final CharSequence title) {
        final View view = getView();
        assert view != null;
        final TextView titleview = view.findViewById(R.id.actionbar_title);
        if (titleview != null) {
            titleview.setText(title);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        geocode = getArguments().getString(GEOCODE_ARG);
    }


    protected void showPopup(final View view) {
        final android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(getActivity(), view);
        CacheMenuHandler.addMenuItems(new MenuInflater(getActivity()), popupMenu.getMenu(), cache);
        popupMenu.setOnMenuItemClickListener(
                item -> AbstractDialogFragment.this.onMenuItemClick(item)
        );
        popupMenu.show();
    }

    protected void init() {
        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (cache == null) {
            ((AbstractActivity) getActivity()).showToast(res.getString(R.string.err_detail_cache_find));

            getActivity().finish();
            return;
        }

        geocode = cache.getGeocode();
    }

    @Override
    public void onResume() {
        super.onResume();
        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(getActivity(),
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.AbstractDialogFragment) {

                    @Override
                    public void executeAfter() {
                        resumeDisposables.add(geoUpdate.start(GeoDirHandler.UPDATE_GEODATA));
                    }
                });
        init();
    }


    @Override
    public void onPause() {
        resumeDisposables.clear();
        super.onPause();
    }

    protected final void addCacheDetails() {
        assert cache != null;
        // cache type
        final String cacheType = cache.getType().getL10n();
        final String cacheSize = cache.showSize() ? " (" + cache.getSize().getL10n() + ")" : "";
        details.add(R.string.cache_type, cacheType + cacheSize);

        details.add(R.string.cache_geocode, cache.getGeocode());
        details.addCacheState(cache);

        cacheDistance = details.addDistance(cache, cacheDistance);

        details.addDifficulty(cache);
        details.addTerrain(cache);
        details.addEventDate(cache);

        // rating
        if (cache.getRating() > 0) {
            details.addRating(cache);
        }

        // favorite count
        final int favCount = cache.getFavoritePoints();
        if (favCount >= 0) {
            final int findsCount = cache.getFindsCount();
            if (findsCount > 0) {
                details.add(R.string.cache_favorite, res.getString(R.string.favorite_count_percent, favCount, (float) (favCount * 100) / findsCount));
            } else if (!cache.isEventCache()) {
                details.add(R.string.cache_favorite, res.getString(R.string.favorite_count, favCount));
            }
        }

        // more details
        final View view = getView();
        assert view != null;
        final Button buttonMore = view.findViewById(R.id.more_details);

        buttonMore.setOnClickListener(arg0 -> {
            CacheDetailActivity.startActivity(getActivity(), geocode);
            getActivity().finish();
        });

        /* Only working combination as it seems */
        registerForContextMenu(buttonMore);
    }

    public final void showToast(final String text) {
        ActivityMixin.showToast(getActivity(), text);
    }

    /**
     * @param geo location
     */
    protected void onUpdateGeoData(final GeoData geo) {
        // do nothing by default
    }

    /**
     * Set the current popup coordinates as new navigation target on map
     */
    private void setAsTarget() {
        final Activity activity = getActivity();
        final Intent result = new Intent();
        result.putExtra(Intents.EXTRA_TARGET_INFO, getTargetInfo());
        activity.setResult(RESULT_CODE_SET_TARGET, result);
        activity.finish();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        CacheMenuHandler.addMenuItems(inflater, menu, cache);

    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        CacheMenuHandler.addMenuItems(new MenuInflater(getActivity()), menu, cache);
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem m = menu.getItem(i);
            m.setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        return onOptionsItemSelected(item);
    }


    @Override
    public boolean onMenuItemClick(final MenuItem menuItem) {
        return onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (CacheMenuHandler.onMenuItemSelected(item, this, cache)) {
            return true;
        }
        if (LoggingUI.onMenuItemSelected(item, getActivity(), cache, dialog -> init())) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            CacheMenuHandler.onPrepareOptionsMenu(menu, cache);
            LoggingUI.onPrepareOptionsMenu(menu, cache);
        } catch (final RuntimeException ignored) {
            // nothing
        }
    }


    protected abstract TargetInfo getTargetInfo();

    protected abstract void startDefaultNavigation2();

    @Override
    public void cachesAround() {
        final TargetInfo targetInfo = getTargetInfo();
        if (targetInfo == null || targetInfo.coords == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }
        CacheListActivity.startActivityCoordinates((AbstractActivity) getActivity(), targetInfo.coords, cache != null ? cache.getName() : null);
        getActivity().finish();
    }

    @Override
    public void onCancel(final DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }

    public static class TargetInfo implements Parcelable {

        public static final Parcelable.Creator<TargetInfo> CREATOR = new Parcelable.Creator<TargetInfo>() {
            @Override
            public TargetInfo createFromParcel(final Parcel in) {
                return new TargetInfo(in);
            }

            @Override
            public TargetInfo[] newArray(final int size) {
                return new TargetInfo[size];
            }
        };
        public final Geopoint coords;
        public final String geocode;

        TargetInfo(final Geopoint coords, final String geocode) {
            this.coords = coords;
            this.geocode = geocode;
        }

        public TargetInfo(final Parcel in) {
            this.coords = in.readParcelable(Geopoint.class.getClassLoader());
            this.geocode = in.readString();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeParcelable(coords, PARCELABLE_WRITE_RETURN_VALUE);
            dest.writeString(geocode);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
