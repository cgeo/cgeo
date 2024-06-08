package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

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
        DEFAULT(R.string.google_maps_style_default, 0, false),
        NIGHT(R.string.google_maps_style_night, R.raw.googlemap_style_night, true),
        AUTO(R.string.google_maps_style_auto, 0, false),
        RETRO(R.string.google_maps_style_retro, R.raw.googlemap_style_retro, false),
        CONTRAST(R.string.google_maps_style_contrast, R.raw.googlemap_style_contrast, false),
        DETAILS(R.string.google_maps_style_details, R.raw.googlemap_style_details, false);

        final int labelRes;
        final int jsonRes;
        final boolean needsInvertedColors; // for scale bar drawing

        GoogleMapsThemes(final int labelRes, final int jsonRes, final boolean needsInvertedColors) {
            this.labelRes = labelRes;
            this.jsonRes = jsonRes;
            this.needsInvertedColors = needsInvertedColors;
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
    public static void selectTheme(final Activity activity, final GoogleMap googleMap, final Action1<GoogleMapsThemes> onThemeSelected) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.map_theme_select);

        final int selectedItem = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme()).ordinal();

        builder.setSingleChoiceItems(GoogleMapsThemes.getLabels(activity).toArray(new String[0]), selectedItem, (dialog, selection) -> {
            final GoogleMapsThemes theme = GoogleMapsThemes.values()[selection];
            Settings.setSelectedGoogleMapTheme(theme.name());
            onThemeSelected.call(theme);
            dialog.cancel();
        });

        builder.show();
    }

    /** Apply selected GM theme to map (use default theme, if none is selected) */
    public static GoogleMapsThemes setTheme(final Activity activity, final GoogleMap googleMap, final GoogleMapsThemes theme) {
        googleMap.setMapStyle(theme.getMapStyleOptions(activity));
        return theme;
    }

}
