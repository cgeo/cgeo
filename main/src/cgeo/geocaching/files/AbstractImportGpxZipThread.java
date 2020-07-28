package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.TextUtils;

import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;

abstract class AbstractImportGpxZipThread extends AbstractImportGpxThread {

    public static final String ENCODING = "cp437"; // Geocaching.com used windows cp 437 encoding
    private String gpxFileName = null;

    protected AbstractImportGpxZipThread(final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
    }

    @Override
    protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        Collection<Geocache> caches = Collections.emptySet();
        // can't assume that GPX file comes before waypoint file in zip -> so we need two passes
        // 1. parse GPX files
        final ZipArchiveInputStream zisPass1 = new ZipArchiveInputStream(new BufferedInputStream(getInputStream()), ENCODING);
        try {
            int acceptedFiles = 0;
            int ignoredFiles = 0;
            for (ZipEntry zipEntry = zisPass1.getNextZipEntry(); zipEntry != null; zipEntry = zisPass1.getNextZipEntry()) {
                gpxFileName = zipEntry.getName();
                if (StringUtils.endsWithIgnoreCase(gpxFileName, FileUtils.GPX_FILE_EXTENSION)) {
                    if (!StringUtils.endsWithIgnoreCase(gpxFileName, GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, (int) zipEntry.getSize(), getSourceDisplayName()));
                        caches = parser.parse(new NoCloseInputStream(zisPass1), progressHandler);
                        acceptedFiles++;
                    }
                } else {
                    ignoredFiles++;
                }
            }
            if (ignoredFiles > 0 && acceptedFiles == 0) {
                throw new ParserException("Imported ZIP does not contain a GPX file.");
            }
        } finally {
            IOUtils.closeQuietly(zisPass1);
        }

        // 2. parse waypoint files
        final InputStream inputStream = getInputStream();
        final ZipArchiveInputStream zisPass2 = new ZipArchiveInputStream(new BufferedInputStream(inputStream), ENCODING);
        try {
            for (ZipEntry zipEntry = zisPass2.getNextZipEntry(); zipEntry != null; zipEntry = zisPass2.getNextZipEntry()) {
                gpxFileName = zipEntry.getName();
                if (StringUtils.endsWithIgnoreCase(gpxFileName, GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                    importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints_with_filename, (int) zipEntry.getSize(), gpxFileName));
                    caches = parser.parse(new NoCloseInputStream(zisPass2), progressHandler);
                }
            }
        } finally {
            IOUtils.closeQuietly(zisPass2);
            IOUtils.closeQuietly(inputStream);
        }

        return caches;
    }

    @Override
    protected String getSourceDisplayName() {
        return gpxFileName == null ? ".gpx" : TextUtils.stripHtml(gpxFileName);
    }

    protected abstract InputStream getInputStream() throws IOException;
}
