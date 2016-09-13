package cgeo.geocaching.maps;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class MapOptions {

    public MapMode mapMode;
    public boolean isLiveEnabled;
    public SearchResult searchResult;
    public String geocode;
    public Geopoint coords;
    public WaypointType waypointType;
    public MapState mapState;
    public String title;

    public MapOptions(final Context context, @Nullable final Bundle extras) {
        if (extras != null) {
            mapMode = (MapMode) extras.get(Intents.EXTRA_MAP_MODE);
            isLiveEnabled = extras.getBoolean(Intents.EXTRA_LIVE_ENABLED, false);
            searchResult = extras.getParcelable(Intents.EXTRA_SEARCH);
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            coords = extras.getParcelable(Intents.EXTRA_COORDS);
            waypointType = WaypointType.findById(extras.getString(Intents.EXTRA_WPTTYPE));
            mapState = extras.getParcelable(Intents.EXTRA_MAPSTATE);
            title = extras.getString(Intents.EXTRA_TITLE);
        } else {
            mapMode = MapMode.LIVE;
            isLiveEnabled = Settings.isLiveMap();
        }
        if (StringUtils.isBlank(title)) {
            title = context.getString(R.string.map_map);
        }
    }

    public MapOptions(final SearchResult search, final String title) {
        this.searchResult = search;
        this.title = title;
        this.mapMode = MapMode.LIST;
        this.isLiveEnabled = false;
    }

    public MapOptions() {
        mapMode = MapMode.LIVE;
        isLiveEnabled = Settings.isLiveMap();
    }

    public MapOptions(final Geopoint coords, final WaypointType type, final String title) {
        this.coords = coords;
        this.waypointType = type;
        this.title = title;
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
        intent.putExtra(Intents.EXTRA_SEARCH, searchResult);
        intent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        intent.putExtra(Intents.EXTRA_COORDS, coords);
        intent.putExtra(Intents.EXTRA_WPTTYPE, waypointType);
        intent.putExtra(Intents.EXTRA_MAPSTATE, mapState);
        intent.putExtra(Intents.EXTRA_TITLE, title);
        return intent;
    }

    public void startIntent(final Activity fromActivity, final Class<?> cls) {
        fromActivity.startActivity(newIntent(fromActivity, cls));
    }

}
