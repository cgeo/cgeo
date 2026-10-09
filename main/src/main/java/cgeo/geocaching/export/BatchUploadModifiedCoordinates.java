package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.UploadCoordinatesBatchService;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Batch upload modified coords
 */
public class BatchUploadModifiedCoordinates extends AbstractExport {
    private final boolean modifiedOnly;

    public BatchUploadModifiedCoordinates(final boolean modifiedOnly) {
        super(R.string.export_modifiedcoords);
        this.modifiedOnly = modifiedOnly;
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        if (activity != null) {
            UploadCoordinatesBatchService.start(activity, cachesList, modifiedOnly);
        }
    }

}
