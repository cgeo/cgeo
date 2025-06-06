package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorage.FileInformation;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeVTMOfflineTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.oscim.android.theme.ContentRenderTheme;
import org.oscim.android.theme.ContentResolverResourceProvider;
import org.oscim.map.Map;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.theme.ZipRenderTheme;
import org.oscim.theme.ZipXmlThemeResourceProvider;

public class MapsforgeThemeHelper implements XmlRenderThemeMenuCallback {

    private static final PersistableFolder MAP_THEMES_FOLDER = PersistableFolder.OFFLINE_MAP_THEMES;
    private static final File MAP_THEMES_INTERNAL_FOLDER = LocalStorage.getMapThemeInternalSyncDir();

    private static final String ZIP_THEME_SEPARATOR = ":";

    private static final int ZIP_FILE_SIZE_LIMIT = 5 * 1024 * 1024; // 5 MB
    private static final int ZIP_RESOURCE_READ_LIMIT = 1024 * 1024; // 1 MB

    private static final int AVAILABLE_THEMES_SCAN_MAXDEPTH = 2;

    private static final Object availableThemesMutex = new Object();
    private static final List<ThemeData> availableThemes = new ArrayList<>();
    private static boolean availableThemesInitialized = false;

    private static final Object cachedZipMutex = new Object();

    private final SharedPreferences sharedPreferences;
    private IRenderTheme mTheme;

    //current Theme style menu settings
    private XmlRenderThemeStyleMenu themeStyleMenu;

    //the last used Zip Resource Provider is cached.
    private static String cachedZipProviderFilename = null;
    private static ZipXmlThemeResourceProvider cachedZipProvider = null;

    private static class ThemeData {
        public final String id;
        public final String userDisplayableName;
        public final FileInformation fileInfo;
        public final Folder containingFolder;

        private ThemeData(final String id, final String userDisplayableName, final FileInformation fileInfo, final Folder containingFolder) {
            this.id = id;
            this.userDisplayableName = userDisplayableName;
            this.fileInfo = fileInfo;
            this.containingFolder = containingFolder;
        }
    }

    public enum RenderThemeType {
        RTT_NONE("", new String[]{}),
        RTT_ELEVATE("", new String[]{"elevate", "elements"}),
        RTT_FZK_BASE("freizeitkarte-v5", new String[]{"freizeitkarte"}),
        RTT_FZK_OUTDOOR_CONTRAST("fzk-outdoor-contrast-v5", new String[]{"fzk-outdoor-contrast"}),
        RTT_FZK_OUTDOOR_SOFT("fzk-outdoor-soft-v5", new String[]{"fzk-outdoor-soft"}),
        RTT_PAWS("paws_4", new String[]{"paws_4"}),
        RTT_VOLUNTARY("", new String[]{"voluntary v5", "velocity v5"});

        public final String relPath;
        public final String[] searchPaths;

        RenderThemeType(final String relPath, final String[] searchPaths) {
            this.relPath = relPath;
            this.searchPaths = searchPaths;
        }
    }

    protected MapsforgeThemeHelper(final Activity activity) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    protected void reapplyMapTheme(final Map map, final AbstractTileProvider tileProvider) {
        if (mTheme != null) {
            disposeTheme();
        }
        if (!tileProvider.supportsThemes()) {
            return;
        }

        //try to apply stored value
        ThemeData selectedTheme = setSelectedMapThemeInternal(Settings.getSelectedMapRenderTheme(Settings.getTileProvider()));

        if (selectedTheme == null) {
            applyDefaultTheme(map, tileProvider);
        } else {
            try {
                //get the theme
                final ThemeFile xmlRenderTheme = createThemeFor(selectedTheme);

                // Validate the theme
                /* @todo
                org.mapsforge.map.rendertheme.rule.RenderThemeHandler.getRenderTheme(AndroidGraphicFactory.INSTANCE, new DisplayModel(), xmlRenderTheme);
                rendererLayer.setXmlRenderTheme(xmlRenderTheme);
                */
                mTheme = map.setTheme(xmlRenderTheme);
            } catch (final Exception e) {
                Log.w("render theme invalid", e);
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_rendertheme_invalid));
                applyDefaultTheme(map, tileProvider);
                selectedTheme = null;
            }
        }
        setSelectedTheme(selectedTheme);

        if (tileProvider instanceof AbstractMapsforgeVTMOfflineTileProvider) {
            ((AbstractMapsforgeVTMOfflineTileProvider) tileProvider).switchBuildingLayer(Settings.getBuildings3D());
        }

        map.updateMap(true);
        map.render();
    }

    private void applyDefaultTheme(final Map map, final AbstractTileProvider tileProvider) {
        if (tileProvider.supportsThemes()) {
            mTheme = map.setTheme(VtmThemes.getDefaultVariant());
        }
    }

    protected void disposeTheme() {
        if (mTheme != null) {
            mTheme.dispose();
        }
    }

    private ThemeFile createThemeFor(@NonNull final ThemeData theme) {
        final String[] themeIdTokens = theme.id.split(ZIP_THEME_SEPARATOR);
        final boolean isZipTheme = themeIdTokens.length == 2;

        if (theme.fileInfo == null || theme.fileInfo.uri == null || theme.containingFolder == null) {
            return null;
        }

        ThemeFile xmlRenderTheme = null;
        try {
            if (!isZipTheme) {
                if (UriUtils.isFileUri(theme.fileInfo.uri)) {
                    xmlRenderTheme = new ExternalRenderTheme(theme.fileInfo.uri.toString(), this);
                } else {
                    //this is the SLOW THEME path. Show OneTimeDialog to warn user about this
                    Dialogs.basicOneTimeMessage(CgeoApplication.getInstance(), OneTimeDialogs.DialogType.MAP_THEME_FIX_SLOWNESS);
                    xmlRenderTheme = new ContentRenderTheme(getContentResolver(), theme.fileInfo.uri, this);
                    xmlRenderTheme.setResourceProvider(new ContentResolverResourceProvider(getContentResolver(), ContentStorage.get().getUriForFolder(theme.containingFolder), true));
                }
            } else {
                //always cache the last used ZipResourceProvider. Check if current one can be reused, if not then reload
                synchronized (cachedZipMutex) {
                    if (cachedZipProvider == null || !themeIdTokens[0].equals(cachedZipProviderFilename)) {
                        if (theme.fileInfo.size > ZIP_FILE_SIZE_LIMIT) {
                            cachedZipProvider = null;
                            cachedZipProviderFilename = null;
                        } else {
                            cachedZipProvider = new ZipXmlThemeResourceProvider(new ZipInputStream(ContentStorage.get().openForRead(theme.fileInfo.uri)), ZIP_RESOURCE_READ_LIMIT);
                            cachedZipProviderFilename = themeIdTokens[0];
                        }
                    }
                    xmlRenderTheme = cachedZipProvider == null ? null : new ZipRenderTheme(themeIdTokens[1], cachedZipProvider, this);
                }
            }
        } catch (Exception ex) {
            Log.w("Problem loading Theme [" + theme.id + "]'" + theme.fileInfo.uri + "'", ex);
            xmlRenderTheme = null;
            cachedZipProvider = null;
            cachedZipProviderFilename = null;
        }
        return xmlRenderTheme;
    }

    protected void selectMapTheme(final Activity activity, final Map map, final AbstractTileProvider tileProvider) {
        if (!tileProvider.supportsThemes()) {
            return;
        }
        final String currentThemeId = Settings.getSelectedMapRenderTheme(tileProvider);

        final List<String> names = new ArrayList<>();
        names.add(activity.getString(R.string.switch_default));
        int currentItem = 0;
        int idx = 1;
        final List<ThemeData> selectableAvThemes = getAvailableThemes();
        for (final ThemeData theme : selectableAvThemes) {
            names.add(theme.userDisplayableName);
            if (StringUtils.equals(currentThemeId, theme.id)) {
                currentItem = idx;
            }
            idx++;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.map_theme_select));
        builder.setSingleChoiceItems(names.toArray(new String[0]), currentItem, (dialog, newItem) -> {
            // Adjust index because of <default> selection
            setSelectedTheme(newItem > 0 ? selectableAvThemes.get(newItem - 1) : null);
            reapplyMapTheme(map, tileProvider);
            dialog.cancel();
        });

        builder.show();
    }

    public void selectMapThemeOptions(final Activity activity, final AbstractTileProvider tileProvider) {
        if (!tileProvider.supportsThemeOptions()) {
            return;
        }

        final Intent intent = new Intent(activity, MapsforgeThemeSettings.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        if (themeOptionsAvailable() && themeStyleMenu != null) {
            intent.putExtra(MapsforgeThemeSettingsFragment.RENDERTHEME_MENU, themeStyleMenu);
            intent.putExtra(MapsforgeThemeSettingsFragment.SHOW3DOPTION, tileProvider instanceof AbstractMapsforgeVTMOfflineTileProvider);
        }
        activity.startActivity(intent);
    }

    public boolean themeOptionsAvailable() {
        return StringUtils.isNotBlank(Settings.getSelectedMapRenderTheme(Settings.getTileProvider()));
    }

    /**
     * Callback handling for theme settings upon MapsforgeVTM theme options selection
     * Note: map theme settings are "spilled" into c:geo shared preferences
     * (Observation: when using OpenAndroMaps Elevate, those settings usually related settings start with "elmt-")
     */
    @Override
    public Set<String> getCategories(final XmlRenderThemeStyleMenu menu) {
        themeStyleMenu = menu;
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
     * Set a new map theme. The theme is evaluated against available themes and possibly corrected.
     * Next time a map viewer is opened, the theme will be evaluated and used if possible
     */
    private static ThemeData setSelectedMapThemeInternal(final String themeIdCandidate) {
        //try to apply stored value
        ThemeData selectedTheme = null;
        final List<ThemeData> avThemes = getAvailableThemes();
        //search for exact match first
        for (ThemeData avTheme : avThemes) {
            if (avTheme.id.equals(themeIdCandidate)) {
                selectedTheme = avTheme;
                break;
            }
        }

        //if no exact match found, check special cases
        if (selectedTheme == null) {
            for (ThemeData avTheme : avThemes) {
                final String avThemeId = avTheme.id;

                //might be a legacy value. Try to find a matching theme ending with stored value
                if (themeIdCandidate.endsWith("/" + avThemeId)) {
                    selectedTheme = avTheme;
                    break;
                }
                //might be an incomplete ZIP value (only Zip name without a selected inside theme). Then use first found value
                if (avThemeId.startsWith(themeIdCandidate + ZIP_THEME_SEPARATOR)) {
                    selectedTheme = avTheme;
                    break;
                }
            }
        }
        setSelectedTheme(selectedTheme);
        return selectedTheme;
    }

    private static void setSelectedTheme(final ThemeData theme) {
        Settings.setSelectedMapRenderTheme(Settings.getTileProvider().getId(), theme == null ? StringUtils.EMPTY : theme.id);
    }

    /**
     * recalculate available themes out of the currently active folder
     */
    private static void recalculateAvailableThemes() {
        final List<ThemeData> newAvailableThemes = new ArrayList<>();
        addAvailableThemes(isThemeSynchronizationActive() ? Folder.fromFile(MAP_THEMES_INTERNAL_FOLDER) : MAP_THEMES_FOLDER.getFolder(), newAvailableThemes, "", 0);

        Collections.sort(newAvailableThemes, (t1, t2) -> TextUtils.COLLATOR.compare(t1.userDisplayableName, t2.userDisplayableName));

        synchronized (availableThemesMutex) {
            availableThemes.clear();
            availableThemes.addAll(newAvailableThemes);
            availableThemesInitialized = true;
        }

        synchronized (cachedZipMutex) {
            cachedZipProvider = null;
            cachedZipProviderFilename = null;
        }
    }

    private static List<ThemeData> getAvailableThemes() {
        synchronized (availableThemesMutex) {
            if (!availableThemesInitialized) {
                //async scan not finished -> rescan synchronized and GUI-blocking!
                recalculateAvailableThemes();
            }
            //make a copy to be thread-safe
            return new ArrayList<>(availableThemes);
        }
    }

    private static void addAvailableThemes(@NonNull final Folder dir, final List<ThemeData> themes, final String prefix, final int level) {
        for (FileInformation candidate : Objects.requireNonNull(ContentStorage.get().list(dir))) {
            if (candidate.isDirectory && (AVAILABLE_THEMES_SCAN_MAXDEPTH < 0 || level < AVAILABLE_THEMES_SCAN_MAXDEPTH)) {
                addAvailableThemes(candidate.dirLocation, themes, prefix + candidate.name + "/", level + 1);
            } else if (candidate.name.endsWith(".xml")) {
                final String themeId = prefix + candidate.name;
                themes.add(new ThemeData(themeId, toUserDisplayableName(candidate, null), candidate, dir));
            } else if (candidate.name.endsWith(".zip") && candidate.size <= ZIP_FILE_SIZE_LIMIT) {
                try (InputStream is = ContentStorage.get().openForRead(candidate.uri)) {
                    for (String zipXmlTheme : ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(is))) {
                        final String themeId = prefix + candidate.name + ZIP_THEME_SEPARATOR + zipXmlTheme;
                        themes.add(new ThemeData(themeId, toUserDisplayableName(candidate, zipXmlTheme), candidate, dir));
                    }
                } catch (IOException ioe) {
                    Log.w("Map Theme ZIP '" + candidate + "' could not be read", ioe);
                } catch (Exception e) {
                    Log.w("Problem opening map Theme ZIP '" + candidate + "'", e);
                }
            }
        }
    }

    /**
     * Calculates a xml theme name fit for user display (in dropdown etc)
     *
     * @param file    theme file name
     * @param zipPath if theme file is a ZIP, then this contains the zip-internal path to the xml. Otherwise null
     * @return user display theme name
     */
    private static String toUserDisplayableName(final FileInformation file, final String zipPath) {
        String userDisplay = StringUtils.removeEnd(file.name, ".xml");
        if (zipPath != null) {
            final int idx = zipPath.lastIndexOf("/");
            userDisplay = userDisplay + "/" + StringUtils.removeEnd(idx < 0 ? zipPath : zipPath.substring(idx + 1), ".xml");
        }
        return userDisplay;
    }

    private static ContentResolver getContentResolver() {
        return CgeoApplication.getInstance().getContentResolver();
    }

    public static boolean isThemeSynchronizationActive() {
        return Settings.getSyncMapRenderThemeFolder();
    }

}
