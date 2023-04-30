package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Batch upload modified coords
 */
public class BatchUploadModifiedCoordinates extends AbstractExport {
    private boolean modifiedOnly = true;

    public BatchUploadModifiedCoordinates(final boolean modifiedOnly) {
        super(R.string.export_modifiedcoords);
        this.modifiedOnly = modifiedOnly;
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[0]);
        new BatchUploadModifiedCoordinatesTask(activity, getProgressTitle(), modifiedOnly).execute(caches);
    }

}
