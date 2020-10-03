package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;



/**
 * This class represents a concrete definite folder location.
 *
 * Folder locations can have different types as defined by {@link FolderType}.
 * Depending on that type, different attributes are filled and different handling is necessary
 * to interact with actual data in that folder. Those different handlings are implemented by {@link PublicLocalStorage}.
 *
 * Instances of this class are immutable. They can be serialized/deserialized to/from a String.
 *
 * Instances support the usage in Maps ({@link #equals(Object)} and {@link #hashCode()}). Note however that
 * two FolderLocation instances pointing to the same actual folder on disk but using two different FolderTypes
 * are NOT considered equal!
 */
public class FolderLocation {


    public enum FolderType {
        /** a 'classic' folder based on a file */
        FILE,
        /** Folder based on Storage Access Frameworks and retrieved by {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} */
        DOCUMENT,
        /** A subfolder of another FolderLocation */
        SUBFOLDER,
    }

    //some base file locations for usage

    /** cGeo's private internal Files directory */
    public static final FolderLocation CGEO_PRIVATE_FILES = FolderLocation.fromFile(CgeoApplication.getInstance().getApplicationContext().getFilesDir());

    /** Root folder for documents (deprecated since API29 but still works somehow) */
    public static final FolderLocation DOCUMENTS_FOLDER_DEPRECATED = FolderLocation.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));


    private static final String EMPTY = "---";
    private static final String CONFIG_SEP = "::";

    private final FolderType type;
    private final Uri uri;

    private final PublicLocalFolder parentPublicFolder; //needed for type SUBFOLDER
    private final FolderLocation parentLocation; //needed for type SUBFOLDER
    private final String subfolder; //needed for type SUBFOLDER

    //Document File corresponding to this FolderLocation. Can be cached here by PublicLocaStorage
    private  DocumentFile cachedDocFile;


    private FolderLocation(final FolderType type, final Uri uri, final PublicLocalFolder parentPublicFolder, final FolderLocation parentLocation, final String subfolder) {
        this.type = type;
        this.uri = uri;

        this.parentPublicFolder = parentPublicFolder;
        this.parentLocation = parentLocation;
        this.subfolder = toFolderName(subfolder);

        //if this location is based on a publiclocal folder, then we have to invalidate cached docfile on change
        final PublicLocalFolder rootPublicFolder = getRootPublicFolder();
        if (rootPublicFolder != null) {
            rootPublicFolder.addChangeListener(pf -> setCachedDocFile(null));
        }
    }


    public FolderType getType() {
        return type;
    }

    public FolderType getBaseType() {
        if (type.equals(FolderType.SUBFOLDER)) {
            return getParentLocation().getBaseType();
        }
        return type;
    }

    /** Uri associated with this folder */
    @Nullable
    public Uri getUri() {
        if (this.type.equals(FolderType.SUBFOLDER)) {
            return Uri.withAppendedPath(getParentLocation().getUri(), this.subfolder);
        }
        return this.uri;
    }

    /** The base Uri (below all subfolders)) */
    @NonNull
    public Uri getBaseUri() {
        switch (this.type) {
            case SUBFOLDER:
                return getParentLocation().getBaseUri();
            case DOCUMENT:
            case FILE:
            default:
                return getUri();
        }
    }

    public List<String> getSubdirsToBase() {
        if (!getType().equals(FolderType.SUBFOLDER)) {
            //important: return a newly created list here, NOT Collections.emptyList()! Reason: later we will write to this List
            return new ArrayList<>();
        }
        final List<String > result = getParentLocation().getSubdirsToBase();
        result.add(subfolder);
        return result;
    }

    /** If this instance is a subfolder based on a publiclocalfolder, then this publiclocalfolder is returned. Otherwise null is returned */
    @Nullable
    public PublicLocalFolder getRootPublicFolder() {
        if (this.parentPublicFolder != null) {
            return this.parentPublicFolder;
        }

        if (this.parentLocation != null) {
            return this.parentLocation.getRootPublicFolder();
        }

        return null;
    }


    //shall only be used by PublicLocalStorage
    protected DocumentFile getCachedDocFile() {
        return this.cachedDocFile;
    }

    //shall only be used by PublicLocalStorage
    protected void setCachedDocFile(final DocumentFile cacheDocFile) {
        this.cachedDocFile = cacheDocFile;
    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String toUserDisplayableString() {
        return getUri() == null ? EMPTY : UriUtils.toUserDisplayableString(getUri());
    }

    public static FolderLocation fromFile(final File file) {
        if (file == null) {
            return null;
        }
        return new FolderLocation(FolderType.FILE, Uri.fromFile(file), null, null, null);
    }

    @Nullable
    public static FolderLocation fromDocumentUri(final Uri uri) {
        if (uri == null) {
            return null;
        }
        return new FolderLocation(FolderType.DOCUMENT, uri, null, null, null);
    }

    public static FolderLocation fromFolderLocation(final FolderLocation folderLocation, final String subfolder) {
        if (folderLocation == null) {
            return null;
        }
        return new FolderLocation(FolderType.SUBFOLDER, null, null, folderLocation, subfolder);
    }

    public static FolderLocation fromPublicFolder(final PublicLocalFolder publicLocalFolder, final String subfolder) {
        if (publicLocalFolder == null) {
            return null;
        }
        return new FolderLocation(FolderType.SUBFOLDER, null, publicLocalFolder, null, subfolder);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FolderLocation)) {
            return false;
        }

        return this.toConfig(true).equals(((FolderLocation) other).toConfig(true));
    }

    @Override
    public int hashCode() {
        return this.toConfig(true).hashCode();
    }

    private FolderLocation getParentLocation() {
        if (this.parentPublicFolder != null) {
            return this.parentPublicFolder.getLocation();
        }
        return this.parentLocation;
    }

    private String toConfig(final boolean unifiedUri) {

        final StringBuilder configString = new StringBuilder(type.toString()).append(CONFIG_SEP);
        switch (type) {
            case SUBFOLDER:
                if (this.parentPublicFolder != null) {
                    configString.append(this.parentPublicFolder.name());
                } else {
                    configString.append("FL").append(CONFIG_SEP).append(this.parentLocation.getUri());
                }
                configString.append(CONFIG_SEP).append(this.subfolder);
                break;
            case FILE:
            case DOCUMENT:
            default:
                if (unifiedUri) {
                    configString.append(UriUtils.toStringDecoded(uri));
                } else {
                    configString.append(uri);
                }
                break;
        }
        return configString.toString();
    }

    @NotNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(toUserDisplayableString())
            .append("[")
            .append(toConfig(false));

        final PublicLocalFolder rootPublicLocalFolder = getRootPublicFolder();
        if (rootPublicLocalFolder != null) {
            sb.append(",based on:").append(rootPublicLocalFolder.name());
        }

        return sb.append("]").toString();
    }

    @NonNull
    private static String toFolderName(final String name) {
        String folderName = name == null ? "default" : name.replaceAll("[^a-zA-Z0-9-_.]", "-").trim();
        if (StringUtils.isBlank(folderName)) {
            folderName = "default";
        }
        return folderName;
    }
}
