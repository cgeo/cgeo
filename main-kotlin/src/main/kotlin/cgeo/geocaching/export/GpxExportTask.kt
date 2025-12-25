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

package cgeo.geocaching.export

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.AsyncTaskWithProgress
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.UriUtils

import android.app.Activity
import android.net.Uri

import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import java.util.List

import org.apache.commons.io.IOUtils

class GpxExportTask : AsyncTaskWithProgress()<String, Uri> {
    private final String filename
    private final String name

    /**
     * Instantiates and configures the task for exporting gpx files.
     *
     * @param activity optional: Show a progress bar and toasts
     */
    public GpxExportTask(final Activity activity, final String title, final String filename, final String name) {
        super(activity, title)
        this.filename = filename
        this.name = name
    }

    override     protected Uri doInBackgroundInternal(final String[] geocodes) {
        val allGeocodes: List<String> = ArrayList<>(Arrays.asList(geocodes))

        setMessage(CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, allGeocodes.size(), allGeocodes.size()))

        val uri: Uri = ContentStorage.get().create(PersistableFolder.GPX, filename)
        if (uri == null) {
            return null
        }

        BufferedWriter writer = null
        try (OutputStream os = ContentStorage.get().openForWrite(uri)) {
            if (os == null) {
                return null
            }

            writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
            GpxSerializer().writeGPX(allGeocodes, writer, this::publishProgress)
        } catch (final IOException e) {
            Log.e("GpxExport.ExportTask export", e)
            // delete partial GPX file on error
            ContentStorage.get().delete(uri)

            return null
        } finally {
            IOUtils.closeQuietly(writer)
        }

        return uri
    }

    override     protected Unit onPostExecuteInternal(final Uri uri) {
        val activityLocal: Activity = activity
        if (activityLocal != null) {
            if (uri != null) {
                ShareUtils.shareOrDismissDialog(activityLocal, uri, "application/xml", R.string.export, name + ' ' + activityLocal.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(uri))
            } else {
                ActivityMixin.showToast(activityLocal, activityLocal.getString(R.string.export_failed))
            }
        }
    }
}
