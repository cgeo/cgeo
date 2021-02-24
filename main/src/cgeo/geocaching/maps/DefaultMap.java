package cgeo.geocaching.maps;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public final class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    private static Class<?> getDefaultMapClass() {
        return Settings.getMapProvider().getMapClass();
    }

    public static Intent getLiveMapIntent(final Activity fromActivity, final Class<?> cls) {
        return new MapOptions().newIntent(fromActivity, cls);
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return getLiveMapIntent(fromActivity, getDefaultMapClass());
    }

    @SuppressWarnings("unused")
    public static Intent getLiveMapIntent(final Activity fromActivity, final Geopoint coords) {
        return new MapOptions(coords).newIntent(fromActivity, getDefaultMapClass());
    }

    public static void startActivityCoords(final Context fromActivity, final Class<?> cls, final Geopoint coords, final WaypointType type, final String title) {
        new MapOptions(coords, type, title).startIntent(fromActivity, cls);
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        startActivityCoords(fromActivity, getDefaultMapClass(), coords, type, title);
    }

    public static void startActivityGeoCode(final Context fromActivity, final Geopoint coords) {
        new MapOptions(coords).startIntent(fromActivity, getDefaultMapClass());
    }

    public static void startActivityGeoCode(final Context fromActivity, final Class<?> cls, final String geocode) {
        new MapOptions(geocode).startIntent(fromActivity, cls);
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        startActivityGeoCode(fromActivity, getDefaultMapClass(), geocode);
    }

    public static void startActivitySearch(final Activity fromActivity, final Class<?> cls, @NonNull final SearchResult search, final String title) {
        new MapOptions(search, title).startIntent(fromActivity, cls);
    }

    public static void startActivitySearch(final Activity fromActivity, @NonNull final SearchResult search, final String title) {
        startActivitySearch(fromActivity, getDefaultMapClass(), search, title);
    }
}
