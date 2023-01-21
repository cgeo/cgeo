package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Exports personal notes.
 */
public class PersonalNoteExport extends AbstractExport {

    private int persNotesCount = 0;

    public PersonalNoteExport() {
        super(R.string.export_persnotes);
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[cachesList.size()]);
        new ExportTask(activity).execute(caches);
    }

    private class ExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
        /**
         * Instantiates and configures the task for exporting personal notes.
         */
        ExportTask(@Nullable final Activity activity) {
            super(activity, getProgressTitle(), CgeoApplication.getInstance().getString(R.string.export_persnotes));
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
                final Context nonNullActivity = activity;
                if (result) {
                    ActivityMixin.showToast(activity, nonNullActivity.getResources().getQuantityString(R.plurals.export_persnotes_upload_success, persNotesCount, persNotesCount));
                } else {
                    ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdateInternal(final Integer status) {
            if (activity != null) {
                setMessage(activity.getString(R.string.export_persnotes_uploading, persNotesCount));
            }
        }
    }

}
