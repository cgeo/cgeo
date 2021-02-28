package cgeo.geocaching.maps.mapsforge.v6;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.XmlUtils;



/**
 * Temporary COPY of ContentResolverResourceProvider from mapsforge to
 * quick-fix a found bug (marked with TODO)
 *
 * As soon as this bug is fixed in mapsforge main line, then mapsforge class
 * shall be used again and this one shall be deleted!
 *
 * see {@link org.mapsforge.map.android.rendertheme,ContentResolverResourceProvider}
 */
public class ContentResolverResourceProvider implements XmlThemeResourceProvider {

    private final ContentResolver contentResolver;
    private final Uri relativeRootUri;

    private final Map<String, Uri> resourceUriCache = new HashMap<>();

    private static class DocumentInfo {
        private final String name;
        private final Uri uri;
        private final boolean isDirectory;

        private DocumentInfo(final String name, final Uri uri, final boolean isDirectory) {
            this.name = name;
            this.uri = uri;
            this.isDirectory = isDirectory;
        }
    }

    public ContentResolverResourceProvider(final ContentResolver contentResolver, final Uri treeUri) {
        this.contentResolver = contentResolver;
        this.relativeRootUri = treeUri;

        refreshCache();
    }

    /**
     * Build uri cache for one dir level (recursive function).
     */
    private void buildCacheLevel(final String prefix, final Uri dirUri) {
        final List<DocumentInfo> docs = queryDir(dirUri);
        for (DocumentInfo doc : docs) {
            if (doc.isDirectory) {
                buildCacheLevel(prefix + doc.name + "/", doc.uri);
            } else {
                resourceUriCache.put(prefix + doc.name, doc.uri);
            }
        }
    }

    @Override
    public InputStream createInputStream(final String relativePath, final String source) throws FileNotFoundException {
        final Uri docUri = resourceUriCache.get(source);
        if (docUri != null) {
            return contentResolver.openInputStream(docUri);
        }
        return null;
    }

    /**
     * Query the content of a directory using scoped storage.
     *
     * @return a list of arrays with info [0: name (String), 1: uri (Uri), 2: isDir (boolean)]
     */
    private List<DocumentInfo> queryDir(final Uri dirUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Collections.emptyList();
        }
        if (dirUri == null) {
            return Collections.emptyList();
        }

        final List<DocumentInfo> result = new ArrayList<>();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, DocumentsContract.getDocumentId(dirUri));

        final  String[] columns = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        Cursor c = null;
        try {
            c = contentResolver.query(childrenUri, columns, null, null, null);

            while (c.moveToNext()) {
                final  String documentId = c.getString(0);
                final String name = c.getString(1);
                final String mimeType = c.getString(2);

                final Uri uri = DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId);
                final boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                result.add(new DocumentInfo(name, uri, isDir));
            }

            return result;
        } finally {
            IOUtils.closeQuietly(c);
        }
    }

    /**
     * Refresh the uri cache by recreating it.
     */
    private void refreshCache() {
        resourceUriCache.clear();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (relativeRootUri == null) {
            return;
        }

        //TODO: following line in mapsforge is wrong, right below is the corrected version
        //Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(relativeRootUri, DocumentsContract.getTreeDocumentId(relativeRootUri));
        final Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(relativeRootUri, DocumentsContract.getDocumentId(relativeRootUri));
        buildCacheLevel(XmlUtils.PREFIX_FILE, dirUri);
    }
}
