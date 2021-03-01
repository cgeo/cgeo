package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.FolderUtils;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.ContentRenderTheme;
import org.mapsforge.map.android.rendertheme.ContentResolverResourceProvider;
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
    private static final String CONTENT_THEMEID_PREFIX = "content::";
    private static final String CONTENT_THEMEID_PREFIX_USER_DISPLAY = "z[SAFTest] ";

    private static final int ZIP_RESOURCE_READ_LIMIT = 1024 * 1024; // 1 MB

    private  static final Object availableThemesMutex = new Object();
    private static final List<ImmutablePair<String, String>> availableThemes = new ArrayList<>();

    private static final Object cachedZipMutex = new Object();
    private static final Object syncTaskMutex = new Object();

    private final Activity activity;
    private final SharedPreferences sharedPreferences;


    //the last used Zip Resource Provider is cached.
    private static String cachedZipProviderFilename = null;
    private static ZipXmlThemeResourceProvider cachedZipProvider = null;

    private static MapThemeFolderSyncTask syncTask = null;

    static {
        try {
            recalculateAvailableThemes();
        } catch (Exception e) {
            Log.e("Error initializing RenderThemeHelper", e);
        }
    }



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
                //get the theme
                final XmlRenderTheme xmlRenderTheme = getThemeFor(selectedTheme);

                // Validate the theme
                org.mapsforge.map.rendertheme.rule.RenderThemeHandler.getRenderTheme(AndroidGraphicFactory.INSTANCE, new DisplayModel(), xmlRenderTheme);
                rendererLayer.setXmlRenderTheme(xmlRenderTheme);
            } catch (final IOException e) {
                Log.w("Failed to set render theme", e);
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_rendertheme_file_unreadable));
                rendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                selectedTheme = StringUtils.EMPTY;
            } catch (final XmlPullParserException e) {
                Log.w("render theme invalid", e);
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_rendertheme_invalid));
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

    private XmlRenderTheme getThemeFor(final String pThemeId) throws IOException {
        boolean isContentTheme = false;
        String themeId = pThemeId;
        if (themeId.startsWith(CONTENT_THEMEID_PREFIX)) {
            isContentTheme = true;
            themeId = themeId.substring(CONTENT_THEMEID_PREFIX.length());
        }
        final String[] themeIdTokens = themeId.split(ZIP_THEME_SEPARATOR);
        final boolean isZipTheme = themeIdTokens.length == 2;

        final XmlRenderTheme xmlRenderTheme;
        if (!isZipTheme) {
            if (!isContentTheme) {
                xmlRenderTheme = new ExternalRenderTheme(new File(LocalStorage.getMapThemeInternalDir(), themeId), this);
            } else {
                final ImmutablePair<ContentStorage.FileInformation, Folder> fileFolder =
                    ContentStorage.get().getParentFolderAndFileInfo(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), themeIdTokens[0]);
                if (fileFolder == null || fileFolder.left == null) {
                    xmlRenderTheme = null;
                } else {
                    xmlRenderTheme = new ContentRenderTheme(getContentResolver(), fileFolder.left.uri, this);
                    xmlRenderTheme.setResourceProvider(new ContentResolverResourceProvider(getContentResolver(),
                        ContentStorage.get().getUriForFolder(fileFolder.right), true));
                }
            }
        } else {
            if (!isContentTheme) {
                //always cache the last used ZipResourceProvider. Check if current one can be reused, if not then reload
                synchronized (cachedZipMutex) {
                    if (cachedZipProvider == null || !themeIdTokens[0].equals(cachedZipProviderFilename)) {
                        //reload
                        cachedZipProvider = new ZipXmlThemeResourceProvider(
                            new ZipInputStream(new FileInputStream(new File(LocalStorage.getMapThemeInternalDir(), themeIdTokens[0]))), ZIP_RESOURCE_READ_LIMIT);
                        cachedZipProviderFilename = themeIdTokens[0];
                    }
                    xmlRenderTheme = new ZipRenderTheme(themeIdTokens[1], cachedZipProvider, this);
                }
            } else {
                xmlRenderTheme = new ZipRenderTheme(themeIdTokens[1],
                    new ZipXmlThemeResourceProvider(new ZipInputStream(
                        ContentStorage.get().openForRead(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), themeIdTokens[0]))),
                    this);
            }
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
        final List<ImmutablePair<String, String>> selectableAvThemes = CollectionStream.of(getAvailableThemes())
            .filter(ip -> Settings.isDebug() || !ip.left.startsWith(CONTENT_THEMEID_PREFIX))
            .toList();

        for (final ImmutablePair<String, String> themePair : selectableAvThemes) {
            names.add(themePair.right + (debugMode ? " (" + themePair.left + ")" : ""));
            if (StringUtils.equals(currentThemeId, themePair.left)) {
                currentItem = idx;
            }
            idx++;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        String title = activity.getString(R.string.map_theme_select);
        if (debugMode) {
            title = title + " (debug mode)";
        }

        builder.setTitle(title);

        builder.setSingleChoiceItems(names.toArray(new String[0]), currentItem, (dialog, newItem) -> {
            // Adjust index because of <default> selection
            if (newItem > 0) {
                Settings.setSelectedMapRenderTheme(selectableAvThemes.get(newItem - 1).left);
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
    public static String setSelectedMapThemeDirect(final String themeIdCandidate) {

        //try to apply stored value
        String selectedThemeId = StringUtils.EMPTY;
        final List<ImmutablePair<String, String>> avThemes = getAvailableThemes();
        //search for exact match first
        for (ImmutablePair<String, String> avThemePair : avThemes) {
            if (avThemePair.left.equals(themeIdCandidate)) {
                selectedThemeId = avThemePair.left;
                break;
            }
        }

        //if no exact match found, check special cases
        if (StringUtils.isBlank(selectedThemeId)) {
            for (ImmutablePair<String, String> avThemePair : avThemes) {
                final String avThemeId = avThemePair.left;

               //might be a legacy value. Try to find a matching theme ending with stored value
                if (themeIdCandidate.endsWith("/" + avThemeId)) {
                    selectedThemeId = avThemeId;
                    break;
                }
                //might be an incomplete ZIP value (only Zip name without a selected inside theme). Then use first found value
                if (avThemeId.startsWith(themeIdCandidate + ZIP_THEME_SEPARATOR)) {
                    selectedThemeId = avThemeId;
                    break;
                }
            }
        }
        Settings.setSelectedMapRenderTheme(selectedThemeId);
        return selectedThemeId;
    }

    /**
     * Syncc public Theme folder with internal app storage copy of it.
     *
     * Call this method whenever you feel that there might be a change in Map Theme files in
     * public folder. Sync will be done in background task and reports its progress via toasts
     */
    public static void resynchronizeMapThemeFolder() {

        synchronized (syncTaskMutex) {
            if (syncTask == null || !syncTask.requestRedo()) {
                syncTask = MapThemeFolderSyncTask.createAndExecute(
                    PersistableFolder.OFFLINE_MAP_THEMES.getFolder(),
                    LocalStorage.getMapThemeInternalDir(),
                    RenderThemeHelper::checkSynchronizeResult);
            }
        }
    }

    private static void checkSynchronizeResult(final FolderUtils.FolderProcessResult result) {
        if (result.result != FolderUtils.ProcessResult.OK || result.filesModified > 0) {
            recalculateAvailableThemes();
        }
    }

    private static void recalculateAvailableThemes() {

        final List<ImmutablePair<String, String>> newAvailableThemes = new ArrayList<>();
        addAvailableThemes(LocalStorage.getMapThemeInternalDir(), newAvailableThemes, "");
        Collections.sort(newAvailableThemes, (t1, t2) -> TextUtils.COLLATOR.compare(t1.right, t2.right));

        synchronized (availableThemesMutex) {
            availableThemes.clear();
            availableThemes.addAll(newAvailableThemes);
        }

        synchronized (cachedZipMutex) {
            cachedZipProvider = null;
            cachedZipProviderFilename = null;
        }
    }

    private static List<ImmutablePair<String, String>> getAvailableThemes() {
        synchronized (availableThemesMutex) {
            //make a copy to be thread-safe
            return new ArrayList<>(availableThemes);
        }
    }

    private static void addAvailableThemes(@NonNull final File dir, final List<ImmutablePair<String, String>> themes, final String prefix) {

        for (File candidate : Objects.requireNonNull(dir.listFiles())) {
            if (candidate.isDirectory()) {
                addAvailableThemes(candidate, themes, prefix + candidate.getName() + "/");
            } else if (candidate.getName().endsWith(".xml")) {
                final String themeId = prefix + candidate.getName();
                themes.add(new ImmutablePair<>(themeId, toUserDisplayableName(candidate, null)));
                themes.add(new ImmutablePair<>(CONTENT_THEMEID_PREFIX + themeId,
                    CONTENT_THEMEID_PREFIX_USER_DISPLAY + toUserDisplayableName(candidate, null)));

            } else if (candidate.getName().endsWith(".zip")) {
                try {
                    for (String zipXmlTheme : ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new FileInputStream(candidate)))) {
                        final String themeId = prefix + candidate.getName() + ZIP_THEME_SEPARATOR + zipXmlTheme;
                        themes.add(new ImmutablePair<>(themeId, toUserDisplayableName(candidate, zipXmlTheme)));
                        themes.add(new ImmutablePair<>(CONTENT_THEMEID_PREFIX + themeId,
                            CONTENT_THEMEID_PREFIX_USER_DISPLAY + toUserDisplayableName(candidate, zipXmlTheme)));
                    }
                } catch (IOException ioe) {
                    Log.w("Map Theme ZIP '" + candidate + "' could not be read", ioe);
                }
            }
        }
    }

    /**
     * Calculates a xml theme name fit for user display (in dropdown etc)
     * @param file theme file name
     * @param zipPath if theme file is a ZIP, then this contains the zip-internal path to the xml. Otherwise null
     * @return user display theme name
     */
    private static String toUserDisplayableName(final File file, final String zipPath) {
        String userDisplay = StringUtils.removeEnd(file.getName(), ".xml");
        if (zipPath != null) {
            final int idx = zipPath.lastIndexOf("/");
            userDisplay = userDisplay + "/" + StringUtils.removeEnd(idx < 0 ? zipPath : zipPath.substring(idx + 1), ".xml");
        }
        return userDisplay;
    }

    private static class MapThemeFolderSyncTask extends AsyncTask<Void, Void, FolderUtils.FolderProcessResult> {

        private final Folder source;
        private final File target;
        private final Consumer<FolderUtils.FolderProcessResult> callback;

        private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

        private final Object requestRedoMutex = new Object();
        private boolean taskIsDone = false;
        private boolean requestRedo = false;
        private long startTime = System.currentTimeMillis();

        public static MapThemeFolderSyncTask createAndExecute(final Folder source, final File target, final Consumer<FolderUtils.FolderProcessResult> callback) {
            final MapThemeFolderSyncTask task = new MapThemeFolderSyncTask(source, target, callback);
            task.execute();

            return task;
        }

        private MapThemeFolderSyncTask(final Folder source, final File target, final Consumer<FolderUtils.FolderProcessResult> callback) {
            this.source = source;
            this.target = target;
            this.callback = callback;
        }

        /** Requests for a running task to redo sync after finished. May fail if task is already done, but in this case the task may safely be discarted */
        public boolean requestRedo() {
            synchronized (requestRedoMutex) {
                if (taskIsDone) {
                    return false;
                }
                cancelFlag.set(true);
                requestRedo = true;
                startTime = System.currentTimeMillis();
                return true;
            }

        }

        @Override
        protected FolderUtils.FolderProcessResult doInBackground(final Void[] params) {
            while (true) {
                final FolderUtils.FolderProcessResult result = FolderUtils.get().synchronizeFolder(source, target, cancelFlag, null);
                synchronized (requestRedoMutex) {
                    if (requestRedo) {
                        cancelFlag.set(false);
                    } else {
                        taskIsDone = true;
                        return result;
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(final FolderUtils.FolderProcessResult result) {
            //show toast only if something actually happened
            if (result.filesModified > 0) {
                showToast(R.string.mapthemes_foldersync_finished_toast,
                    LocalizationUtils.getString(R.string.persistablefolder_offline_maps_themes),
                    Formatter.formatDuration(System.currentTimeMillis() - startTime),
                    result.filesModified, LocalizationUtils.getPlural(R.plurals.file_count, result.filesInSource, "file(s)"));
            }
            if (callback != null) {
                callback.accept(result);
            }
        }

        private static void showToast(final int resId, final Object ... params) {
            final ImmutablePair<String, String> msgs = LocalizationUtils.getMultiPurposeString(resId, "RenderTheme", params);
            ActivityMixin.showApplicationToast(msgs.left);
            Log.iForce("[RenderThemeHelper.ThemeFolderSyncTask]" + msgs.right);
        }

    }

    private static ContentResolver getContentResolver() {
        return CgeoApplication.getInstance().getContentResolver();
    }

}

