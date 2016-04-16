package cgeo.geocaching.maps;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Intent;

public final class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        if (Settings.useNewMapAsDefault()) {
            return NewMap.getLiveMapIntent(fromActivity);
        }
        return CGeoMap.getLiveMapIntent(fromActivity);
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        if (Settings.useNewMapAsDefault()) {
            NewMap.startActivityCoords(fromActivity, coords, type, title);
        } else {
            CGeoMap.startActivityCoords(fromActivity, coords, type, title);
        }
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        if (Settings.useNewMapAsDefault()) {
            NewMap.startActivityGeoCode(fromActivity, geocode);
        } else {
            CGeoMap.startActivityGeoCode(fromActivity, geocode);
        }
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title) {
        if (Settings.useNewMapAsDefault()) {
            NewMap.startActivitySearch(fromActivity, search, title);
        } else {
            CGeoMap.startActivitySearch(fromActivity, search, title);
        }
    }

}
