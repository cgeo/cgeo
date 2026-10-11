package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.PersonalNoteUploadService;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Exports personal notes.
 */
public class PersonalNoteExport extends AbstractExport {

    public PersonalNoteExport() {
        super(R.string.export_persnotes);
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        if (activity != null) {
            PersonalNoteUploadService.start(activity, cachesList);
        }
    }

}
