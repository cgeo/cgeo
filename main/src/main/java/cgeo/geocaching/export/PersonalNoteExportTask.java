package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class PersonalNoteExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
    /**
     * Instantiates and configures the task for exporting personal notes.
     */

    private int persNotesCount = 0;

    PersonalNoteExportTask(@Nullable final Activity activity, final String title) {
        super(activity, title, LocalizationUtils.getString(R.string.export_persnotes));
    }

    @Override
    protected Boolean doInBackgroundInternal(final Geocache[] caches) {
        try {
            int i = 0;
            for (final Geocache cache : caches) {
                final IConnector connector = ConnectorFactory.getConnector(cache);
                publishProgress(++i);
                if (connector instanceof PersonalNoteCapability && StringUtils.isNotBlank(cache.getPersonalNote())) {
                    ((PersonalNoteCapability) connector).uploadPersonalNote(cache);
                    persNotesCount++;
                }
            }
        } catch (final Exception e) {
            Log.e("PersonalNoteExport.ExportTask uploading personal notes", e);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecuteInternal(final Boolean result) {
        if (activity != null) {
            if (result) {
                ActivityMixin.showToast(activity, LocalizationUtils.getPlural(R.plurals.export_persnotes_upload_success, persNotesCount));
            } else {
                ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
            }
        }
    }

    @Override
    protected void onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(LocalizationUtils.getString(R.string.export_persnotes_uploading, persNotesCount));
        }
    }
}
