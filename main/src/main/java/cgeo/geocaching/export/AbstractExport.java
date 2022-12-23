package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.content.res.Resources;

import androidx.annotation.NonNull;

abstract class AbstractExport implements Export {
    private final String name;
    private final String progressTitle;

    protected AbstractExport(final int name) {
        final Resources resources = CgeoApplication.getInstance().getResources();
        this.name = resources.getString(name);
        progressTitle = resources.getString(R.string.export_progress, this.name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String toString() {
        // used in the array adapter of the dialog showing the exports
        return getName();
    }

    protected String getProgressTitle() {
        return progressTitle;
    }
}
