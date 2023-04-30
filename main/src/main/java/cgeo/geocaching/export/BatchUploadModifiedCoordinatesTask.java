package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;

public class BatchUploadModifiedCoordinatesTask extends AsyncTaskWithProgress<Geocache, Boolean> {
    /**
     * Instantiates and configures the task for uploading modified cache coords.
     */
    private final boolean modifiedOnly;
    private int uploadProgress = 0;
    private int uploadOk = 0;
    private int uploadFailed = 0;

    BatchUploadModifiedCoordinatesTask(@Nullable final Activity activity, final String title, final boolean modifiedOnly) {
        super(activity, title, CgeoApplication.getInstance().getString(R.string.export_modifiedcoords));
        this.modifiedOnly = modifiedOnly;
    }

    @Override
    protected Boolean doInBackgroundInternal(final Geocache[] caches) {
        try {
            IConnector con;
            for (final Geocache cache : caches) {
                publishProgress(++uploadProgress);
                if (!modifiedOnly || cache.hasUserModifiedCoords()) {
                    con = ConnectorFactory.getConnector(cache);
                    if (con.supportsOwnCoordinates()) {
                        if (!con.uploadModifiedCoordinates(cache, cache.getCoords())) {
                            uploadFailed++;
                        } else {
                            uploadOk++;
                        }
                    }
                }
            }
        } catch (final Exception e) {
            Log.e("BatchUploadModifiedCoordinates.ExportTask exception when uploading coords", e);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecuteInternal(final Boolean result) {
        if (activity != null) {
            final Context nonNullActivity = activity;
            if (result) {
                ActivityMixin.showToast(activity, uploadFailed == 0
                        ? nonNullActivity.getString(R.string.export_modifiedcoords_success)
                        : nonNullActivity.getString(R.string.export_modifiedcoords_result, uploadFailed, uploadOk));
            } else {
                ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_modifiedcoords_error));
            }
            Log.d("upload finished: ok=" + uploadOk + " / failed=" + uploadFailed + " / total=" + uploadProgress);
        }
    }

    @Override
    protected void onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(activity.getString(R.string.export_modifiedcoords_uploading, uploadProgress));
        }
    }
}
