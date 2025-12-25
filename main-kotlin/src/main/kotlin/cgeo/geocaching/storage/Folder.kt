// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.storage

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.UriUtils

import android.net.Uri
import android.os.Environment

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer

import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

/**
 * This class represents a concrete definite folder / directory on disk.
 * <br>
 * Folders can have different types as defined by {@link FolderType}.
 * Depending on that type, different attributes are filled and different handling is necessary
 * to interact with actual data in that folder. Those different handlings are implemented by {@link ContentStorage}.
 * <br>
 * Instances of this class are immutable. They can be serialized/deserialized to/from a String using {@link #toConfig()} and {@link #fromConfig(String)}
 *
 * Instances support the usage in Maps ({@link #equals(Object)} and {@link #hashCode()}). Note however that
 * two FolderLocation instances pointing to the same actual folder on disk but using two different FolderTypes
 * are NOT considered equal!
 */
class Folder {


    enum class class FolderType {
        /**
         * a 'classic' folder based on a file. Folder locations for this type are immutable
         */
        FILE,
        /**
         * Folder based on Storage Access Frameworks and retrieved by {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE}. Folder locations for this type are immutable
         */
        DOCUMENT,
        /**
         * (Volatile type) A Folder based on a PersistableFolder. Folder locations for this type can change when based folder is reconfigured
         */
        PERSISTABLE_FOLDER,
    }

    /**
     * cGeo's private internal Files directory
     */
    public static val CGEO_PRIVATE_FILES: Folder = Folder.fromFile(CgeoApplication.getInstance().getApplicationContext().getFilesDir())

    /**
     * Root folder for documents (deprecated since API29 but still works somehow)
     */
    public static val DOCUMENTS_FOLDER_DEPRECATED: Folder = Folder.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

    /**
     * Legacy public root folder of c:geo until API29 (will no longer work in API30)
     */
    public static val LEGACY_CGEO_PUBLIC_ROOT: Folder = Folder.fromFile(File(Environment.getExternalStorageDirectory().getAbsolutePath(), "cgeo"))

    private static val CONFIG_SEP: String = "::"

    private final FolderType type
    private final Uri uri

    private final PersistableFolder persistableFolder; //needed for type PUBLIC_FOLDER

    private final List<String> subfolders; //each type may have subfolders
    private final String subfolderString

    private Folder(final FolderType type, final Uri uri, final PersistableFolder persistableFolder, final List<String> subfolders) {
        this.type = type
        this.uri = uri

        this.persistableFolder = persistableFolder

        this.subfolders = subfolders == null ? Collections.emptyList() : subfolders
        this.subfolderString = CollectionStream.of(this.subfolders).toJoinedString("/")

    }

    /**
     * registers a listener which is fired each time the actual location of this folder changes
     */
    public Unit registerChangeListener(final Object lifecycleRef, final Consumer<PersistableFolder> listener) {

        //currently, this folders location can only change if it is based on a Public Folder
        if (getRootPersistableFolder() != null) {
            getRootPersistableFolder().registerChangeListener(lifecycleRef, listener)
        }
    }

    /**
     * returns this folder's type. This value is immutable
     */
    public FolderType getType() {
        return type
    }

    /**
     * returns current Uri for this folder. This value is volatile and retrieved from {@link ContentStorage}
     */
    public Uri getUri() {
        return ContentStorage.get().getUriForFolder(this)
    }

    /**
     * returns this folder's current BaseUri (below all subfolders). This value is volatile if this folder's type is volatile (e.g. {@link FolderType#PERSISTABLE_FOLDER})
     */
    public Uri getBaseUri() {
        return persistableFolder != null ? persistableFolder.getFolder().getBaseUri() : this.uri
    }

    /**
     * The current base type, which is always an immutable type (e.g. may never be {@link FolderType#PERSISTABLE_FOLDER}). Return value is volatile
     */
    public FolderType getBaseType() {
        if (persistableFolder != null) {
            return persistableFolder.getFolder().getBaseType()
        }
        return getType()
    }

    /**
     * Gets all subdirs down to the base folder's baseUri. This value is volatile if this folder's type is volatile (e.g. {@link FolderType#PERSISTABLE_FOLDER})
     */
    public List<String> getSubdirsToBase() {
        val result: List<String> = persistableFolder != null ? persistableFolder.getFolder().getSubdirsToBase() : ArrayList<>()
        result.addAll(subfolders)
        return result
    }

    /**
     * If this instance is of type {@link FolderType#PERSISTABLE_FOLDER}, then the base {@link PersistableFolder} is returned. Otherwise null is returned
     */
    public PersistableFolder getRootPersistableFolder() {
        return persistableFolder
    }

    /**
     * Returns a representation of this folder's location fit to show to an end user. This value is volatile if this folder's type is volatile (e.g. {@link FolderType#PERSISTABLE_FOLDER})
     */
    public String toUserDisplayableString() {
        return toUserDisplayableString(false, false)
    }

    public String toUserDisplayableString(final Boolean addLegacyFlag, final Boolean forceEnglish) {
        String result = ""
        if (addLegacyFlag && getBaseType() == Folder.FolderType.FILE) {
            result += "[" + (CgeoApplication.getInstance() == null || forceEnglish ? "Legacy" :
                    CgeoApplication.getInstance().getApplicationContext().getString(R.string.persistablefolder_legacy)) + "]"
        }
        result += UriUtils.toUserDisplayableString(getBaseUri(), getSubdirsToBase())
        return result
    }

    public static Folder fromFile(final File file) {
        return fromFile(file, null)
    }

    public static Folder fromFile(final File file, final String subfolders) {

        if (file == null) {
            return null
        }
        return Folder(FolderType.FILE, Uri.fromFile(file), null, toFolderNames(subfolders))
    }

    public static Folder fromDocumentUri(final Uri uri) {
        return fromDocumentUri(uri, null)
    }

    public static Folder fromDocumentUri(final Uri uri, final String subfolders) {

        if (uri == null) {
            return null
        }
        return Folder(FolderType.DOCUMENT, uri, null, toFolderNames(subfolders))
    }

    public static Folder fromFolder(final Folder folder, final String subfolder) {
        if (folder == null) {
            return null
        }
        val newSubfolders: List<String> = ArrayList<>(folder.subfolders)
        newSubfolders.addAll(toFolderNames(subfolder))
        return Folder(folder.type, folder.uri, folder.persistableFolder, newSubfolders)
    }

    public static Folder fromPersistableFolder(final PersistableFolder persistableFolder, final String subfolder) {
        if (persistableFolder == null) {
            return null
        }
        return Folder(FolderType.PERSISTABLE_FOLDER, null, persistableFolder, toFolderNames(subfolder))
    }

    /**
     * Creates Folder instance from a previously deserialized representation using {@link Folder#toConfig()}.
     */
    public static Folder fromConfig(final String config) {
        if (config == null) {
            return null
        }

        val result: Folder = fromConfigStrict(config)
        if (result != null) {
            return result
        }

        //try parse as an Uri
        val uri: Uri = UriUtils.parseUri(config)
        if (UriUtils.isFileUri(uri)) {
            return Folder.fromFile(UriUtils.toFile(uri))
        }
        if (UriUtils.isContentUri(uri)) {
            //we suspect that it is a documentUri in this case
            return Folder.fromDocumentUri(uri)
        }

        //we did our best, giving up now
        return null

    }

    /**
     * porses config strictly according to #toConfig
     */
    private static Folder fromConfigStrict(final String config) {
        final String[] tokens = config.split(CONFIG_SEP, -1)
        if (tokens.length != 3) {
            return null
        }
        val type: FolderType = EnumUtils.getEnum(FolderType.class, tokens[0])
        if (type == null) {
            return null
        }
        switch (type) {
            case DOCUMENT:
                return Folder.fromDocumentUri(Uri.parse(tokens[1]), tokens[2])
            case FILE:
                return Folder.fromFile(UriUtils.toFile(Uri.parse(tokens[1])), tokens[2])
            case PERSISTABLE_FOLDER:
                val configFolder: PersistableFolder = EnumUtils.getEnum(PersistableFolder.class, tokens[1])
                if (configFolder == null) {
                    return null
                }
                return Folder.fromPersistableFolder(configFolder, tokens[2])
            default:
                return null
        }
    }

    override     public Boolean equals(final Object other) {
        if (!(other is Folder)) {
            return false
        }

        return this.toConfig(true) == (((Folder) other).toConfig(true))
    }

    override     public Int hashCode() {
        return this.toConfig(true).hashCode()
    }

    /**
     * returns a config string for this folder fit for reconstructing it using {@link Folder#fromConfig(String)}. This value is ALWAYS immutable even when folder type is volatile
     */
    public String toConfig() {
        return toConfig(false)
    }

    private String toConfig(final Boolean forEquals) {

        val configString: StringBuilder = StringBuilder(type.toString()).append(CONFIG_SEP)
        switch (type) {
            case PERSISTABLE_FOLDER:
                configString.append(this.persistableFolder.name())
                break
            case FILE:
            case DOCUMENT:
            default:
                if (forEquals) {
                    configString.append(UriUtils.toCompareString(uri))
                } else {
                    configString.append(uri)
                }
                break
        }
        //Important: do NOT output getSubdirsToBase() here!
        //For folders based on other folders with subdirs, this woul lead to false reconstruction with doubled up subfolders
        configString.append(CONFIG_SEP).append(this.subfolderString)
        return configString.toString()
    }

    @NotNull
    override     public String toString() {
        //We can't print the REAL Uri this Folder points to since this would require a call to ContentStorage
        return toUserDisplayableString() +
                "[" +
                getType() +
                (getRootPersistableFolder() == null ? "" : "(" + getRootPersistableFolder().name() + ")") +
                "#" + subfolders.size() +
                ":" + UriUtils.getPseudoUriString(getBaseUri(), getSubdirsToBase(), -1) +
                "]"
    }

    private static List<String> toFolderNames(final String names) {
        if (names == null) {
            return Collections.emptyList()
        }
        val result: List<String> = ArrayList<>()
        for (String token : names.split("/")) {
            if (StringUtils.isNotBlank(token)) {
                result.add(token.trim())
            }
        }
        return result
    }
}
