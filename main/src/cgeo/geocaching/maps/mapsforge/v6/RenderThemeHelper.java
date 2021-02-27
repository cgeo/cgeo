package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.FolderUtils;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.ZipRenderTheme;
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;
import org.xmlpull.v1.XmlPullParserException;




/**
 * Helper class for Map Theme selection and related tasks.
 *
 * Works in conjunction with {@link RenderThemeSettings} (for theme settings GUI).
 *
 * Note: this class is an attempt to bundle all large parts of this code were simply moved from class
 * NewMap and might need refactoring.
 */
public class RenderThemeHelper implements XmlRenderThemeMenuCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ZIP_THEME_SEPARATOR = ":";
    private static final int ZIP_RESOURCE_READ_LIMIT = 1024 * 1024; // 1 MB

    private static boolean availableThemesInitialized = false;
    private static final List<String> availableThemes = new ArrayList<>();

    //the last used Zip Resource Provider is cached.
    private static String cachedZipProviderFilename = null;
    private static ZipXmlThemeResourceProvider cachedZipProvider = null;

    private final Activity activity;
    private final SharedPreferences sharedPreferences;


    //current Theme style menu settings
    private XmlRenderThemeStyleMenu themeStyleMenu;
    private String prefThemeOptionMapStyle = "";

    public RenderThemeHelper(final Activity activity) {
        this.activity = activity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void reapplyMapTheme(final ITileLayer tileLayer, final TileCache tileCache) {

        if (tileLayer == null || tileLayer.getTileLayer() == null) {
            return;
        }

        if (!tileLayer.hasThemes()) {
            tileLayer.getTileLayer().requestRedraw();
            return;
        }

        final TileRendererLayer rendererLayer = (TileRendererLayer) tileLayer.getTileLayer();

        //try to apply stored value
        String selectedTheme = setSelectedMapThemeDirect(Settings.getSelectedMapRenderTheme());

        if (StringUtils.isEmpty(selectedTheme)) {
            rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        } else {
            try {
                XmlRenderTheme xmlRenderTheme = null;
                if (!isZipTheme(selectedTheme)) {
                    //xml file theme
                    xmlRenderTheme = new ExternalRenderTheme(new File(LocalStorage.getMapThemeInternalDir(), selectedTheme), this);
                } else {
                    //zip file theme
                    final String[] tokens = selectedTheme.split(ZIP_THEME_SEPARATOR);

                    //always cache the last used ZipResourceProvider. Check if current one can be reused, if not then reload
                    if (cachedZipProvider == null || !tokens[0].equals(cachedZipProviderFilename)) {
                        //reload
                        cachedZipProvider = new ZipXmlThemeResourceProvider(new ZipInputStream(new FileInputStream(new File(LocalStorage.getMapThemeInternalDir(), tokens[0]))), ZIP_RESOURCE_READ_LIMIT);
                        cachedZipProviderFilename = tokens[0];
                    }
                    xmlRenderTheme = new ZipRenderTheme(tokens[1], cachedZipProvider);
                }
                // Validate the theme file
                org.mapsforge.map.rendertheme.rule.RenderThemeHandler.getRenderTheme(AndroidGraphicFactory.INSTANCE, new DisplayModel(), xmlRenderTheme);
                rendererLayer.setXmlRenderTheme(xmlRenderTheme);
            } catch (final IOException e) {
                Log.w("Failed to set render theme", e);
                ActivityMixin.showApplicationToast(getString(R.string.err_rendertheme_file_unreadable));
                rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                selectedTheme = StringUtils.EMPTY;
            } catch (final XmlPullParserException e) {
                Log.w("render theme invalid", e);
                ActivityMixin.showApplicationToast(getString(R.string.err_rendertheme_invalid));
                rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                selectedTheme = StringUtils.EMPTY;
            }
        }

        if (!StringUtils.equals(Settings.getSelectedMapRenderTheme(), selectedTheme)) {
            Settings.setSelectedMapRenderTheme(selectedTheme);
        }

        //copied from NewMap in Feb 2021. Apparently cache tile needs purgin upon theme (re)select
        if (tileCache != null) {
            tileCache.purge();
        }
        rendererLayer.requestRedraw();
    }

    public void selectMapTheme(final ITileLayer tileLayer, final TileCache tileCache) {

        final String currentTheme = Settings.getSelectedMapRenderTheme();


        final List<String> names = new ArrayList<>();
        names.add(getString(R.string.switch_default));
        int currentItem = 0;
        int idx = 1;
        for (final String theme : availableThemes) {
            names.add(theme); //this would be the place to do some user-display-magic if wanted
            if (StringUtils.equals(currentTheme, theme)) {
                currentItem = idx;
            }
            idx++;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);

        builder.setTitle(R.string.map_theme_select);

        builder.setSingleChoiceItems(names.toArray(new String[0]), currentItem, (dialog, newItem) -> {
            // Adjust index because of <default> selection
            if (newItem > 0) {
                Settings.setSelectedMapRenderTheme(availableThemes.get(newItem - 1));
            } else {
                Settings.setSelectedMapRenderTheme(StringUtils.EMPTY);
            }
            reapplyMapTheme(tileLayer, tileCache);

            dialog.cancel();
        });

        builder.show();
    }

    public void selectMapThemeOptions() {
        final Intent intent = new Intent(activity, RenderThemeSettings.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        if (themeStyleMenu != null) {
            intent.putExtra(RenderThemeSettings.RENDERTHEME_MENU, themeStyleMenu);
        }
        activity.startActivity(intent);
    }

    public boolean themeOptionsAvailable() {
        return !StringUtils.isBlank(Settings.getSelectedMapRenderTheme());
    }

    /**
     * Callback handling for theme settings upon new Map Theme selection
     * Note: code was copied 1:1 in February 2021 from NewMap and might need refactoring.
     * <p>
     * Code works in conjunction with {@link RenderThemeSettings} somehow.
     * Apparently map theme settings are "spilled" into c:geo shared preferences
     * (Observation: when using OpenAndroMaps Elevate, those settings usually related settings start with "elmt-")
     */
    @Override
    public Set<String> getCategories(final XmlRenderThemeStyleMenu menu) {
        themeStyleMenu = menu;
        prefThemeOptionMapStyle = menu.getId();
        final String id = this.sharedPreferences.getString(themeStyleMenu.getId(), themeStyleMenu.getDefaultValue());

        final XmlRenderThemeStyleLayer baseLayer = themeStyleMenu.getLayer(id);
        if (baseLayer == null) {
            Log.w("Invalid style " + id);
            return null;
        }
        final Set<String> result = baseLayer.getCategories();

        // add the categories from overlays that are enabled
        for (final XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
            if (this.sharedPreferences.getBoolean(overlay.getId(), overlay.isEnabled())) {
                result.addAll(overlay.getCategories());
            }
        }

        return result;
    }

    /**
     * Callback handling for theme settings upon new Map Theme selection.
     * Note: code was copied 1:1 in February 2021 from NewMap and might need refactoring.
     * Apparently a restart of NewMap activity is needed when the "mapStype" setting in theme settings is changed. Don't know why though.
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String s) {
        if (StringUtils.equals(s, prefThemeOptionMapStyle)) {
            AndroidUtil.restartActivity(activity);
        }
    }


    private String getString(@StringRes final int resId) {
        return CgeoApplication.getInstance().getApplicationContext().getString(resId);
    }

    /**
     * Please call this method upon destroy of related activity to clean up ressources
     */
    public void onDestroy() {
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    /**
     * Set a new map theme. The theme is evaluated against available themes and possibly corrected.
     * Next time a map viewer is opened, the theme will be evaluated and used if possible
     *
     * @param namTheme theme to set
     * @param theme really set (might be corrected if given value is not correct/incomplete)
     */
    public static String setSelectedMapThemeDirect(final String themeCandidate) {

        //try to apply stored value
        String selectedTheme = themeCandidate;
        if (!availableThemes.contains(selectedTheme)) {
            selectedTheme = StringUtils.EMPTY;
            for (String avTheme : availableThemes) {
                //might be a legacy value. Try to find a matching theme ending with stored value
                if (themeCandidate.endsWith("/" + avTheme)) {
                    selectedTheme = avTheme;
                }
                //might be an incomplete ZIP value (only Zip name without a selected inside theme). Then use first found value
                if (avTheme.startsWith(themeCandidate + ZIP_THEME_SEPARATOR)) {
                    selectedTheme = avTheme;
                }
            }
        }
        Settings.setSelectedMapRenderTheme(selectedTheme);
        return selectedTheme;
    }

    /**
     * Syncc public Theme folder with internal app storage copy of it.
     * Call this method whenever you feel that there might be a change in Map Theme files in
     * public folder
     *
     * @param activity Activity used for sync progress display. If null, then sync is done WITHOUT a GUI display (and thread-blocking)
     */
    public static void resynchronizeMapThemeFolder(@Nullable final Activity guiDisplayActivity) {

        if (guiDisplayActivity == null) {
            final FolderUtils.FolderProcessResult result = FolderUtils.get().synchronizeFolder(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(),
                LocalStorage.getMapThemeInternalDir(),
                null);
            checkSynchronizeResult(result);
        } else {
            FolderUtils.get().synchronizeFolderAsynchronousWithGui(
                guiDisplayActivity,
                PersistableFolder.OFFLINE_MAP_THEMES.getFolder(),
                LocalStorage.getMapThemeInternalDir(),
                result -> checkSynchronizeResult(result));
        }
    }

    private static void checkSynchronizeResult(final FolderUtils.FolderProcessResult result) {
        if (!availableThemesInitialized || result.result != FolderUtils.ProcessResult.OK || result.filesModified > 0) {
            recalculateAvailableThemes();
        }
    }

    private static void recalculateAvailableThemes() {
        availableThemes.clear();
        cachedZipProvider = null;
        cachedZipProviderFilename = null;

        addAvailableThemes(LocalStorage.getMapThemeInternalDir(), availableThemes, "");
        availableThemesInitialized = true;
    }

    private static void addAvailableThemes(final File dir, final List<String> themes, final String prefix) {

        for (File candidate : dir.listFiles()) {
            if (candidate.isDirectory()) {
                addAvailableThemes(candidate, themes, prefix + candidate.getName() + "/");
            } else if (candidate.getName().endsWith(".xml")) {
                themes.add(prefix + candidate.getName());
            } else if (candidate.getName().endsWith(".zip")) {
                try {
                    themes.addAll(CollectionStream.of(
                        ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new FileInputStream(candidate)))).map(t -> prefix + candidate.getName() + ZIP_THEME_SEPARATOR + t).toList());
                } catch (IOException ioe) {
                    Log.w("Map Theme ZIP '" + candidate + "' could not be read", ioe);
                }
            }
        }
    }

    private static boolean isZipTheme(final String theme) {
        return theme != null && theme.contains(ZIP_THEME_SEPARATOR);
    }
}

