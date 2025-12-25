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
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.PersonalNoteCapability
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.AsyncTaskWithProgress
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.content.Context

import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class PersonalNoteExportTask : AsyncTaskWithProgress()<Geocache, Boolean> {
    /**
     * Instantiates and configures the task for exporting personal notes.
     */

    private var persNotesCount: Int = 0

    PersonalNoteExportTask(final Activity activity, final String title) {
        super(activity, title, CgeoApplication.getInstance().getString(R.string.export_persnotes))
    }

    override     protected Boolean doInBackgroundInternal(final Geocache[] caches) {
        try {
            Int i = 0
            for (final Geocache cache : caches) {
                val connector: IConnector = ConnectorFactory.getConnector(cache)
                publishProgress(++i)
                if (connector is PersonalNoteCapability && StringUtils.isNotBlank(cache.getPersonalNote())) {
                    ((PersonalNoteCapability) connector).uploadPersonalNote(cache)
                    persNotesCount++
                }
            }
        } catch (final Exception e) {
            Log.e("PersonalNoteExport.ExportTask uploading personal notes", e)
            return false
        }
        return true
    }

    override     protected Unit onPostExecuteInternal(final Boolean result) {
        if (activity != null) {
            val nonNullActivity: Context = activity
            if (result) {
                ActivityMixin.showToast(activity, nonNullActivity.getResources().getQuantityString(R.plurals.export_persnotes_upload_success, persNotesCount, persNotesCount))
            } else {
                ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_failed))
            }
        }
    }

    override     protected Unit onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(activity.getString(R.string.export_persnotes_uploading, persNotesCount))
        }
    }
}
