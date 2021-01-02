package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a concrete definite folder / directory on disk.
 *
 * Folders can have different types as defined by {@link FolderType}.
 * Depending on that type, different attributes are filled and different handling is necessary
 * to interact with actual data in that folder. Those different handlings are implemented by {@link PublicLocalStorage}.
 *
 * Instances of this class are immutable. They can be serialized/deserialized to/from a String.
 *
 * Instances support the usage in Maps ({@link #equals(Object)} and {@link #hashCode()}). Note however that
 * two FolderLocation instances pointing to the same actual folder on disk but using two different FolderTypes
 * are NOT considered equal!
 */
public class Folder {


    public enum FolderType {
        /** a 'classic' folder based on a file */
        FILE,
        /** Folder based on Storage Access Frameworks and retrieved by {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} */
        DOCUMENT,
        /** A Folder based on a PublicLocaFolder */
        PUBLIC_FOLDER,
    }

    /** cGeo's private internal Files directory */
    public static final Folder CGEO_PRIVATE_FILES = Folder.fromFile(CgeoApplication.getInstance().getApplicationContext().getFilesDir());

    /** Root folder for documents (deprecated since API29 but still works somehow) */
    public static final Folder DOCUMENTS_FOLDER_DEPRECATED = Folder.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));

    /** Legacy public root folder of c:geo until API29 (will no longer work in API30) */
    public static final Folder LEGACY_CGEO_PUBLIC_ROOT = Folder.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "cgeo"));

    private static final String EMPTY = "---";
    private static final String CONFIG_SEP = "::";

    private final FolderType type;
    private final Uri uri;

    private final PublicLocalFolder publicFolder; //needed for type PUBLIC_FOLDER

    private final List<String> subfolders; //each type may have subfolders
    private final String subfolderString;

    private Folder(final FolderType type, final Uri uri, final PublicLocalFolder publicFolder, final List<String> subfolders) {
        this.type = type;
        this.uri = uri;

        this.publicFolder = publicFolder;

        this.subfolders = subfolders == null ? Collections.emptyList() : subfolders;
        this.subfolderString = CollectionStream.of(this.subfolders).toJoinedString("/");

    }

    /** registers a listener which is fired each time the actual location of this folder changes */
    public void registerChangeListener(final Object lifecycleRef, final Consumer<PublicLocalFolder> listener) {

        //currently, this folders location can only change if it is based on a Public Folder
        if (getRootPublicFolder() != null) {
            getRootPublicFolder().registerChangeListener(lifecycleRef, listener);
        }
    }


    public FolderType getType() {
        return type;
    }

    /** Uri associated with this folder */
    @NonNull
    public Uri getUri() {
        final Uri uri = publicFolder != null ? publicFolder.getFolder().getUri() : this.uri;
        return Uri.withAppendedPath(uri, this.subfolderString);
    }

    /** The base Uri (below all subfolders)) */
    @NonNull
    public Uri getBaseUri() {
        return publicFolder != null ? publicFolder.getFolder().getBaseUri() : this.uri;
    }

    @NonNull
    public FolderType getBaseType() {
        if (publicFolder != null) {
            return publicFolder.getFolder().getBaseType();
        }
        return getType();
    }

    public List<String> getSubdirsToBase() {
        final List<String > result = publicFolder != null ? publicFolder.getFolder().getSubdirsToBase() : new ArrayList<>();
        result.addAll(subfolders);
        return result;
    }

    public String getSubdirsToBaseAsString() {
        return CollectionStream.of(getSubdirsToBase()).toJoinedString("/");
    }

    /** If this instance is a subfolder based on a publiclocalfolder, then this publiclocalfolder is returned. Otherwise null is returned */
    @Nullable
    public PublicLocalFolder getRootPublicFolder() {
        return publicFolder;
    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String toUserDisplayableString() {
        return getUri() == null ? EMPTY : UriUtils.toUserDisplayableString(getUri());
    }

    @Nullable
    public static Folder fromFile(final File file) {
        return fromFile(file, null);
    }

    @Nullable
    public static Folder fromFile(final File file, final String subfolders) {

        if (file == null) {
            return null;
        }
        return new Folder(FolderType.FILE, Uri.fromFile(file), null, toFolderNames(subfolders));
    }

    @Nullable
    public static Folder fromDocumentUri(final Uri uri) {
        return fromDocumentUri(uri, null);
    }

    @Nullable
    public static Folder fromDocumentUri(final Uri uri, final String subfolders) {

        if (uri == null) {
            return null;
        }
        return new Folder(FolderType.DOCUMENT, uri, null, toFolderNames(subfolders));
    }

    @Nullable
    public static Folder fromFolder(final Folder folder, final String subfolder) {
        if (folder == null) {
            return null;
        }
        final List<String> newSubfolders = new ArrayList<>(folder.subfolders);
        newSubfolders.addAll(toFolderNames(subfolder));
        return new Folder(folder.type, folder.uri, folder.publicFolder, newSubfolders);
    }

    @Nullable
    public static Folder fromPublicFolder(final PublicLocalFolder publicLocalFolder) {
        return fromPublicFolder(publicLocalFolder, null);
    }

    @Nullable
    public static Folder fromPublicFolder(final PublicLocalFolder publicLocalFolder, final String subfolder) {
        if (publicLocalFolder == null) {
            return null;
        }
        return new Folder(FolderType.PUBLIC_FOLDER, null, publicLocalFolder, toFolderNames(subfolder));
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Folder)) {
            return false;
        }

        return this.toConfig(true).equals(((Folder) other).toConfig(true));
    }

    @Override
    public int hashCode() {
        return this.toConfig(true).hashCode();
    }

    private String toConfig(final boolean forEquals) {

        final StringBuilder configString = new StringBuilder(type.toString()).append(CONFIG_SEP);
        switch (type) {
            case PUBLIC_FOLDER:
                configString.append(this.publicFolder.name());
                break;
            case FILE:
            case DOCUMENT:
            default:
                if (forEquals) {
                    configString.append(UriUtils.toCompareString(uri));
                } else {
                    configString.append(uri);
                }
                break;
        }
        configString.append(CONFIG_SEP).append(this.subfolderString);
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
    private static List<String> toFolderNames(final String names) {
        if (names == null) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>();
        for (String token : names.split("/")) {
            if (!StringUtils.isBlank(token)) {
                result.add(token.replaceAll("[^a-zA-Z0-9-_.]", "-").trim());
            }
        }
        return result;
    }
}
