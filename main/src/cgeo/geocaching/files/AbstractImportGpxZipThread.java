package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;

import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

abstract class AbstractImportGpxZipThread extends AbstractImportGpxThread {

    protected AbstractImportGpxZipThread(final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
    }

    @Override
    protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        Collection<Geocache> caches = Collections.emptySet();
        // can't assume that GPX file comes before waypoint file in zip -> so we need two passes
        // 1. parse GPX files
        final ZipInputStream zisPass1 = new ZipInputStream(new BufferedInputStream(getInputStream()));
        try {
            for (ZipEntry zipEntry = zisPass1.getNextEntry(); zipEntry != null; zipEntry = zisPass1.getNextEntry()) {
                if (StringUtils.endsWithIgnoreCase(zipEntry.getName(), GPXImporter.GPX_FILE_EXTENSION)) {
                    if (!StringUtils.endsWithIgnoreCase(zipEntry.getName(), GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, (int) zipEntry.getSize()));
                        caches = parser.parse(new NoCloseInputStream(zisPass1), progressHandler);
                    }
                } else {
                    throw new ParserException("Imported zip is not a GPX zip file.");
                }
                zisPass1.closeEntry();
            }
        } finally {
            zisPass1.close();
        }

        // 2. parse waypoint files
        final ZipInputStream zisPass2 = new ZipInputStream(new BufferedInputStream(getInputStream()));
        try {
            for (ZipEntry zipEntry = zisPass2.getNextEntry(); zipEntry != null; zipEntry = zisPass2.getNextEntry()) {
                if (StringUtils.endsWithIgnoreCase(zipEntry.getName(), GPXImporter.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                    importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints, (int) zipEntry.getSize()));
                    caches = parser.parse(new NoCloseInputStream(zisPass2), progressHandler);
                }
                zisPass2.closeEntry();
            }
        } finally {
            zisPass2.close();
        }

        return caches;
    }

    protected abstract InputStream getInputStream() throws IOException;
}