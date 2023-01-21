package cgeo.geocaching.maps;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class MapOptions {

    public MapMode mapMode;
    public boolean isLiveEnabled;
    public boolean isStoredEnabled;
    public SearchResult searchResult;
    public String geocode;
    public Geopoint coords;
    public WaypointType waypointType;
    public MapState mapState;
    public String title;
    public int fromList;
    public GeocacheFilterContext filterContext = new GeocacheFilterContext(LIVE);

    public MapOptions(final Context context, @Nullable final Bundle extras) {
        if (extras != null) {
            mapMode = (MapMode) extras.get(Intents.EXTRA_MAP_MODE);
            isLiveEnabled = extras.getBoolean(Intents.EXTRA_LIVE_ENABLED, false);
            isStoredEnabled = extras.getBoolean(Intents.EXTRA_STORED_ENABLED, false);
            searchResult = extras.getParcelable(Intents.EXTRA_SEARCH);
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            coords = extras.getParcelable(Intents.EXTRA_COORDS);
            waypointType = (WaypointType) extras.get(Intents.EXTRA_WPTTYPE);
            mapState = extras.getParcelable(Intents.EXTRA_MAPSTATE);
            title = extras.getString(Intents.EXTRA_TITLE);
            if (null != coords && null == waypointType) {
                waypointType = WaypointType.WAYPOINT;
            }
            fromList = extras.getInt(Intents.EXTRA_LIST_ID, StoredList.TEMPORARY_LIST.id);
            filterContext = extras.getParcelable(Intents.EXTRA_FILTER_CONTEXT);
        } else {
            mapMode = MapMode.LIVE;
            isStoredEnabled = true;
            isLiveEnabled = Settings.isLiveMap();
        }
        if (StringUtils.isBlank(title)) {
            title = context.getString(R.string.map_offline);
        }
    }

    public MapOptions(final SearchResult search, final String title, final int fromList) {
        this.searchResult = search;
        this.title = title;
        this.mapMode = MapMode.LIST;
        this.isLiveEnabled = false;
        this.fromList = fromList;
    }

    public MapOptions() {
        mapMode = MapMode.LIVE;
        isStoredEnabled = true;
        isLiveEnabled = Settings.isLiveMap();
    }

    public MapOptions(final Geopoint coords) {
        mapMode = MapMode.LIVE;
        this.coords = coords;
        this.waypointType = WaypointType.WAYPOINT;
        isStoredEnabled = true;
        isLiveEnabled = Settings.isLiveMap();
    }

    public MapOptions(final Geopoint coords, final WaypointType type) {
        this.coords = coords;
        this.waypointType = type;
        mapMode = MapMode.COORDS;
        isLiveEnabled = false;
    }

    public MapOptions(final Geopoint coords, final WaypointType type, final String title, final String geocode) {
        this.coords = coords;
        this.waypointType = type;
        this.title = title;
        this.geocode = geocode;
        mapMode = MapMode.COORDS;
        isLiveEnabled = false;
    }

    public MapOptions(final String geocode) {
        this.geocode = geocode;
        this.mapMode = MapMode.SINGLE;
        this.isLiveEnabled = false;
    }

    public Intent newIntent(final Context context, final Class<?> cls) {
        final Intent intent = new Intent(context, cls);
        intent.putExtra(Intents.EXTRA_MAP_MODE, mapMode);
        intent.putExtra(Intents.EXTRA_LIVE_ENABLED, isLiveEnabled);
        intent.putExtra(Intents.EXTRA_STORED_ENABLED, isStoredEnabled);
        intent.putExtra(Intents.EXTRA_SEARCH, searchResult);
        intent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        intent.putExtra(Intents.EXTRA_COORDS, coords);
        intent.putExtra(Intents.EXTRA_WPTTYPE, waypointType);
        intent.putExtra(Intents.EXTRA_MAPSTATE, mapState);
        intent.putExtra(Intents.EXTRA_TITLE, title);
        intent.putExtra(Intents.EXTRA_LIST_ID, fromList);
        intent.putExtra(Intents.EXTRA_FILTER_CONTEXT, filterContext);
        return intent;
    }

    public void startIntent(final Context fromActivity, final Class<?> cls) {
        fromActivity.startActivity(newIntent(fromActivity, cls));
    }

    public void startIntentWithoutTransition(final Activity fromActivity, final Class<?> cls) {
        startIntent(fromActivity, cls);

        // avoid weired transitions
        ActivityMixin.overrideTransitionToFade(fromActivity);
    }
}
