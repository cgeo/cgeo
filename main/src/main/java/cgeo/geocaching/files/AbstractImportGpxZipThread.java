package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.TextUtils;

import android.os.Handler;

import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.core.util.Predicate;

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
    private Collection<Geocache> caches = Collections.emptySet();
    private String gpxFileName = null;

    protected AbstractImportGpxZipThread(final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
    }

    @Override
    protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        // can't assume that GPX file comes before waypoint file in zip -> so we need two passes
        // 1. parse GPX files (all except waypoint files)
        if (!parseZipFileGpxContent(parser, filename -> !StringUtils.endsWithIgnoreCase(filename, GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION),
                GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename)) {
            throw new ParserException("Imported ZIP does not contain a GPX file.");
        }
        // 2. parse only waypoint files
        parseZipFileGpxContent(parser, filename -> StringUtils.endsWithIgnoreCase(filename, GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION),
                GPXImporter.IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints_with_filename);
        return caches;
    }

    /**
     * parse the zip archive for GPX files which match the criteria given in the filename matcher
     *
     * @param parser          the GPXParser object
     * @param filenameMatcher pass true if the GPX file should be progressed in this run
     * @param importStep      the GPXImporter step
     * @param stepText        the description @StringRes which should be displayed
     * @return true if at least one file is a GPX
     */
    private boolean parseZipFileGpxContent(final GPXParser parser, final Predicate<String> filenameMatcher, final int importStep, final @StringRes int stepText) throws IOException, ParserException {
        final ZipArchiveInputStream zisPass = new ZipArchiveInputStream(new BufferedInputStream(getInputStream()), ENCODING);
        try {
            int acceptedFiles = 0;
            int ignoredFiles = 0;
            for (ZipEntry zipEntry = zisPass.getNextZipEntry(); zipEntry != null; zipEntry = zisPass.getNextZipEntry()) {
                gpxFileName = zipEntry.getName();
                if (StringUtils.endsWithIgnoreCase(gpxFileName, FileUtils.GPX_FILE_EXTENSION)) {
                    if (filenameMatcher.test(gpxFileName)) {
                        final long size = zipEntry.getSize();
                        importStepHandler.sendMessage(importStepHandler.obtainMessage(importStep, stepText, (int) (size == -1 ? getZipFileSize(gpxFileName) : size), getSourceDisplayName()));
                        caches = parser.parse(new NoCloseInputStream(zisPass), progressHandler);
                        acceptedFiles++;
                    }
                } else {
                    ignoredFiles++;
                }
            }
            if (ignoredFiles == 0 || acceptedFiles > 0) {
                return true;
            }
        } finally {
            IOUtils.closeQuietly(zisPass);
        }
        return false;
    }

    /**
     * Get the file size of a specific file inside the zip archive.
     * It worth it to spend some extra time to gather the size so that we can visualize a progress while parsing afterwards.
     * This method is rather time consuming, so don't call this if the size can also be found otherwise.
     *
     * @param filename the filename of the file inside the zip archive.
     * @return the uncompressed size of the entry data
     */
    private long getZipFileSize(final String filename) throws IOException {
        final ZipArchiveInputStream zisPass = new ZipArchiveInputStream(new BufferedInputStream(getInputStream()), ENCODING);
        try {
            for (ZipEntry zipEntry = zisPass.getNextZipEntry(); zipEntry != null; zipEntry = zisPass.getNextZipEntry()) {
                if (filename.equals(zipEntry.getName())) {
                    zisPass.getNextZipEntry(); // internally this read everything until the next entry. The file size is sometimes included at the end of the file entry, and is therefore only available if everything was read.
                    return zipEntry.getSize();
                }
            }
        } finally {
            IOUtils.closeQuietly(zisPass);
        }
        return -1;
    }

    @Override
    protected String getSourceDisplayName() {
        return gpxFileName == null ? ".gpx" : TextUtils.stripHtml(gpxFileName);
    }

    @WorkerThread
    protected abstract InputStream getInputStream() throws IOException;
}
