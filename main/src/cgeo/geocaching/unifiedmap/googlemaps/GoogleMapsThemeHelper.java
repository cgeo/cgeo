package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;

class GoogleMapsThemeHelper {

    public enum GoogleMapsThemes {
        DEFAULT(R.string.google_maps_style_default, 0),
        NIGHT(R.string.google_maps_style_night, R.raw.googlemap_style_night),
        AUTO(R.string.google_maps_style_auto, 0),
        RETRO(R.string.google_maps_style_retro, R.raw.googlemap_style_retro),
        CONTRAST(R.string.google_maps_style_contrast, R.raw.googlemap_style_contrast);

        final int labelRes;
        final int jsonRes;

        GoogleMapsThemes(final int labelRes, final int jsonRes) {
            this.labelRes = labelRes;
            this.jsonRes = jsonRes;
        }

        @Nullable
        public MapStyleOptions getMapStyleOptions(final Context context) {
            final int jsonResId;
            if (this == AUTO) {
                jsonResId = Settings.isLightSkin(context) ? DEFAULT.jsonRes : NIGHT.jsonRes;
            } else {
                jsonResId = this.jsonRes;
            }
            if (jsonResId != 0) {
                return MapStyleOptions.loadRawResourceStyle(context, jsonResId);
            }
            return null;
        }

        @NonNull
        public static List<String> getLabels(final Context context) {
            final List<String> themeLabels = new ArrayList<>();
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                themeLabels.add(context.getResources().getString(theme.labelRes));
            }
            return themeLabels;
        }

        @NonNull
        public static GoogleMapsThemes getByName(final String themeName) {
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                if (theme.name().equals(themeName)) {
                    return theme;
                }
            }
            return DEFAULT;
        }
    }

    private GoogleMapsThemeHelper() {
        // utility class
    }

    /** Open theme selection dialog for GM map view, store result in settings */
    public static void selectTheme(final Activity activity, final GoogleMap googleMap) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.map_theme_select);

        final int selectedItem = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme()).ordinal();

        builder.setSingleChoiceItems(GoogleMapsThemes.getLabels(activity).toArray(new String[0]), selectedItem, (dialog, selection) -> {
            final GoogleMapsThemes theme = GoogleMapsThemes.values()[selection];
            Settings.setSelectedGoogleMapTheme(theme.name());
            googleMap.setMapStyle(theme.getMapStyleOptions(activity));
            dialog.cancel();
        });

        builder.show();
    }

    /** Apply selected GM theme to map (use default theme, if none is selected) */
    public static void setTheme(final Activity activity, final GoogleMap googleMap) {
        final GoogleMapsThemes theme = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme());
        googleMap.setMapStyle(theme.getMapStyleOptions(activity));
    }

}
