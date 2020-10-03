package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.storage.Folder.CGEO_PRIVATE_FILES;
import static cgeo.geocaching.storage.Folder.DOCUMENTS_FOLDER_DEPRECATED;
import static cgeo.geocaching.storage.Folder.LEGACY_CGEO_PUBLIC_ROOT;

import android.content.Context;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Instances of this class represent an application-relevant folder.
 */
public enum ConfigurableFolder {

    /** Base directory  */
    BASE (R.string.pref_configurablefolder_basedir, R.string.configurablefolder_base, LEGACY_CGEO_PUBLIC_ROOT, Folder.fromFolder(DOCUMENTS_FOLDER_DEPRECATED, "cgeo")),

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    //legacy setting: "mapDirectory", a pure file path is stored
    OFFLINE_MAPS(R.string.pref_configurablefolder_offlinemaps, R.string.configurablefolder_offline_maps, Folder.fromConfigurableFolder(BASE, "maps")),
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    //legacy setting: "renderthemepath", a pure file path is stored
    OFFLINE_MAP_THEMES(R.string.pref_configurablefolder_offlinemapthemes, R.string.configurablefolder_offline_maps_themes, Folder.fromConfigurableFolder(BASE, "themes")),
    /** Target folder for written logfiles */
    LOGFILES(R.string.pref_configurablefolder_logfiles, R.string.configurablefolder_logfiles, Folder.fromConfigurableFolder(BASE, "logfiles")),
    /** GPX Files */
    GPX(R.string.pref_configurablefolder_gpx, R.string.configurablefolder_gpx, Folder.fromConfigurableFolder(BASE, "gpx")),
    /** Backup storage folder */
    BACKUP(R.string.pref_configurablefolder_backup, R.string.configurablefolder_backup, Folder.fromConfigurableFolder(BASE, "backup")),
    /** Field Note folder */
    FIELD_NOTES(R.string.pref_configurablefolder_fieldnotes, R.string.configurablefolder_fieldnotes, Folder.fromConfigurableFolder(BASE, "field-notes")),
    /** (Log) Image folder) */
    //IMAGES(R.string.pref_configurablefolder_images, R.string.configurablefolder_images, Folder.fromconfigurablefolder(BASE, "images")),

    /** A Folder to use solely for Unit Test */
    TEST_FOLDER(R.string.pref_configurablefolder_testdir, 0, Folder.fromFolder(CGEO_PRIVATE_FILES,  "unittest"));

    @AnyRes
    private final int prefKeyId;
    @AnyRes
    private final int nameKeyId;
    private final boolean needsWrite;

    private Folder userDefinedFolder;
    private final Folder defaultFolder;

    private final WeakHashMap<Object, List<Consumer<ConfigurableFolder>>> changeListeners = new WeakHashMap<>();


    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    ConfigurableFolder(@AnyRes final int prefKeyId, @AnyRes final int nameKeyId, @NonNull final Folder ... defaultFolderCandidates) {
        this.prefKeyId = prefKeyId;
        this.nameKeyId = nameKeyId;
        this.needsWrite = true;

        this.defaultFolder = getAccessibleDefaultFolder(defaultFolderCandidates);

        //read current user-defined location from settings.
        this.userDefinedFolder = Folder.fromConfig(Settings.getConfigurableFolder(this));

        //if this ConfigurableFolder's value is based on another ConfigurableFolder, then we have to notify on  indirect change
        final ConfigurableFolder rootconfigurablefolder = defaultFolder.getRootConfigurableFolder();
        if (rootconfigurablefolder != null) {
            rootconfigurablefolder.registerChangeListener(this, pf -> notifyChanged());
        }
    }

    /** registers a listener which is fired each time the actual location of this folder changes */
    public void registerChangeListener(final Object lifecycleRef, final Consumer<ConfigurableFolder> listener) {
        List<Consumer<ConfigurableFolder>> listeners = changeListeners.get(lifecycleRef);
        if (listeners == null) {
            listeners = new ArrayList<>();
            changeListeners.put(lifecycleRef, listeners);
        }
        listeners.add(listener);
    }

    private void notifyChanged() {
        for (List<Consumer<ConfigurableFolder>> list : this.changeListeners.values()) {
            for (Consumer<ConfigurableFolder> listener :list) {
                listener.accept(this);
            }
        }
    }

    public Folder getFolder() {
        return this.userDefinedFolder == null ? this.defaultFolder : this.userDefinedFolder;
    }

    public Folder getDefaultFolder() {
        return this.defaultFolder;
    }

    public boolean isUserDefined() {
        return this.userDefinedFolder != null;
    }

    /** Sets a new user-defined location (or "null" if default shall be used). Should be called ONLY by {@link FolderStorage} */
    protected void setUserDefinedFolder(@Nullable final Folder userDefinedFolder) {
        this.userDefinedFolder = userDefinedFolder;
        Settings.setConfigurableFolder(this, userDefinedFolder == null ? null : userDefinedFolder.toConfig());
        notifyChanged();
    }

    private Folder getAccessibleDefaultFolder(final Folder[] candidates) {

        for (Folder candidate : candidates) {
            //candidate is ok if it is either directly accessible or based on another public folder (which will become accessible later)
            if (candidate != null && (candidate.getRootConfigurableFolder() != null || FolderStorage.get().checkAvailability(candidate, needsWrite()))) {
                return candidate;
            }
        }

        return Folder.fromFolder(CGEO_PRIVATE_FILES, "public/" + name());
    }

    @NonNull
    public String toUserDisplayableName() {
        if (this.nameKeyId == 0 || CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            return name();
        }
        return CgeoApplication.getInstance().getApplicationContext().getString(this.nameKeyId);
    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String toUserDisplayableValue() {
        String result = getFolder().toUserDisplayableString();

        result += " (";
        if (CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            result += isUserDefined() ? "User-Defined" : "Default";
        } else {
            final Context ctx = CgeoApplication.getInstance().getApplicationContext();
            result += isUserDefined() ? ctx.getString(R.string.configurablefolder_usertype_userdefined) : ctx.getString(R.string.configurablefolder_usertype_default);
        }
        result += ")";
        return result;
    }

    public boolean needsWrite() {
        return needsWrite;
    }

    @Override
    public String toString() {
        return name() + ": " + toUserDisplayableValue() + "[" + getFolder() + "]";
    }

}
