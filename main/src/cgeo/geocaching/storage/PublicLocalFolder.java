package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.File;



/**
 * Instances of this class represent an application-relevant folder.
 */
public enum PublicLocalFolder {

    /** Base directory  */
    BASE (R.string.pref_publicfolder_basedir, FolderLocation.fromFile(new File(FolderLocation.DOCUMENTS_FOLDER, "cgeo"))),

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    OFFLINE_MAPS(R.string.pref_publicfolder_offlinemaps, FolderLocation.fromSubfolder(BASE, "maps"), pf -> {
        MapsforgeMapProvider.getInstance().updateOfflineMaps();
    }),
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    OFFLINE_MAP_THEMES(R.string.pref_publicfolder_offlinemapthemes, FolderLocation.fromSubfolder(BASE, "themes")),
    /** Target folder for written logfiles */
    LOGFILES(R.string.pref_publicfolder_logfiles, FolderLocation.fromSubfolder(BASE, "logfiles"));

    @AnyRes
    private final int prefKeyId;
    private final boolean needsWrite;

    private final FolderLocation defaultLocation;
    private final Consumer<PublicLocalFolder> userDefinedLocationChangedCallback;

    private FolderLocation userDefinedLocation;


    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    PublicLocalFolder(@AnyRes final int prefKeyId, final FolderLocation defaultLocation) {
        this(prefKeyId, defaultLocation, null);
    }

    PublicLocalFolder(@AnyRes final int prefKeyId, final FolderLocation defaultLocation, final Consumer<PublicLocalFolder> userDefinedLocationChangedCallback) {

        this.prefKeyId = prefKeyId;
        this.needsWrite = true;

        this.defaultLocation = defaultLocation;
        this.userDefinedLocationChangedCallback = userDefinedLocationChangedCallback;

        //read current user-defined location from settings.
        this.userDefinedLocation = FolderLocation.fromDocumentUri(Settings.getPublicLocalFolderUri(this));
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

    public void setUserDefinedLocation(final Uri userDefinedUri) {
        this.userDefinedLocation = FolderLocation.fromDocumentUri(userDefinedUri);
        Settings.setPublicLocalFolderUri(this, userDefinedUri);

        if (this.userDefinedLocationChangedCallback != null) {
            this.userDefinedLocationChangedCallback.accept(this);
        }
    }
    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String getUserDisplayableName() {
        String result = getLocation().getUserDisplayableName();

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
        return name() + ": " + getUserDisplayableName() + "[" + getLocation() + "]";
    }

}
