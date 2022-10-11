package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ContextLogger;
import static cgeo.geocaching.storage.Folder.CGEO_PRIVATE_FILES;
import static cgeo.geocaching.storage.Folder.LEGACY_CGEO_PUBLIC_ROOT;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Instances of this enum represent applicaton folders whose state is persisted.
 */
public enum PersistableFolder {

    /**
     * Base directory
     */
    BASE(R.string.pref_persistablefolder_basedir, R.string.persistablefolder_base, LEGACY_CGEO_PUBLIC_ROOT),

    /**
     * Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps)
     */
    //legacy setting: "mapDirectory", a pure file path is stored
    OFFLINE_MAPS(R.string.pref_persistablefolder_offlinemaps, R.string.persistablefolder_offline_maps, Folder.fromPersistableFolder(BASE, "maps")),
    /**
     * Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data
     */
    //legacy setting: "renderthemepath", a pure file path is stored
    OFFLINE_MAP_THEMES(R.string.pref_persistablefolder_offlinemapthemes, R.string.persistablefolder_offline_maps_themes, Folder.fromPersistableFolder(OFFLINE_MAPS, "_themes")),
    /** Offline Maps: optional folder for map shading files (dem) */
    OFFLINE_MAP_SHADING(R.string.pref_persistablefolder_offlinemapshading, R.string.persistablefolder_offline_maps_shading, Folder.fromPersistableFolder(OFFLINE_MAPS, "_hgt")),
    /**
     * Target folder for written logfiles
     */
    LOGFILES(R.string.pref_persistablefolder_logfiles, R.string.persistablefolder_logfiles, Folder.fromPersistableFolder(BASE, "logfiles")),
    /**
     * GPX Files
     */
    GPX(R.string.pref_persistablefolder_gpx, R.string.persistablefolder_gpx, Folder.fromPersistableFolder(BASE, "gpx")),
    /**
     * Backup storage folder
     */
    BACKUP(R.string.pref_persistablefolder_backup, R.string.persistablefolder_backup, Folder.fromPersistableFolder(BASE, "backup")),
    /**
     * Field Note folder
     */
    FIELD_NOTES(R.string.pref_persistablefolder_fieldnotes, R.string.persistablefolder_fieldnotes, Folder.fromPersistableFolder(BASE, "field-notes")),
    ///** Spoiler Image folder */
    SPOILER_IMAGES(R.string.pref_persistablefolder_spoilerimages, R.string.persistablefolder_spoilerimages, Folder.fromPersistableFolder(BASE, "GeocachePhotos")),
    /**
     * Routing base & config folder
     */
    ROUTING_BASE(R.string.pref_persistablefolder_routingbase, R.string.persistablefolder_routingbase, Folder.fromPersistableFolder(BASE, "routing")),
    /**
     * Routing tile files
     */
    ROUTING_TILES(R.string.pref_persistablefolder_routingtiles, R.string.persistablefolder_routingtiles, Folder.fromPersistableFolder(ROUTING_BASE, "segments4")),

    /**
     * A Folder to use solely for Unit Test
     */
    TEST_FOLDER(R.string.pref_persistablefolder_testdir, 0, Folder.fromFolder(CGEO_PRIVATE_FILES, "unittest"));

    @AnyRes
    private final int prefKeyId;
    @AnyRes
    private final int nameKeyId;
    private final boolean needsWrite;

    private final Folder[] defaultFolderCandidates;

    private Folder userDefinedFolder;
    private Folder defaultFolder;
    private boolean defaultFolderInitialized;

    private final WeakHashMap<Object, List<Consumer<PersistableFolder>>> changeListeners = new WeakHashMap<>();

    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    @StringRes
    public int getNameKeyId() {
        return nameKeyId;
    }

    PersistableFolder(@AnyRes final int prefKeyId, @AnyRes final int nameKeyId, @NonNull final Folder... defaultFolderCandidates) {
        this.prefKeyId = prefKeyId;
        this.nameKeyId = nameKeyId;
        this.needsWrite = true;

        this.defaultFolderCandidates = defaultFolderCandidates;
        this.defaultFolder = this.defaultFolderCandidates[0];
        this.defaultFolderInitialized = false;
        registerForIndirectChange(this.defaultFolder);

        //read current user-defined location from settings.
        this.userDefinedFolder = Folder.fromConfig(Settings.getPersistableFolder(this));
    }

    private void registerForIndirectChange(final Folder folder) {
        //if this PersistableFolder's value is based on another PersistableFolder, then we have to notify on  indirect change
        final PersistableFolder rootPersistableFolder = folder.getRootPersistableFolder();
        if (rootPersistableFolder != null) {
            rootPersistableFolder.registerChangeListener(this, pf -> notifyChanged());
        }
    }

    /**
     * registers a listener which is fired each time the actual location of this folder changes
     */
    public void registerChangeListener(final Object lifecycleRef, final Consumer<PersistableFolder> listener) {
        List<Consumer<PersistableFolder>> listeners = changeListeners.get(lifecycleRef);
        if (listeners == null) {
            listeners = new ArrayList<>();
            changeListeners.put(lifecycleRef, listeners);
        }
        listeners.add(listener);
    }

    private void notifyChanged() {
        for (List<Consumer<PersistableFolder>> list : this.changeListeners.values()) {
            for (Consumer<PersistableFolder> listener : list) {
                listener.accept(this);
            }
        }
    }

    /**
     * The folder this {@link PersistableFolder} currently points to
     */
    public Folder getFolder() {
        return this.userDefinedFolder == null ? getDefaultFolder() : this.userDefinedFolder;
    }

    /**
     * The (FolderType-specific) Uri this {@link PersistableFolder} currently points to
     */
    public Uri getUri() {
        return ContentStorage.get().getUriForFolder(getFolder());
    }

    public Uri getUriForFolder(final Folder folder) {
        return ContentStorage.get().getUriForFolder(folder);
    }

    public void reevaluateDefaultFolder() {
        this.defaultFolder = ContentStorage.get().getAccessibleDefaultFolder(this.defaultFolderCandidates, needsWrite(), name());
        defaultFolderInitialized = true;
        registerForIndirectChange(this.defaultFolder);
    }

    public Folder getDefaultFolder() {
        if (!defaultFolderInitialized) {
            reevaluateDefaultFolder();
            defaultFolderInitialized = true;
        }
        return this.defaultFolder;
    }

    /**
     * reevaluate folder defaults. Careful, this might run several seconds!
     */
    public static void reevaluateDefaultFolders() {
        try (ContextLogger ignored = new ContextLogger(true, "PersistableFolder.reevaluateDefaultFolders")) {
            for (PersistableFolder folder : PersistableFolder.values()) {
                folder.reevaluateDefaultFolder();
            }
        }
    }

    public boolean isUserDefined() {
        return this.userDefinedFolder != null;
    }

    /**
     * Sets a new user-defined location (or "null" if default shall be used). Should be called ONLY by {@link ContentStorage}
     */
    protected void setUserDefinedFolder(@Nullable final Folder userDefinedFolder, final boolean setByUser) {
        this.userDefinedFolder = userDefinedFolder;
        Settings.setPersistableFolder(this, userDefinedFolder == null ? null : userDefinedFolder.toConfig(), setByUser);
        notifyChanged();
    }

    /**
     * Returns an internationalized ID for this folder (e.g. "GPX" or "Offline Maps"). Does not return the actual value/path (see {@link #toUserDisplayableValue()} for that)
     */
    @NonNull
    public String toUserDisplayableName() {
        if (this.nameKeyId == 0 || CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            return name();
        }
        return CgeoApplication.getInstance().getApplicationContext().getString(this.nameKeyId);
    }

    /**
     * Returns a representation of this folder's location fit to show to an end user
     */
    @NonNull
    public String toUserDisplayableValue() {
        return toUserDisplayableValue(false);
    }

    /**
     * Returns a representation of this folder's location fit to show to an end user
     */
    @NonNull
    public String toUserDisplayableValue(final boolean forceEnglish) {
        String result = getFolder().toUserDisplayableString(true, forceEnglish);

        result += " (";
        if (forceEnglish || CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            result += isUserDefined() ? "User-Defined" : "Default";
        } else {
            final Context ctx = CgeoApplication.getInstance().getApplicationContext();
            result += isUserDefined() ? ctx.getString(R.string.persistablefolder_usertype_userdefined) : ctx.getString(R.string.persistablefolder_usertype_default);
        }
        result += ")";
        return result;
    }

    /**
     * Returns whether this persistable folder currently points to a legacy (=FILE-based) folder
     */
    public boolean isLegacy() {
        return getFolder().getBaseType() == Folder.FolderType.FILE;
    }

    public boolean needsWrite() {
        return needsWrite;
    }

    @Override
    public String toString() {
        return name() + ": " + toUserDisplayableValue(true) + "[" + getFolder() + "]";
    }

}
