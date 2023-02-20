package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorage.FileInformation;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.FolderUtils;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.ContentRenderTheme;
import org.mapsforge.map.android.rendertheme.ContentResolverResourceProvider;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.ZipRenderTheme;
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;


/**
 * Helper class for Map Theme selection and related tasks.
 *
 * Works in conjunction with {@link RenderThemeSettings} (for theme settings GUI).
 *
 * Note: this class is an attempt to bundle all large parts of this code were simply moved from class
 * NewMap and might need refactoring.
 */
public class RenderThemeHelper implements XmlRenderThemeMenuCallback {

    private static final PersistableFolder MAP_THEMES_FOLDER = PersistableFolder.OFFLINE_MAP_THEMES;
    private static final File MAP_THEMES_INTERNAL_FOLDER = LocalStorage.getMapThemeInternalSyncDir();

    private static final String ZIP_THEME_SEPARATOR = ":";

    private static final int ZIP_FILE_SIZE_LIMIT = 5 * 1024 * 1024; // 5 MB
    private static final int ZIP_RESOURCE_READ_LIMIT = 1024 * 1024; // 1 MB

    private static final long FILESYNC_MAX_FILESIZE = 5 * 1024 * 1024; //5MB

    private static final int AVAILABLE_THEMES_SCAN_MAXDEPTH = 2;

    private static final Object availableThemesMutex = new Object();
    private static final List<ThemeData> availableThemes = new ArrayList<>();
    private static boolean availableThemesInitialized = false;

    private static final Object cachedZipMutex = new Object();

    private final Activity activity;
    private final SharedPreferences sharedPreferences;

    //current Theme style menu settings
    private XmlRenderThemeStyleMenu themeStyleMenu;
    private String prefThemeOptionMapStyle = "";
    private String prefThemeStyleKey = "";

    //the last used Zip Resource Provider is cached.
    private static String cachedZipProviderFilename = null;
    private static ZipXmlThemeResourceProvider cachedZipProvider = null;

    // cache most recent resource provider
    private XmlThemeResourceProvider resourceProvider = null;

    private static MapThemeFolderSynchronizer syncTask = null;

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

    public RenderThemeHelper(final Activity activity) {
        this.activity = activity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
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
        ThemeData selectedTheme = setSelectedMapTheme(Settings.getSelectedMapRenderTheme());


        if (selectedTheme == null) {
            applyDefaultTheme(rendererLayer);
        } else {
            try {
                //get the theme
                final XmlRenderTheme xmlRenderTheme = createThemeFor(selectedTheme);

                // Validate the theme
                org.mapsforge.map.rendertheme.rule.RenderThemeHandler.getRenderTheme(AndroidGraphicFactory.INSTANCE, new DisplayModel(), xmlRenderTheme);
                rendererLayer.setXmlRenderTheme(xmlRenderTheme);
                //setting xmlrendertheme has filled prefThemeStyleKey -> now apply scales
                applyScales(rendererLayer, prefThemeStyleKey);
            } catch (final IOException e) {
                Log.w("Failed to set render theme", e);
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_rendertheme_file_unreadable));
                rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                selectedTheme = null;
            } catch (final Exception e) {
                Log.w("render theme invalid", e);
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_rendertheme_invalid));
                rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                selectedTheme = null;
            }
        }
        setSelectedTheme(selectedTheme);

        //copied from NewMap in Feb 2021. Apparently cache tile needs purgin upon theme (re)select
        if (tileCache != null) {
            tileCache.purge();
        }
        rendererLayer.requestRedraw();
    }

    private void applyDefaultTheme(final TileRendererLayer rendererLayer) {
        rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        applyScales(rendererLayer, Settings.RENDERTHEMESCALE_DEFAULTKEY);
    }

    private void applyScales(final TileRendererLayer rendererLayer, final String themeStyleId) {

        final int mapScale = Settings.getMapRenderScale(themeStyleId, Settings.RenderThemeScaleType.MAP);
        final int textScale = Settings.getMapRenderScale(themeStyleId, Settings.RenderThemeScaleType.TEXT);
        final int symbolScale = Settings.getMapRenderScale(themeStyleId, Settings.RenderThemeScaleType.SYMBOL);

        rendererLayer.getDisplayModel().setUserScaleFactor(mapScale / 100f);
        rendererLayer.setTextScale(textScale / 100f);
        DisplayModel.symbolScale = symbolScale / 100f;

    }

    private XmlRenderTheme createThemeFor(@NonNull final ThemeData theme) throws IOException {
        final String[] themeIdTokens = theme.id.split(ZIP_THEME_SEPARATOR);
        final boolean isZipTheme = themeIdTokens.length == 2;

        if (theme.fileInfo == null || theme.fileInfo.uri == null || theme.containingFolder == null) {
            return null;
        }

        XmlRenderTheme xmlRenderTheme = null;
        try {
            if (!isZipTheme) {
                if (UriUtils.isFileUri(theme.fileInfo.uri)) {
                    xmlRenderTheme = new ExternalRenderTheme(UriUtils.toFile(theme.fileInfo.uri), this);
                } else {
                    //this is the SLOW THEME path. Show OneTimeDialog to warn user about this
                    Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_THEME_FIX_SLOWNESS);
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
            resourceProvider = xmlRenderTheme == null ? null : xmlRenderTheme.getResourceProvider();
        } catch (Exception ex) {
            Log.w("Problem loading Theme [" + theme.id + "]'" + theme.fileInfo.uri + "'", ex);
            xmlRenderTheme = null;
            cachedZipProvider = null;
            cachedZipProviderFilename = null;
        }
        return xmlRenderTheme;
    }

    public void selectMapTheme(final ITileLayer tileLayer, final TileCache tileCache) {

        final String currentThemeId = Settings.getSelectedMapRenderTheme();
        final boolean debugMode = Settings.isDebug();


        final List<String> names = new ArrayList<>();
        names.add(LocalizationUtils.getString(R.string.switch_default));
        int currentItem = 0;
        int idx = 1;
        final List<ThemeData> selectableAvThemes = getAvailableThemes();
        for (final ThemeData theme : selectableAvThemes) {
            names.add(theme.userDisplayableName + (debugMode ? " (" + theme.id + ")" : ""));
            if (StringUtils.equals(currentThemeId, theme.id)) {
                currentItem = idx;
            }
            idx++;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        String title = activity.getString(R.string.map_theme_select);
        if (debugMode) {
            title = title + " (debug mode, sync = " + (isThemeSynchronizationActive() ? "ON" : "off") + ")";
        }

        builder.setTitle(title);

        builder.setSingleChoiceItems(names.toArray(new String[0]), currentItem, (dialog, newItem) -> {
            // Adjust index because of <default> selection
            setSelectedTheme(newItem > 0 ? selectableAvThemes.get(newItem - 1) : null);
            reapplyMapTheme(tileLayer, tileCache);
            dialog.cancel();
        });

        builder.show();
    }

    public void selectMapThemeOptions() {
        final Intent intent = new Intent(activity, RenderThemeSettings.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        if (themeOptionsAvailable() && themeStyleMenu != null) {
            intent.putExtra(RenderThemeSettingsFragment.RENDERTHEME_MENU, themeStyleMenu);
        }
        activity.startActivity(intent);
    }

    public boolean themeOptionsAvailable() {
        return StringUtils.isNotBlank(Settings.getSelectedMapRenderTheme());
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
        prefThemeStyleKey = menu.getId() + "-" + id;
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
    private static ThemeData setSelectedMapTheme(final String themeIdCandidate) {

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
        Settings.setSelectedMapRenderTheme(theme == null ? StringUtils.EMPTY : theme.id);
    }

    /**
     * Depending on whether map theme folder synchronization is currently turned off or on, this
     * method does two different things:
     * * if turned off: app-private local folder is safely deleted
     * * if turned on: folder is re-synced (every change in source folder is synced to target folder)
     *
     * In any case, this method will take care of thread syncrhonization e.g. when a sync is currently running then
     * this sync will first be aborted, and only after that either target folder is deleted (if sync=off) or sync is restarted (if sync=on)
     *
     * Call this method whenever you feel that there might be a change in Map Theme files in
     * public folder. Sync will be done in background task and reports its progress via toasts
     */
    public static void resynchronizeOrDeleteMapThemeFolder() {
        MapThemeFolderSynchronizer.requestResynchronization(MAP_THEMES_FOLDER.getFolder(), MAP_THEMES_INTERNAL_FOLDER, isThemeSynchronizationActive());
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

    private static class MapThemeFolderSynchronizer extends AsyncTask<Void, Void, FolderUtils.FolderProcessResult> {

        private enum AfterSyncRequest { EXIT_NORMAL, REDO, ABORT_DELETE }

        private static final Object syncTaskMutex = new Object();

        private final Folder source;
        private final File target;
        private final boolean doSync;

        private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

        private final Object requestRedoMutex = new Object();
        private boolean taskIsDone = false;
        private AfterSyncRequest afterSyncRequest = AfterSyncRequest.EXIT_NORMAL;
        private long startTime = System.currentTimeMillis();

        public static void requestResynchronization(final Folder source, final File target, final boolean doSync) {
            synchronized (syncTaskMutex) {
                if (syncTask == null || !syncTask.requestAfter(doSync ? MapThemeFolderSynchronizer.AfterSyncRequest.REDO : MapThemeFolderSynchronizer.AfterSyncRequest.ABORT_DELETE)) {
                    Log.i("[MapThemeFolderSync] start synchronization " + source + " -> " + target);
                    syncTask = new MapThemeFolderSynchronizer(source, target, doSync);
                    syncTask.execute();
                }
            }
        }

        private MapThemeFolderSynchronizer(final Folder source, final File target, final boolean doSync) {
            this.source = source;
            this.target = target;
            this.doSync = doSync;
        }

        /**
         * Requests for a running task to redo sync after finished. May fail if task is already done, but in this case the task may safely be discarted
         */
        public boolean requestAfter(final AfterSyncRequest afterSyncRequest) {
            synchronized (requestRedoMutex) {
                if (taskIsDone || !doSync) {
                    return false;
                }
                Log.i("[MapThemeFolderSync] Requesting '" + afterSyncRequest + "' " + source + " -> " + target);
                cancelFlag.set(true);
                this.afterSyncRequest = afterSyncRequest;
                startTime = System.currentTimeMillis();
                return true;
            }
        }

        @Override
        protected FolderUtils.FolderProcessResult doInBackground(final Void[] params) {
            Log.i("[MapThemeFolderSync] start synchronization " + source + " -> " + target + " (doSync=" + doSync + ")");
            FolderUtils.FolderProcessResult result = null;
            if (!doSync) {
                FileUtils.deleteDirectory(target);
            } else {
                boolean cont = true;
                while (cont) {
                    result = FolderUtils.get().synchronizeFolder(source, target, MapThemeFolderSynchronizer::shouldBeSynced, cancelFlag, null);
                    synchronized (requestRedoMutex) {
                        switch (afterSyncRequest) {
                            case EXIT_NORMAL:
                                taskIsDone = true;
                                cont = false;
                                break;
                            case ABORT_DELETE:
                                FileUtils.deleteDirectory(target);
                                cont = false;
                                break;
                            case REDO:
                                Log.i("[MapThemeFolderSync] redo synchronization " + source + " -> " + target);
                                cancelFlag.set(false);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            synchronized (availableThemesMutex) {
                if (result == null || !availableThemesInitialized || result.result != FolderUtils.ProcessResult.OK || result.filesModified > 0) {
                    recalculateAvailableThemes();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final FolderUtils.FolderProcessResult result) {
            Log.i("[MapThemeFolderSync] Finished synchronization (state=" + afterSyncRequest + ")");
            //show toast only if something actually happened
            if (result != null && result.filesModified > 0) {
                showToast(R.string.mapthemes_foldersync_finished_toast,
                        LocalizationUtils.getString(R.string.persistablefolder_offline_maps_themes),
                        Formatter.formatDuration(System.currentTimeMillis() - startTime),
                        result.filesModified, LocalizationUtils.getPlural(R.plurals.file_count, result.filesInSource, "file(s)"));
            }
            Log.i("[MapThemeFolderSync] Finished synchronization callback");
        }

        private static boolean shouldBeSynced(final FileInformation fileInfo) {
            return fileInfo != null && !fileInfo.name.endsWith(".map") && fileInfo.size <= FILESYNC_MAX_FILESIZE;
        }

        private static void showToast(final int resId, final Object... params) {
            final ImmutablePair<String, String> msgs = LocalizationUtils.getMultiPurposeString(resId, "RenderTheme", params);
            ActivityMixin.showApplicationToast(msgs.left);
            Log.iForce("[RenderThemeHelper.ThemeFolderSyncTask]" + msgs.right);
        }

    }

    private static ContentResolver getContentResolver() {
        return CgeoApplication.getInstance().getContentResolver();
    }

    public static boolean isThemeSynchronizationActive() {
        return Settings.getSyncMapRenderThemeFolder();
    }

    /**
     * Method is called after user has changed the sync state in Settings Activity
     */
    public static boolean changeSyncSetting(final Activity activity, final boolean doSync, final Consumer<Boolean> callback) {

        if (doSync) {
            //this means user just turned sync on. Ask user if he/she is really shure about this.-
            final FolderUtils.FolderInfo themeFolderInfo = FolderUtils.get().getFolderInfo(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), -1);
            final String folderName = MAP_THEMES_FOLDER.getFolder().toUserDisplayableString();
            final ImmutableTriple<String, String, String> folderInfoStrings = themeFolderInfo.getUserDisplayableFolderInfoStrings();
            Dialogs.newBuilder(activity)
                    .setTitle(R.string.init_renderthemefolder_synctolocal_dialog_title)
                    .setMessage(LocalizationUtils.getString(R.string.init_renderthemefolder_synctolocal_dialog_message, folderName, folderInfoStrings.left, folderInfoStrings.middle, folderInfoStrings.right))
                    .setPositiveButton(android.R.string.ok, (d, c) -> {
                        d.dismiss();
                        //start sync
                        resynchronizeOrDeleteMapThemeFolder();
                        callback.accept(true);
                    })
                    .setNegativeButton(android.R.string.cancel, (d, c) -> {
                        d.dismiss();
                        callback.accept(false);
                        //following method will DELETE any existing data in sync folder (because sync is set to off in settings)
                        resynchronizeOrDeleteMapThemeFolder();
                    })
                    .create().show();
        } else {
            //this means user just turned sync OFF
            Settings.setSyncMapRenderThemeFolder(false);
            //start sync will delete any existing data in sync folder
            resynchronizeOrDeleteMapThemeFolder();
            callback.accept(false);
        }
        return true;
    }

    public XmlThemeResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public static RenderThemeType getRenderThemeType() {
        final String selectedMapRenderTheme = Settings.getSelectedMapRenderTheme();
        for (RenderThemeHelper.RenderThemeType rtt : RenderThemeHelper.RenderThemeType.values()) {
            for (String searchPath : rtt.searchPaths) {
                if (StringUtils.containsIgnoreCase(selectedMapRenderTheme, searchPath)) {
                    return rtt;
                }
            }
        }
        return RenderThemeType.RTT_NONE;
    }

}

