package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.storage.Folder.CGEO_PRIVATE_FILES;
import static cgeo.geocaching.storage.Folder.DOCUMENTS_FOLDER_DEPRECATED;

import android.content.Context;
import android.net.Uri;

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
public enum PublicLocalFolder {

    /** Base directory  */
    BASE (R.string.pref_publicfolder_basedir, Folder.fromFolder(DOCUMENTS_FOLDER_DEPRECATED, "cgeo")),

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    OFFLINE_MAPS(R.string.pref_publicfolder_offlinemaps, Folder.fromPublicFolder(BASE, "maps")),
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    OFFLINE_MAP_THEMES(R.string.pref_publicfolder_offlinemapthemes, Folder.fromPublicFolder(BASE, "themes")),
    /** Target folder for written logfiles */
    LOGFILES(R.string.pref_publicfolder_logfiles, Folder.fromPublicFolder(BASE, "logfiles")),

    /** A Folder to use solely for Unit Test */
    TEST_FOLDER(R.string.pref_publicfolder_testdir, Folder.fromFolder(CGEO_PRIVATE_FILES,  "unittest"));

    @AnyRes
    private final int prefKeyId;
    private final boolean needsWrite;

    private final Folder defaultLocation;

    private Folder userDefinedLocation;

    private final WeakHashMap<Object, List<Consumer<PublicLocalFolder>>> changeListeners = new WeakHashMap<>();


    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    PublicLocalFolder(@AnyRes final int prefKeyId, @NonNull final Folder defaultLocation) {
        this.prefKeyId = prefKeyId;
        this.needsWrite = true;

        this.defaultLocation = defaultLocation;

        //read current user-defined location from settings.
        this.userDefinedLocation = Folder.fromDocumentUri(Settings.getPublicLocalFolderUri(this));

        //if this PublicLocalFolder's value is based on another publiclocalfolder, then we have to notify on  indirect change
        final PublicLocalFolder rootPublicFolder = defaultLocation.getRootPublicFolder();
        if (rootPublicFolder != null) {
            rootPublicFolder.addChangeListener(this, pf -> notifyChanged());
        }
    }

    public void addChangeListener(final Object lifecycleRef, final Consumer<PublicLocalFolder> listener) {
        List<Consumer<PublicLocalFolder>> listeners = changeListeners.get(lifecycleRef);
        if (listeners == null) {
            listeners = new ArrayList<>();
            changeListeners.put(lifecycleRef, listeners);
        }
        listeners.add(listener);
    }

    private void notifyChanged() {
        for (List<Consumer<PublicLocalFolder>> list : this.changeListeners.values()) {
            for (Consumer<PublicLocalFolder> listener :list) {
                listener.accept(this);
            }
        }
    }

    public Folder getLocation() {
        return this.userDefinedLocation == null ? this.defaultLocation : this.userDefinedLocation;
    }

    public Folder getDefaultLocation() {
        return this.defaultLocation;
    }

    public boolean isUserDefinedLocation() {
        return this.userDefinedLocation != null;
    }

    /** Sets a new user-defined location (or "null" if default shall be used). Should be called ONLY by {@link PublicLocalStorage} */
    protected void setUserDefinedLocation(@Nullable final Uri userDefinedUri) {
        this.userDefinedLocation = Folder.fromDocumentUri(userDefinedUri);
        Settings.setPublicLocalFolderUri(this, userDefinedUri);
        notifyChanged();
    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String toUserDisplayableString() {
        String result = getLocation().toUserDisplayableString();

        result += " (";
        if (CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            result += isUserDefinedLocation() ? "User-Defined" : "Default";
        } else {
            final Context ctx = CgeoApplication.getInstance().getApplicationContext();
            result += isUserDefinedLocation() ? ctx.getString(R.string.publiclocalstorage_userdefined) : ctx.getString(R.string.publiclocalstorage_default);
        }
        result += ")";
        return result;
    }

    public boolean needsWrite() {
        return needsWrite;
    }

    @Override
    public String toString() {
        return name() + ": " + toUserDisplayableString() + "[" + getLocation() + "]";
    }

}
