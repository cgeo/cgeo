package cgeo.geocaching.storage;

import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.UriUtils;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Implementation for File-based content
 */
class FileContentAccessor extends AbstractContentAccessor {

    FileContentAccessor(@NonNull final Context context) {
        super(context);
    }

    public boolean delete(@NonNull final Uri uri) {
        return new File(uri.getPath()).delete();
    }

    @Override
    public Uri rename(@NonNull final Uri uri, @NonNull final String newName) throws IOException {
        final File current = new File(uri.getPath());
        final File newFile = new File(current.getParent(), newName);
        return (current.renameTo(newFile) ? Uri.fromFile(newFile) : null);
    }

    public Uri create(@NonNull final Folder folder, @NonNull final String name) throws IOException {
        final File dir = toFile(folder, true);
        if (dir == null || !dir.isDirectory()) {
            throw new IOException("Dir is null or not a dir for " + folder);
        }
        final String fileName = FileUtils.createUniqueFilename(name, Arrays.asList(Objects.requireNonNull(dir.list())), dir);
        try {
            final File newFile = new File(dir, fileName);
            return newFile.createNewFile() ? Uri.fromFile(newFile) : null;
        } catch (IOException ioe) {
            throw new IOException("Could not create file '" + fileName + "' in dir '" + dir + "'", ioe);
        }
    }

    public ContentStorage.FileInformation getFileInfo(@NonNull final Folder folder, final String name) {
        final File dir = toFile(folder, false);
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        final File f = new File(dir, name);
        return f.exists() ? fileToInformation(folder, f) : null;
    }

    public List<ContentStorage.FileInformation> list(@NonNull final Folder folder) {
        final File dir = toFile(folder, false);
        if (dir == null) {
            return Collections.emptyList();
        }
        return CollectionStream.of(dir.listFiles())
                .map(f -> fileToInformation(folder, f)).toList();
    }

    public boolean ensureFolder(@NonNull final Folder folder, final boolean needsWrite) {
        final File dir = new File(folderToUri(folder).getPath());
        if (dir.isDirectory()) {
            return dir.canRead() && (!needsWrite || dir.canWrite());
        }
        return dir.mkdirs() && dir.canRead() && (!needsWrite || dir.canWrite());
    }

    /**
     * Must return null if folder does not yet exist
     */
    public Uri getUriForFolder(@NonNull final Folder folder) {
        final Uri folderUri = folderToUri(folder);
        if (new File(folderUri.getPath()).isDirectory()) {
            return folderUri;
        }
        return null;
    }

    /**
     * Must return null if file does not yet exist
     */
    public ContentStorage.FileInformation getFileInfo(@NonNull final Uri uri) {
        final File file = new File(uri.getPath());
        if (!file.exists()) {
            return null;
        }
        return fileToInformation(Folder.fromFile(file.getParentFile()), file);
    }

    private Uri folderToUri(final Folder folder) {
        return UriUtils.appendPath(folder.getBaseUri(), CollectionStream.of(folder.getSubdirsToBase()).toJoinedString("/"));
    }

    private File toFile(@NonNull final Folder folder, final boolean needsWrite) {
        if (!ensureFolder(folder, needsWrite)) {
            return null;
        }
        return new File(folderToUri(folder).getPath());
    }

    private ContentStorage.FileInformation fileToInformation(final Folder folder, final File file) {
        return new ContentStorage.FileInformation(
                file.getName(), UriUtils.appendPath(folderToUri(folder), file.getName()),
                file.isDirectory(),
                file.isDirectory() ? Folder.fromFolder(folder, file.getName()) : null, getTypeForName(file.getName()),
                file.length(), file.lastModified());
    }
}
