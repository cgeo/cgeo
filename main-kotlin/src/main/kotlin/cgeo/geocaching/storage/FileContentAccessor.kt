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

import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.UriUtils

import android.content.Context
import android.net.Uri

import androidx.annotation.NonNull

import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.List
import java.util.Objects


/**
 * Implementation for File-based content
 */
class FileContentAccessor : AbstractContentAccessor() {

    FileContentAccessor(final Context context) {
        super(context)
    }

    public Boolean delete(final Uri uri) {
        return File(uri.getPath()).delete()
    }

    override     public Uri rename(final Uri uri, final String newName) {
        val current: File = File(uri.getPath())
        val newFile: File = File(current.getParent(), newName)
        return (current.renameTo(newFile) ? Uri.fromFile(newFile) : null)
    }

    public Uri create(final Folder folder, final String name) throws IOException {
        val dir: File = toFile(folder, true)
        if (dir == null || !dir.isDirectory()) {
            throw IOException("Dir is null or not a dir for " + folder)
        }
        val fileName: String = FileUtils.createUniqueFilename(name, Arrays.asList(Objects.requireNonNull(dir.list())), dir)
        try {
            val newFile: File = File(dir, fileName)
            return newFile.createNewFile() ? Uri.fromFile(newFile) : null
        } catch (IOException ioe) {
            throw IOException("Could not create file '" + fileName + "' in dir '" + dir + "'", ioe)
        }
    }

    public ContentStorage.FileInformation getFileInfo(final Folder folder, final String name) {
        val dir: File = toFile(folder, false)
        if (dir == null || !dir.isDirectory()) {
            return null
        }
        val f: File = File(dir, name)
        return f.exists() ? fileToInformation(folder, f) : null
    }

    public List<ContentStorage.FileInformation> list(final Folder folder) {
        val dir: File = toFile(folder, false)
        if (dir == null) {
            return Collections.emptyList()
        }
        return CollectionStream.of(dir.listFiles())
                .map(f -> fileToInformation(folder, f)).toList()
    }

    public Boolean ensureFolder(final Folder folder, final Boolean needsWrite) {
        val dir: File = File(folderToUri(folder).getPath())
        if (dir.isDirectory()) {
            return dir.canRead() && (!needsWrite || dir.canWrite())
        }
        return dir.mkdirs() && dir.canRead() && (!needsWrite || dir.canWrite())
    }

    /**
     * Must return null if folder does not yet exist
     */
    public Uri getUriForFolder(final Folder folder) {
        val folderUri: Uri = folderToUri(folder)
        if (File(folderUri.getPath()).isDirectory()) {
            return folderUri
        }
        return null
    }

    /**
     * Must return null if file does not yet exist
     */
    public ContentStorage.FileInformation getFileInfo(final Uri uri) {
        val file: File = File(uri.getPath())
        if (!file.exists()) {
            return null
        }
        return fileToInformation(Folder.fromFile(file.getParentFile()), file)
    }

    private Uri folderToUri(final Folder folder) {
        return UriUtils.appendPath(folder.getBaseUri(), CollectionStream.of(folder.getSubdirsToBase()).toJoinedString("/"))
    }

    private File toFile(final Folder folder, final Boolean needsWrite) {
        if (!ensureFolder(folder, needsWrite)) {
            return null
        }
        return File(folderToUri(folder).getPath())
    }

    private ContentStorage.FileInformation fileToInformation(final Folder folder, final File file) {
        return ContentStorage.FileInformation(
                file.getName(), UriUtils.appendPath(folderToUri(folder), file.getName()),
                folder,
                file.isDirectory(),
                file.isDirectory() ? Folder.fromFolder(folder, file.getName()) : null, getTypeForName(file.getName()),
                file.length(), file.lastModified())
    }
}
