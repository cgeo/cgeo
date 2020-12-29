package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.storage.FolderLocation.DOCUMENTS_FOLDER_DEPRECATED;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Instances of this class represent an application-relevant folder.
 */
public enum PublicLocalFolder {

    /** Base directory  */
    BASE (R.string.pref_publicfolder_basedir, FolderLocation.fromFolderLocation(DOCUMENTS_FOLDER_DEPRECATED, "cgeo")),

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    OFFLINE_MAPS(R.string.pref_publicfolder_offlinemaps, FolderLocation.fromPublicFolder(BASE, "maps")),
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    OFFLINE_MAP_THEMES(R.string.pref_publicfolder_offlinemapthemes, FolderLocation.fromPublicFolder(BASE, "themes")),
    /** Target folder for written logfiles */
    LOGFILES(R.string.pref_publicfolder_logfiles, FolderLocation.fromPublicFolder(BASE, "logfiles"));

    @AnyRes
    private final int prefKeyId;
    private final boolean needsWrite;

    private final FolderLocation defaultLocation;

    private FolderLocation userDefinedLocation;

    private final List<WeakReference<Consumer<PublicLocalFolder>>> changeListeners = new LinkedList<>();


    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    PublicLocalFolder(@AnyRes final int prefKeyId, @NonNull final FolderLocation defaultLocation) {
        this.prefKeyId = prefKeyId;
        this.needsWrite = true;

        this.defaultLocation = defaultLocation;

        //read current user-defined location from settings.
        this.userDefinedLocation = FolderLocation.fromDocumentUri(Settings.getPublicLocalFolderUri(this));

        //if this PublicLocalFolder's value is based on another publiclocalfolder, then we have to notify on  indirect change
        final PublicLocalFolder rootPublicFolder = defaultLocation.getRootPublicFolder();
        if (rootPublicFolder != null) {
            rootPublicFolder.addChangeListener(pf -> notifyChanged());
        }
    }

    public void addChangeListener(final Consumer<PublicLocalFolder> listener) {
        changeListeners.add(new WeakReference<>(listener));
    }

    private void notifyChanged() {
        final Iterator<WeakReference<Consumer<PublicLocalFolder>>> it = changeListeners.iterator();
        while (it.hasNext()) {
            final WeakReference<Consumer<PublicLocalFolder>> listener = it.next();
            if (listener.get() == null) {
                //while we're at it, we also cleanup entries where reference got lost
                it.remove();
            } else {
                listener.get().accept(this);
            }
        }
    }

    public FolderLocation getLocation() {
        return this.userDefinedLocation == null ? this.defaultLocation : this.userDefinedLocation;
    }

    public FolderLocation getDefaultLocation() {
        return this.defaultLocation;
    }

    public boolean isUserDefinedLocation() {
        return this.userDefinedLocation != null;
    }

    /** Sets a new user-defined location (or "null" if default shall be used). Should be called ONLY by {@link PublicLocalStorage} */
    void setUserDefinedLocation(@Nullable final Uri userDefinedUri) {
        this.userDefinedLocation = FolderLocation.fromDocumentUri(userDefinedUri);
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
